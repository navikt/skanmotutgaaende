package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = TestConfig.class,
		webEnvironment = RANDOM_PORT,
		properties = "spring.cloud.vault.token=123456"
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class AbstractItest {

	private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/intern/journalpostapi/v1/journalpost/001/mottaDokumentUtgaaendeSkanning";
	private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST = "/rest/intern/journalpostapi/v1/journalpost/002/mottaDokumentUtgaaendeSkanning";

	final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";
	private final String URL_DOKARKIV_JOURNALPOST_BAD_REQUEST = "/rest/intern/journalpostapi/v1/journalpost/4000004/mottaDokumentUtgaaendeSkanning";

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@BeforeEach
	void setUp() {
		setUpStubs();
	}

	private void setUpStubs() {
		stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE))
				.willReturn(aResponse().withStatus(OK.value())));
		stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())));
	}


	void setUpHappyStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{}")));
	}

	void setUpBadStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_BAD_REQUEST))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())));
	}
}
