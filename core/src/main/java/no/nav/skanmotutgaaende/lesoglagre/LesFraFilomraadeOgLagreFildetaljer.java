package no.nav.skanmotutgaaende.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.AbstractSkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilService;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    public List<LagreFildetaljerResponse> lesOgLagre() {
        File zipfil = lesZipfilService.lesZipfil();
        try {
            List<FilepairWithMetadata> filepairWithMetadataList = Unzipper.unzipXmlPdf(zipfil);

            List<LagreFildetaljerResponse> responses = filepairWithMetadataList.stream()
                    .map(filepair -> lagreFil(filepair))
                    .filter(response -> null != response)
                    .collect(Collectors.toList());

            String zipName = filepairWithMetadataList.get(0).getSkanningmetadata().getJournalpost().getBatchNavn();
            log.info("Skanmotutgaaende lagret fildetaljer fra zipfil {} i dokarkiv", zipName);
            return responses;
        } catch (IOException e) {
            log.error("Skanmotutgaaende klarte ikke lese fra fil {}", zipfil.getName(), e);
        } catch (SkanmotutgaaendeUnzipperFunctionalException e) {
            log.error("Skanmotutgaaende feilet i unzipping av fil {}", zipfil.getName(), e);
        } catch (InvalidMetadataException e) {
            log.error("Skanningmetadata hadde ugyldige verdier i zipFil {}", zipfil.getName(), e);
        }
        return new ArrayList<>();
    }

    private LagreFildetaljerResponse lagreFil(FilepairWithMetadata filepairWithMetadata) {
        LagreFildetaljerResponse response = null;
        try {
            response = lagreFildetaljerService.lagreFildetaljer(filepairWithMetadata);
            log.info("Skanmotutgaaende lagret fildetaljer for journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        } catch (AbstractSkanmotutgaaendeFunctionalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet funskjonelt med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        } catch (AbstractSkanmotutgaaendeTechnicalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet teknisk med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        }
        return response;
    }
}
