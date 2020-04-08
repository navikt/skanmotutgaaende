package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class LagrefildetaljerIT {

    private final byte[] DUMMY_FILE = "dummyfile".getBytes();
    private final String JOURNALPOST_ID = "001";
    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/intern/journalpostapi/v1/journalpost/001/mottaDokumentUtgaaendeSkanning";

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
    }

    @Test
    public void shouldLagreFildetaljer() throws IOException {
        LagreFildetaljerRequest request = createLagreFildetaljerRequest();
        LagreFildetaljerResponse res = lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID);
        assertEquals(null, res);
    }

    private LagreFildetaljerRequest createLagreFildetaljerRequest() throws IOException {
        return LagreFildetaljerRequest.builder()
                .dato(new Date())
                .endorsernr("3110190003NAV743506")
                .mottattfra("1400")
                .mottatti("1400")
                .batchnavn("__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip")
                .dokumentvarianter(Arrays.asList(
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype("pdf")
                                .variantFormat("ARKIV")
                                .fysiskDokument(DUMMY_FILE)
                                .filnavn("dummy.pdf")
                                .build(),
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype("xml")
                                .variantFormat("ORIGINAL")
                                .fysiskDokument(DUMMY_FILE)
                                .filnavn("dummy.xml")
                                .build()))
                .build();
    }
}
