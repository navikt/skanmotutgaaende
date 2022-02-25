package no.nav.skanmotutgaaende.itest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
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

	final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";

	public void setUp() {
		setUpStubs();
	}

	private void setUpStubs() {
		String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/intern/journalpostapi/v1/journalpost/001/mottaDokumentUtgaaendeSkanning";
		stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE))
				.willReturn(aResponse()
						.withStatus(OK.value()).withHeader("Connection", "close")));
		String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST = "/rest/intern/journalpostapi/v1/journalpost/002/mottaDokumentUtgaaendeSkanning";
		stubFor(put(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE_INVALID_JOURNALPOST))
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value()).withHeader("Connection", "close")));
	}


	void setUpHappyStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withHeader("Connection", "close")
						.withBody("{}")));
	}

	void setUpBadStubs() {
		String URL_DOKARKIV_JOURNALPOST_BAD_REQUEST = "/rest/intern/journalpostapi/v1/journalpost/4000004/mottaDokumentUtgaaendeSkanning";
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_BAD_REQUEST))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value()).withHeader("Connection", "close")));
	}
}
