package no.nav.skanmotutgaaende.consumers.journalpostapi;

import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.consumers.journalpostapi.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.exceptions.functional.JournalpostConflictException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.skanmotutgaaende.consumers.azure.AzureOAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.skanmotutgaaende.consumers.journalpostapi.NavHeaders.NAV_CALL_ID;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.MDC_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
public class LagreFildetaljerConsumer {

	private final WebClient webClient;

	public LagreFildetaljerConsumer(
			WebClient webClient,
			SkanmotutgaaendeProperties skanmotutgaaendeProperties,
			CodecProperties codecProperties
	) {
		this.webClient = webClient.mutate()
				.baseUrl(skanmotutgaaendeProperties.getEndpoints().getDokarkiv().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
								.maxInMemorySize((int) codecProperties.getMaxInMemorySize().toBytes()))
						.build())
				.build();
	}

	@Retryable(retryFor = SkanmotutgaaendeTechnicalException.class)
	public void lagreFilDetaljer(@Validated LagreFildetaljerRequest lagreFildetaljerRequest, String journalpostId) {
		webClient.put()
				.uri(uriBuilder -> uriBuilder
						.path("/{journalpostId}/mottaDokumentUtgaaendeSkanning")
						.build(journalpostId))
				.header(NAV_CALL_ID, MDC.get(MDC_CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.bodyValue(lagreFildetaljerRequest)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(WebClientResponseException.class, err -> mapError(err, journalpostId))
				.block();
	}

	private Throwable mapError(WebClientResponseException webException, String journalpostId) throws SkanmotutgaaendeFunctionalException {
		if (webException.getStatusCode().is4xxClientError()) {
			if (CONFLICT.equals(webException.getStatusCode())) {
				throw new JournalpostConflictException(format("lagreFilDetaljer feilet funksjonelt med journalpostId=%s, statusKode=%s. Feilmelding=%s", journalpostId,
						webException.getStatusCode(), webException.getResponseBodyAsString()), webException);
			}
			throw new SkanmotutgaaendeFunctionalException(format("lagreFilDetaljer feilet funksjonelt med statusKode=%s. Feilmelding=%s",
					webException.getStatusCode(), webException.getMessage()), webException);
		}
		throw new SkanmotutgaaendeTechnicalException(format("lagreFilDetaljer feilet teknisk med statusKode=%s. Feilmelding=%s", webException
				.getStatusCode(), webException.getResponseBodyAsString()), webException);
	}
}
