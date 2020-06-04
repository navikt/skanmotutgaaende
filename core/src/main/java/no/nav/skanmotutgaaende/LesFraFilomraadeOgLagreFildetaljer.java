package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeUnzipperTechnicalException;
import no.nav.skanmotutgaaende.filomraade.FilomraadeService;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.mdc.MDCGenerate;
import no.nav.skanmotutgaaende.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class LesFraFilomraadeOgLagreFildetaljer {

    private final FilomraadeService filomraadeService;
    private final LagreFildetaljerService lagreFildetaljerService;

    private boolean isFeilomraadeDirty = false;

    public LesFraFilomraadeOgLagreFildetaljer(FilomraadeService filomraadeService,
                                              LagreFildetaljerService lagreFildetaljerService) {
        this.filomraadeService = filomraadeService;
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    //@Scheduled(cron = "0 0 6,7,16,17,21 * * ?")
    @Scheduled(initialDelay = 10_000, fixedDelay = 1_800_000) //Kjører hvert 30 min. For tidlig testing
    public void scheduledJob() {
        lesOgLagreZipfiler();
    }

    public void lesOgLagreZipfiler() {
        List<String> processedZipFiles = new ArrayList<>();
        try {
            List<String> zipFileNames = filomraadeService.getFileNames();
            log.info("Skanmotutgaaende fant {} zipfiler på sftp server: {}", processedZipFiles.size(), processedZipFiles);

            for (String zipName : zipFileNames) {
                setUpMDCforZip(zipName);
                AtomicBoolean safeToDeleteZipFile = new AtomicBoolean(true);
                isFeilomraadeDirty = false;

                log.info("Skanmotutgaaende laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList;
                byte[] zipFile = filomraadeService.getZipFile(zipName);
                try {
                    filepairList = Unzipper.unzipXmlPdf(zipFile);
                } catch (Exception e) {
                    log.error("Skanmotutgaaende klarte ikke lese zipfil {}", zipName, e);
                    processedZipFiles.add(zipName);
                    lastOppZipfilTilFeilomrade(zipFile, zipName);
                    continue;
                }
                log.info("Skanmotutgaaende begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    setUpMDCforFile(filepair.getName());

                    Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair, zipName);
                    if (skanningmetadata.isEmpty()) {
                        lastOppFilpar(filepair, zipName);
                        tearDownMDCforFile();
                    } else {
                        boolean lagringOk = lagreFildetaljerService.lagreFildetaljer(skanningmetadata, filepair);
                        try {
                            if (!lagringOk) {
                                lastOppFilpar(filepair, zipName);
                            }
                        } catch (Exception e) {
                            log.error("Skanmotutgaaende feilet ved opplasting til feilområde, fil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
                            safeToDeleteZipFile.set(false);
                        } finally {
                            tearDownMDCforFile();
                        }
                    }
                });

                if (safeToDeleteZipFile.get()) {
                    filomraadeService.moveZipFile(zipName, "processed");
                }
                cleanUpLastOppFilerTilFeilomrade(zipName);
                tearDownMDCforZip();
            }
        } catch (Exception e) {
            log.error("Skanmotutgaaende ukjent feil oppstod i lesOgLagreZipfiler, feilmelding={}", e.getMessage(), e);
        } finally {
            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        log.info("Skanmotutgaaende laster opp filpar til feilområde, fil={} zipfil={}", filepair.getName(), zipName);
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
        isFeilomraadeDirty = true;
    }

    private void lastOppZipfilTilFeilomrade(byte[] zipFile, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(zipFile, zipName, path);
    }

    private void cleanUpLastOppFilerTilFeilomrade(String zipName) {
        if (isFeilomraadeDirty) {
            filomraadeService.cleanDirtyFeilomrade(Utils.removeFileExtensionInFilename(zipName));
        }
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair, String zipName) {
        try {
            return Optional.of(UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml()));
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotutgaaende klarte ikke unmarshalle.", filepair.getName(), e);
            return Optional.empty();
        } catch (SkanmotutgaaendeUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            return Optional.empty();
        } catch (SkanmotutgaaendeUnzipperTechnicalException e) {
            log.error("Teknisk feil oppsto ved deserialisering av {}, feilmelding={}, cause={}", filepair.getName(), e.getMessage(), e.getCause().getMessage(), e);
            return Optional.empty();
        }
    }

    private void setUpMDCforZip(String zipname) {
        MDCGenerate.setZipId(zipname);
    }

    private void tearDownMDCforZip() {
        MDCGenerate.clearZipId();
    }

    private void setUpMDCforFile(String filename) {
        MDCGenerate.setFileName(filename);
        MDCGenerate.generateNewCallIdIfThereAreNone();
    }

    private void tearDownMDCforFile() {
        MDCGenerate.clearFilename();
        MDCGenerate.clearCallId();
    }
}
