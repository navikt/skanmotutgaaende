package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.AbstractSkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import no.nav.skanmotutgaaende.filomraade.FilomraadeService;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LesFraFilomraadeOgLagreFildetaljer {

    private final FilomraadeService filomraadeService;
    private final LagreFildetaljerService lagreFildetaljerService;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public LesFraFilomraadeOgLagreFildetaljer(FilomraadeService filomraadeService,
                                              LagreFildetaljerService lagreFildetaljerService) {
        this.filomraadeService = filomraadeService;
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 72 * HOUR)
    public void scheduledJob() {
        lesOgLagre();
    }

    public List<List<LagreFildetaljerResponse>> lesOgLagre() {
        List<List<LagreFildetaljerResponse>> allResponses = new ArrayList<>();
        List<String> readZipFiles = new ArrayList<>();

        Map<String, byte[]> zipfiles = lesFil();

        log.info("Skanmotutgaaende leste fra filområde og fant {} zipfiler", zipfiles.size());

        for (String zipName : zipfiles.keySet()) {
            try {
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(zipfiles.get(zipName));

                List<LagreFildetaljerResponse> responses = filepairList.stream()
                        .map(filepair -> lagreFil(filepair, zipName))
                        .filter(response -> null != response)
                        .collect(Collectors.toList());

                log.info("Skanmotutgaaende lagret fildetaljer fra zipfil {} i dokarkiv", zipName);
                readZipFiles.add(zipName);
                allResponses.add(responses);
            } catch (IOException e) {
                log.error("Skanmotutgaaende klarte ikke lese fra fil {}", zipName, e);
            } catch (SkanmotutgaaendeUnzipperFunctionalException e) {
                log.error("Skanmotutgaaende feilet i unzipping av fil {}", zipName, e);
            }
        }

        slettZipfiler(readZipFiles);

        return allResponses;
    }

    private Map<String, byte[]> lesFil() {
        try {
            return filomraadeService.getZipFiles();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            return new HashMap<>();
        }
    }

    private LagreFildetaljerResponse lagreFil(Filepair filepair, String zipName) {
        log.info("Skanmotutgaaende behandler fil {}", filepair.getName());
        LagreFildetaljerResponse response = null;

        FilepairWithMetadata filepairWithMetadata = extractMetadata(filepair, zipName);

        if (filepairWithMetadata == null) {
            return null;
        }
        log.info("Filpar med navn {} er tilknyttet journalpost med id {}", filepair.getName(), filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        try {
            response = lagreFildetaljerService.lagreFildetaljer(filepairWithMetadata);
            log.info("Skanmotutgaaende lagret fildetaljer for journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        } catch (AbstractSkanmotutgaaendeFunctionalException e) {
            log.warn("Skanmotutgaaende feilet funksjonelt med lagring av fildetaljer til journalpost med id {}. Fil: {}. Feilmelding: {}",
                    filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), filepair.getName(), e.getMessage(), e);
            lastOppFilpar(filepair, zipName);
        } catch (AbstractSkanmotutgaaendeTechnicalException e) {
            log.warn("Skanmotutgaaende feilet teknisk med lagring av fildetaljer til journalpost med id {}. Fil: {}. Feilmelding: {}",
                    filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), filepair.getName(), e.getLocalizedMessage(), e);
            lastOppFilpar(filepair, zipName);
        }
        return response;
    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        String pdfName = filepair.getName() + ".pdf";
        String xmlName = filepair.getName() + ".xml";
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), pdfName, path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), xmlName, path);
    }

    private void slettZipfiler(List<String> readZipFiles) {
        filomraadeService.deleteZipFiles(readZipFiles);
    }

    private FilepairWithMetadata extractMetadata(Filepair filepair, String zipName) {
        try {
            return UnzipSkanningmetadataUtils.extractMetadata(filepair);
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotutgaaende klarte ikke unmarshalle.", filepair.getName(), e);
            lastOppFilpar(filepair, zipName);
            return null;
        }
    }
}
