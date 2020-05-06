package no.nav.skanmotutgaaende.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.AbstractSkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilService;
import no.nav.skanmotutgaaende.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
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

    private final LesZipfilService lesZipfilService;
    private final LagreFildetaljerService lagreFildetaljerService;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public LesFraFilomraadeOgLagreFildetaljer(LesZipfilService lesZipfilService,
                                              LagreFildetaljerService lagreFildetaljerService) {
        this.lesZipfilService = lesZipfilService;
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 72 * HOUR)
    public void scheduledJob() {
        lesOgLagre();
    }

    public List<List<LagreFildetaljerResponse>> lesOgLagre() {
        Map<String, byte[]> zipfiles = lesFil();
        log.info("Skanmotutgaaende leste fra filområde og fant {} zipfiler", zipfiles.size());

        List<List<LagreFildetaljerResponse>> allResponses = new ArrayList<>();
        for (String zipname : zipfiles.keySet()) {
            try {
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(zipfiles.get(zipname));

                List<LagreFildetaljerResponse> responses = filepairList.stream()
                        .map(filepair -> lagreFil(filepair))
                        .filter(response -> null != response)
                        .collect(Collectors.toList());

                log.info("Skanmotutgaaende lagret fildetaljer fra zipfil {} i dokarkiv", zipname);
                allResponses.add(responses);
            } catch (IOException e) {
                // TODO: Håndter denne feilen skikkelig. Løses av MMA-4346
                log.error("Skanmotutgaaende klarte ikke lese fra fil {}", zipname, e);
            } catch (SkanmotutgaaendeUnzipperFunctionalException e) {
                // TODO: Håndter denne feilen skikkelig. Løses av MMA-4346
                log.error("Skanmotutgaaende feilet i unzipping av fil {}", zipname, e);
            }
        }
        return allResponses;
    }

    private Map<String, byte[]> lesFil() {
        try {
            return lesZipfilService.getZipFiles();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            return new HashMap<>();
        }
    }

    private LagreFildetaljerResponse lagreFil(Filepair filepair) {
        log.info("Skanmotutgaaende behandler fil {}", filepair.getName());
        LagreFildetaljerResponse response = null;
        FilepairWithMetadata filepairWithMetadata = extractMetadata(filepair);
        if (filepairWithMetadata == null) {
            return null;
        }
        log.info("Fil med navn {} har jpid {}", filepair.getName(), filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        try {
            response = lagreFildetaljerService.lagreFildetaljer(filepairWithMetadata);
            log.info("Skanmotutgaaende lagret fildetaljer for journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        } catch (AbstractSkanmotutgaaendeFunctionalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet funksjonelt med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        } catch (AbstractSkanmotutgaaendeTechnicalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet teknisk med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        }
        return response;
    }

    private FilepairWithMetadata extractMetadata(Filepair filepair) {
        try {
            return UnzipSkanningmetadataUtils.extractMetadata(filepair);
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotutgaaende klarte ikke unmarshalle.", filepair.getName(), e);
            return null;
        }
    }
}
