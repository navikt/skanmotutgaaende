package no.nav.skanmot1408.consumers.lagrefildetaljer;

import no.nav.skanmot1408.config.properties.Skanmot1408Properties;
import no.nav.skanmot1408.exceptions.functional.MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException;
import no.nav.skanmot1408.exceptions.functional.MottaDokumentUtgaaendeSkanningFunctionalException;
import no.nav.skanmot1408.exceptions.functional.MottaDokumentUtgaaendeSkanningTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmot1408.exceptions.technical.MottaDokumentUtgaaendeSkanningTechnicalException;
import no.nav.skanmot1408.metrics.Metrics;
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
import java.time.Duration;

import static no.nav.skanmot1408.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.skanmot1408.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.skanmot1408.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmot1408.metrics.MetricLabels.PROCESS_NAME;

@Component
public class LagreFildetaljerConsumer {

    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/mottaDokumentUtgaaendeSkanning";

    private final RestTemplate restTemplate;
    private final String dokarkivJournalpostUrl;

    public LagreFildetaljerConsumer(RestTemplateBuilder restTemplateBuilder,
                                             Skanmot1408Properties skanmot1408Properties) {
        this.dokarkivJournalpostUrl = skanmot1408Properties.getDokarkivjournalposturl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(150))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(skanmot1408Properties.getServiceuser().getUsername(),
                        skanmot1408Properties.getServiceuser().getPassword())
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lagreFilDetaljer"}, percentiles = {0.5, 0.95}, histogram = true)
    public LagreFildetaljerResponse lagreFilDetaljer(LagreFildetaljerRequest lagreFildetaljerRequest, String journalpostId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<LagreFildetaljerRequest> requestEntity = new HttpEntity<>(lagreFildetaljerRequest, headers);

            String putLagreFildetaljerUrl = dokarkivJournalpostUrl + "/" + journalpostId + MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE;

            return restTemplate.exchange(putLagreFildetaljerUrl, HttpMethod.PUT, requestEntity, LagreFildetaljerResponse.class).getBody();
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (MDC.get(MDC_NAV_CALL_ID) != null) {
            headers.add(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
        }
        if (MDC.get(MDC_NAV_CONSUMER_ID) != null) {
            headers.add(MDC_NAV_CONSUMER_ID, MDC.get(MDC_NAV_CONSUMER_ID));
        }
        return headers;
    }
}
