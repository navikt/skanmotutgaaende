package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.consumers.azure.AzureTokenConsumer;
import no.nav.skanmotutgaaende.exceptions.functional.JournalpostConflictException;
import no.nav.skanmotutgaaende.exceptions.functional.LagreFilDetaljerFinnesIkkeException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static no.nav.skanmotutgaaende.lagrefildetaljer.NavHeaders.NAV_CALL_ID;
import static no.nav.skanmotutgaaende.lagrefildetaljer.NavHeaders.NAV_CONSUMER_ID;
import static no.nav.skanmotutgaaende.lagrefildetaljer.NavHeaders.NAV_CONSUMER_ID_VALUE;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.MDC_CALL_ID;
import static no.nav.skanmotutgaaende.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotutgaaende.metrics.MetricLabels.PROCESS_NAME;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class LagreFildetaljerConsumer {

	private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "mottaDokumentUtgaaendeSkanning";

	private final RestTemplate restTemplate;
	private final SkanmotutgaaendeProperties.AzureEndpoint dokarkiv;
	private final AzureTokenConsumer azureTokenConsumer;

	public LagreFildetaljerConsumer(
			AzureTokenConsumer azureTokenConsumer,
			RestTemplateBuilder restTemplateBuilder,
			SkanmotutgaaendeProperties skanmotutgaaendeProperties
	) {
		this.azureTokenConsumer = azureTokenConsumer;
		this.dokarkiv = skanmotutgaaendeProperties.getEndpoints().getDokarkiv();
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(150))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lagreFilDetaljer"}, percentiles = {0.5, 0.95}, histogram = true)
	public void lagreFilDetaljer(LagreFildetaljerRequest lagreFildetaljerRequest, String journalpostId) {
		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<LagreFildetaljerRequest> requestEntity = new HttpEntity<>(lagreFildetaljerRequest, headers);

			URI uri = UriComponentsBuilder.fromHttpUrl(dokarkiv.getUrl())
					.pathSegment(journalpostId, MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE)
					.build().toUri();
			restTemplate.exchange(uri, PUT, requestEntity, LagreFildetaljerResponse.class);

		} catch (HttpClientErrorException e) {
			if (NOT_FOUND.equals(e.getStatusCode())) {
				throw new LagreFilDetaljerFinnesIkkeException(String.format("lagreFilDetaljer feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			} else if (CONFLICT.equals(e.getStatusCode())) {
				throw new JournalpostConflictException(String.format("lagreFilDetaljer feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			} else {
				throw new SkanmotutgaaendeFunctionalException(String.format("lagreFilDetaljer feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			}
		} catch (HttpServerErrorException e) {
			throw new SkanmotutgaaendeTechnicalException(String.format("lagreFilDetaljer feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(azureTokenConsumer.getClientCredentialToken(dokarkiv.getScope()).access_token());
		headers.setContentType(APPLICATION_JSON);

		if (MDC.get(MDC_CALL_ID) != null) {
			headers.add(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
		}
		headers.add(NAV_CONSUMER_ID, NAV_CONSUMER_ID_VALUE);
		return headers;
	}
}
