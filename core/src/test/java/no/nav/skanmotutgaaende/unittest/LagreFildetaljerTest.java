package no.nav.skanmotutgaaende.unittest;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LagreFildetaljerTest {

    private final String JOURNALPOSTID = "11111111";
    private final String MOTTAKSKANAL = "SKAN_IM";
    private final String BATCHNAVN = "navnPaaBatch.zip";
    private final String FILNAVN = "filnavn.pdf";
    private final String ENDORSERNR = "222111NAV456";
    private final String FYSISK_POSTBOKS = "1002";
    private final String STREKKODE_POSTBOKS = "1004";
    private final byte[] DUMMY_FILE = "dummyfile".getBytes();

    @Test
    public void shouldExtractLagreFildetaljerRequestFromSkanningmetadata() {
        LagreFildetaljerRequest lagreFildetaljerRequest = LagreFildetaljerService.extractLagreFildetaljerRequestFromSkanningmetadata(
                FilepairWithMetadata.builder()
                        .skanningmetadata(Skanningmetadata.builder()
                                .journalpost(Journalpost.builder()
                                        .journalpostId(JOURNALPOSTID)
                                        .mottakskanal(MOTTAKSKANAL)
                                        .datoMottatt(new Date())
                                        .batchNavn(BATCHNAVN)
                                        .filNavn(FILNAVN)
                                        .endorsernr(ENDORSERNR)
                                        .build())
                                .skanningInfo(SkanningInfo.builder()
                                        .fysiskPostboks(FYSISK_POSTBOKS)
                                        .strekkodePostboks(STREKKODE_POSTBOKS)
                                        .build())
                                .build())
                        .xml(DUMMY_FILE)
                        .pdf(DUMMY_FILE)
                .build());

        assertEquals(BATCHNAVN, lagreFildetaljerRequest.getBatchnavn());
        assertEquals(MOTTAKSKANAL, lagreFildetaljerRequest.getMottakskanal());
        assertEquals(FYSISK_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequest.FYSISK_POSTBOKS));
        assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequest.STREKKODE_POSTBOKS));
        assertEquals(ENDORSERNR, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequest.ENDORSER_NR));
        assertEquals(2, lagreFildetaljerRequest.getDokumentvarianter().size());
    }

    private String getTillegsopplysningerVerdiFromNokkel(List<LagreFildetaljerRequest.Tilleggsopplysninger> tilleggsopplysninger, String nokkel) {
        return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
    }

}
