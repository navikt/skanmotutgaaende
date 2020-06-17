package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.AbstractSkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeUnzipperTechnicalException;
import no.nav.skanmotutgaaende.filomraade.FilomraadeService;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.mdc.MDCGenerate;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import no.nav.skanmotutgaaende.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Slf4j
@Component
public class LesFraFilomraadeOgLagreFildetaljer {

    private final FilomraadeService filomraadeService;
    private final LagreFildetaljerService lagreFildetaljerService;
    private final DokCounter dokCounter;

    private boolean isFeilomraadeDirty = false;

    public LesFraFilomraadeOgLagreFildetaljer(FilomraadeService filomraadeService,
                                              LagreFildetaljerService lagreFildetaljerService,
                                              DokCounter dokCounter) {
        this.filomraadeService = filomraadeService;
        this.lagreFildetaljerService = lagreFildetaljerService;
        this.dokCounter = dokCounter;
    }

    @Scheduled(cron = "${skanmotutgaaende.schedule}")
    public void scheduledJob() {
        lesOgLagreZipfiler();
    }

    public void lesOgLagreZipfiler() {
        List<String> processedZipFiles = new ArrayList<>();
        try {
            List<String> zipFileNames = filomraadeService.getFileNames();
            log.info("Skanmotutgaaende fant {} zipfiler på sftp server: {}", zipFileNames.size(), zipFileNames);

            for (String zipName : zipFileNames) {
                setUpMDCforZip(zipName);
                AtomicBoolean safeToDeleteZipFile = new AtomicBoolean(true);
                isFeilomraadeDirty = false;

                log.info("Skanmotutgaaende laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList;
                byte[] zipFile = null;
                try {
                    zipFile = filomraadeService.getZipFile(zipName);
                    filepairList = Unzipper.unzipXmlPdf(zipFile);
                } catch (Exception e) {
                    log.error("Skanmotutgaaende klarte ikke lese zipfil {}", zipName, e);
                    dokCounter.incrementError(e);
                    if(zipFile != null){
                        processedZipFiles.add(zipName);
                        lastOppZipfilTilFeilomrade(zipFile, zipName);
                    }
                    continue;
                }
                log.info("Skanmotutgaaende begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    setUpMDCforFile(filepair.getName());

                    Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair, zipName);
                    if (skanningmetadata.isEmpty()) {
                        boolean opplastingOk = lastOppFilpar(filepair, zipName);
                        safeToDeleteZipFile.set(opplastingOk && safeToDeleteZipFile.get());
                    } else {
                        boolean lagringOk = lagreFilDetaljer(skanningmetadata.get(), filepair);
                        if (!lagringOk) {
                            boolean opplastingOk = lastOppFilpar(filepair, zipName);
                            safeToDeleteZipFile.set(opplastingOk && safeToDeleteZipFile.get());
                        }
                    }
                    tearDownMDCforFile();
                });

                if (safeToDeleteZipFile.get()) {
                    try {
                        filomraadeService.moveZipFile(zipName, "processed");
                    } catch(Exception e){
                        dokCounter.incrementError(e);
                    }
                }
                cleanUpLastOppFilerTilFeilomrade(zipName);
                tearDownMDCforZip();
            }
        } catch (Exception e) {
            log.error("Skanmotutgaaende ukjent feil oppstod i lesOgLagreZipfiler, feilmelding={}", e.getMessage(), e);
            dokCounter.incrementError(e);
        } finally {
            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
    }

    private boolean lagreFilDetaljer(Skanningmetadata skanningmetadata, Filepair filepair) {
        try {
            lagreFildetaljerService.lagreFildetaljer(skanningmetadata, filepair);
            return true;
        } catch (AbstractSkanmotutgaaendeFunctionalException e) {
            log.warn("Skanmotutgaaende feilet funksjonelt med lagring av fildetaljer fil={}, batch={}", filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn(), e);
            dokCounter.incrementError(e);
            return false;
        } catch (AbstractSkanmotutgaaendeTechnicalException e) {
            log.warn("Skanmotutgaaende feilet teknisk med lagring av fildetaljer fil={}, batch={}", filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn(), e);
            dokCounter.incrementError(e);
            return false;
        } catch (Exception e) {
            log.warn("Skanmotutgaaende feilet med ukjent feil ved lagring av fildetaljer fil={}, batch={}", filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn(), e);
            dokCounter.incrementError(e);
            return false;
        }
    }

    private boolean lastOppFilpar(Filepair filepair, String zipName) {
        try {
            log.info("Skanmotutgaaende laster opp filpar til feilområde, fil={} zipfil={}", filepair.getName(), zipName);
            String path = Utils.removeFileExtensionInFilename(zipName);
            filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
            filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
            isFeilomraadeDirty = true;
            return true;
        } catch (Exception e) {
            log.error("Skanmotutgaaende feilet ved opplasting til feilområde, fil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
            dokCounter.incrementError(e);
            return false;
        }
    }

    private void lastOppZipfilTilFeilomrade(byte[] zipFile, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(zipFile, zipName, path);
    }

    private void cleanUpLastOppFilerTilFeilomrade(String zipName) {
        if (isFeilomraadeDirty) {
            try{
                filomraadeService.cleanDirtyFeilomrade(Utils.removeFileExtensionInFilename(zipName));
            } catch(Exception e) {
                dokCounter.incrementError(e);
            }
        }
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair, String zipName) {
        try {
            Skanningmetadata skanningmetadata = UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml());

            incrementMetadataMetrics(skanningmetadata);
            skanningmetadata.verifyFields();

            return Optional.of(UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml()));
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotutgaaende klarte ikke unmarshalle.", filepair.getName(), e);
            dokCounter.incrementError(e);
            return Optional.empty();
        } catch (SkanmotutgaaendeUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            dokCounter.incrementError(e);
            return Optional.empty();
        } catch (SkanmotutgaaendeUnzipperTechnicalException e) {
            log.error("Teknisk feil oppsto ved deserialisering av {}, feilmelding={}, cause={}", filepair.getName(), e.getMessage(), e.getCause().getMessage(), e);
            dokCounter.incrementError(e);
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

    private void incrementMetadataMetrics(Skanningmetadata skanningmetadata){
        final String STREKKODEPOSTBOKS = "strekkodePostboks";
        final String FYSISKPOSTBOKS = "fysiskPostboks";
        final String EMPTY = "empty";

        dokCounter.incrementCounter(Map.of(
                STREKKODEPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getStrekkodePostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY),
                FYSISKPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getFysiskPostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));
    }
}
