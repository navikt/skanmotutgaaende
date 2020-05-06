package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.MottaDokumentUtgaaendeSkanningTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.MottaDokumentUtgaaendeSkanningTechnicalException;
import no.nav.skanmotutgaaende.jaxws.MDCGenerate;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.metrics.Metrics;
import no.nav.skanmotutgaaende.constants.MDCConstants;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static no.nav.skanmotutgaaende.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotutgaaende.metrics.MetricLabels.PROCESS_NAME;

@Component
public class LagreFildetaljerConsumer {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String CONSUMER_ID = "skanmotutgaaende";
    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "mottaDokumentUtgaaendeSkanning";

    private final RestTemplate restTemplate;
    private final String dokarkivJournalpostUrl;

    public LagreFildetaljerConsumer(RestTemplateBuilder restTemplateBuilder,
                                             SkanmotutgaaendeProperties skanmotutgaaendeProperties) {
        this.dokarkivJournalpostUrl = skanmotutgaaendeProperties.getDokarkivjournalposturl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(150))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(skanmotutgaaendeProperties.getServiceuser().getUsername(),
                        skanmotutgaaendeProperties.getServiceuser().getPassword())
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lagreFilDetaljer"}, percentiles = {0.5, 0.95}, histogram = true)
    public LagreFildetaljerResponse lagreFilDetaljer(LagreFildetaljerRequest lagreFildetaljerRequest, String journalpostId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<LagreFildetaljerRequest> requestEntity = new HttpEntity<>(lagreFildetaljerRequest, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(dokarkivJournalpostUrl)
                    .pathSegment(journalpostId, MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE)
                    .build().toUri();
            return restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, LagreFildetaljerResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException(String.format("mottaDokumentUtgaaendeSkanning feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            } else if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                throw new MottaDokumentUtgaaendeSkanningTillaterIkkeTilknyttingFunctionalException(String.format("mottaDokumentUtgaaendeSkanning feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            } else {
                throw new MottaDokumentUtgaaendeSkanningFunctionalException(String.format("mottaDokumentUtgaaendeSkanning feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            }
        } catch (HttpServerErrorException e) {
            throw new MottaDokumentUtgaaendeSkanningTechnicalException(String.format("mottaDokumentUtgaaendeSkanning feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }

    private HttpHeaders createHeaders() {
        MDCGenerate.generateNewCallIdIfThereAreNone();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add(CORRELATION_HEADER, MDC.get(MDCConstants.MDC_CALL_ID));
        headers.add(MDCConstants.MDC_NAV_CONSUMER_ID, CONSUMER_ID);
        return headers;
    }
}
