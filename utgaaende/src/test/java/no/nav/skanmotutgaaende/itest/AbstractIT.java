package no.nav.skanmotutgaaende.itest;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = TestConfig.class,
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public abstract class AbstractIT {


	public static final String INNGAAENDE = "inngaaende";
	public static final String FEILMAPPE = "feilmappe";
	public static final String FAGPOST_MAPPE = "fagpostmappe";
	final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";
	String URL_DOKARKIV_JOURNALPOST_BAD_REQUEST = "/rest/intern/journalpostapi/v1/journalpost/4000004/mottaDokumentUtgaaendeSkanning";



	void setUpHappyStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)).willReturn(aResponse()
				.withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close")
				.withBody("{}")));
	}

	public void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	void setUpBadStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_BAD_REQUEST)).willReturn(aResponse()
				.withStatus(BAD_REQUEST.value()).withHeader("Connection", "close")));
	}

	void setUpConflictStubs() {
		stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_BAD_REQUEST)).willReturn(aResponse()
				.withStatus(CONFLICT.value()).withHeader("Connection", "close")));
	}
}
