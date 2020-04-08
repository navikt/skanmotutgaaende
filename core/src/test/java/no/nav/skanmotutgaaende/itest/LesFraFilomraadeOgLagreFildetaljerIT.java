package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningFunctionalException;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lesoglagre.LesFraFilomraadeOgLagreFildetaljer;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilConsumer;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilService;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgLagreFildetaljerIT {

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/.+/mottaDokumentUtgaaendeSkanning";
    private final String URL_DOKARKIV_JOURNALPOST_003 = "/rest/intern/journalpostapi/v1/journalpost/003/mottaDokumentUtgaaendeSkanning";

    LesFraFilomraadeOgLagreFildetaljer lesFraFilomraadeOgLagreFildetaljer;
    LesZipfilService lesZipfilService;
    LagreFildetaljerService lagreFildetaljerService;

    @Autowired
    SkanmotutgaaendeProperties skanmotutgaaendeProperties;

    @BeforeEach
    void setUpServices() {
        lesZipfilService = new LesZipfilService(new LesZipfilConsumer());
        lagreFildetaljerService = new LagreFildetaljerService(new LagreFildetaljerConsumer(new RestTemplateBuilder(), skanmotutgaaendeProperties));
        lesFraFilomraadeOgLagreFildetaljer = new LesFraFilomraadeOgLagreFildetaljer(lesZipfilService, lagreFildetaljerService);
        setUpStubs();
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));
    }

    @Test
    public void shouldLesOgLagreHappy() {
        assertDoesNotThrow(() -> lesFraFilomraadeOgLagreFildetaljer.lesOgLagre());
        verify(exactly(10), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldThrowExceptionIfFailingToLagreFildetaljer() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_003))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
        assertThrows(MottaDokumentUtgaaendeSkanningFunctionalException.class, () -> lesFraFilomraadeOgLagreFildetaljer.lesOgLagre());
    }
}
