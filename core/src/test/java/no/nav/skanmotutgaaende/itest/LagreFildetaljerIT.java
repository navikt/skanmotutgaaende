package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningFunctionalException;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.ENDORSERNR_NOKKEL;
import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.FYSISK_POSTBOKS_NOKKEL;
import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.STREKKODE_POSTBOKS_NOKKEL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class LagreFildetaljerIT {

    private final byte[] DUMMY_FILE = "dummyfile".getBytes();
    private final String JOURNALPOST_ID = "001";
    private final String JOURNALPOST_ID_INVALID = "002";
    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/intern/journalpostapi/v1/journalpost/001/mottaDokumentUtgaaendeSkanning";
    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST = "/rest/intern/journalpostapi/v1/journalpost/002/mottaDokumentUtgaaendeSkanning";

    private LagreFildetaljerConsumer lagrefildetaljerConsumer;

    @Autowired
    private SkanmotutgaaendeProperties skanmotutgaaendeProperties;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        lagrefildetaljerConsumer = new LagreFildetaljerConsumer(new RestTemplateBuilder(), skanmotutgaaendeProperties);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));
        stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
    }

    @Test
    public void shouldLagreFildetaljer() {
        LagreFildetaljerRequest request = createLagreFildetaljerRequest();
        assertDoesNotThrow(() -> lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID));
    }

    @Test
    public void shoulfFailIfInvalidRequest() {
        LagreFildetaljerRequest request = createLagreFildetaljerRequest();
        assertThrows(MottaDokumentUtgaaendeSkanningFunctionalException.class, () -> lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID_INVALID));
    }

    private LagreFildetaljerRequest createLagreFildetaljerRequest() {
        return LagreFildetaljerRequest.builder()
                .datoMottatt(new Date())
                .batchnavn("xml_pdf_pairs_testdata.zip")
                .tilleggsopplysninger(Arrays.asList(
                        Tilleggsopplysning.builder()
                                .nokkel(ENDORSERNR_NOKKEL)
                                .verdi("3110190003NAV743506")
                                .build(),
                        Tilleggsopplysning.builder()
                                .nokkel(FYSISK_POSTBOKS_NOKKEL)
                                .verdi("1408")
                                .build(),
                        Tilleggsopplysning.builder()
                                .nokkel(STREKKODE_POSTBOKS_NOKKEL)
                                .verdi("1408")
                                .build()
                ))
                .dokumentvarianter(Arrays.asList(
                        DokumentVariant.builder()
                                .filtype("pdf")
                                .variantformat("ARKIV")
                                .fysiskDokument(DUMMY_FILE)
                                .filnavn("data_005.pdf")
                                .build(),
                        DokumentVariant.builder()
                                .filtype("xml")
                                .variantformat("ORIGINAL")
                                .fysiskDokument(DUMMY_FILE)
                                .filnavn("data_005.xml")
                                .build()))
                .build();
    }
}
