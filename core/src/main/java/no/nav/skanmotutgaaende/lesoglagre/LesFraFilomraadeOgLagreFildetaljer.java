package no.nav.skanmotutgaaende.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilService;
import no.nav.skanmotutgaaende.utils.Unzipper;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class LesFraFilomraadeOgLagreFildetaljer {

    private final LesZipfilService lesZipfilService;
    private final LagreFildetaljerService lagreFildetaljerService;

    public LesFraFilomraadeOgLagreFildetaljer(LesZipfilService lesZipfilService,
                                              LagreFildetaljerService lagreFildetaljerService) {
        this.lesZipfilService = lesZipfilService;
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    public void lesOgLagre() throws IOException, AbstractSkanmotutgaaendeFunctionalException {
        File zipfil = lesZipfilService.lesZipfil();
        List<FilepairWithMetadata> filepairWithMetadataList = Unzipper.unzipXmlPdf(zipfil);
        lagreFildetaljerService.lagreFildetaljer(filepairWithMetadataList);
        String zipName = filepairWithMetadataList.get(0).getSkanningmetadata().getJournalpost().getBatchNavn();
        log.info("Skanmotutgaaende lagret fildetaljer fra fil {} i dokarkiv", zipName);
    }
}
