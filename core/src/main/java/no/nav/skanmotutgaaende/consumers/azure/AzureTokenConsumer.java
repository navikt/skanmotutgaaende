package no.nav.skanmotutgaaende.consumers.azure;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static no.nav.skanmotutgaaende.config.cache.LokalCacheConfig.AZURE_CACHE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@Component
@Profile({"nais", "local"})
public class AzureTokenConsumer {
	private static final String AZURE_TOKEN_INSTANCE = "azuretoken";
	private final RestTemplate restTemplate;
	private final AzureProperties azureProperties;

	public AzureTokenConsumer(AzureProperties azureProperties,
							  RestTemplateBuilder restTemplateBuilder,
							  ClientHttpRequestFactory azureTokenHttpRequestFactory) {
		this.restTemplate = restTemplateBuilder
				.requestFactory(() -> azureTokenHttpRequestFactory)
				.build();
		this.azureProperties = azureProperties;
	}

	@Cacheable(AZURE_CACHE)
	@Retry(name = AZURE_TOKEN_INSTANCE)
	@CircuitBreaker(name = AZURE_TOKEN_INSTANCE)
	public TokenResponse getClientCredentialToken(String scope) {
		try {
			HttpHeaders headers = createHeaders();
			String form = "grant_type=client_credentials&scope=" + scope + "&client_id=" +
					azureProperties.getClientId() + "&client_secret=" + azureProperties.getClientSecret();
			HttpEntity<String> requestEntity = new HttpEntity<>(form, headers);

			return restTemplate.exchange(azureProperties.getTokenUrl(), POST, requestEntity, TokenResponse.class)
					.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			throw new AzureTokenException(String.format("Klarte ikke hente token fra Azure. Feilet med httpstatus=%s. Feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		headers.setAccept(Collections.singletonList(APPLICATION_JSON));
		return headers;
	}
}