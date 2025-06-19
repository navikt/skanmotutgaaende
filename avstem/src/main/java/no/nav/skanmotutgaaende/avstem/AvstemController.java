package no.nav.skanmotutgaaende.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.nav.skanmotutgaaende.avstem.AvstemRoute.FEIL_ROUTE;
import static no.nav.skanmotutgaaende.avstem.AvstemRoute.FERDIG_ROUTE;
import static no.nav.skanmotutgaaende.jira.OpprettJiraService.finnForrigeVirkedag;

@Slf4j
@Component
public class AvstemController {

	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;

	public AvstemController(AvstemService avstemService, OpprettJiraService opprettJiraService) {
		this.avstemService = avstemService;
		this.opprettJiraService = opprettJiraService;
	}

	@Handler
	public void avstemAlleReferanser(Exchange exchange) {
		Set<Exchange> exchanges = exchange.getIn().getBody(Set.class);
		log.info("fant {} avstemmingsreferanser for avstemming", exchanges.size());

		LocalDate avstemtDato = finnForrigeVirkedag();
		for (Exchange e : exchanges) {
			try {
				Set<String> referanser = new HashSet<>(List.of(e.getIn().getBody(String.class).split("\\n+")));
				Set<String> feiledeReferanser = avstemService.avstemReferanser(referanser);
				log.info("Skanmotutgaaende fant {} feilende avstemmingsreferanser", feiledeReferanser.size());

				if (!feiledeReferanser.isEmpty()) {
					JiraResponse jiraResponse = opprettJiraSakForFeiledeReferanser(referanser, feiledeReferanser, avstemtDato, e);
					log.info("Skanmotutgaaende har opprettet Jira-sak={} for feilende skanmotutgaaende avstemmingsreferanser", jiraResponse.jiraIssueKey());
				}
				sendMessageToRoute(FERDIG_ROUTE, e);
			} catch (Exception exception) {
				log.error("Skanmotutgaaende feilet ved avstemming av referanser. Exception: {}", exception.getMessage(), exception);
				sendMessageToRoute(FEIL_ROUTE, e);
			}
		}
	}

	private JiraResponse opprettJiraSakForFeiledeReferanser(Set<String> referanser, Set<String> feiledeReferanser, LocalDate avstemtDato, Exchange exchange) {
		try {
			byte[] csvByteArray = getfeiledeReferanserAsCsvByteArray(feiledeReferanser);
			return opprettJiraService.opprettAvstemJiraOppgave(csvByteArray, referanser.size(), feiledeReferanser.size(), avstemtDato);
		} catch (Exception e) {
			sendMessageToRoute(FEIL_ROUTE, exchange);
			throw e;
		}
	}

	public void sendMessageToRoute(String routeUri, Exchange exchange) {
		ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate();
		producerTemplate.send(routeUri, exchange);
	}

	public byte[] getfeiledeReferanserAsCsvByteArray(Set<String> feiledeReferanser) {
		StringBuilder csvBuilder = new StringBuilder();
		for (String feiletReferanse : feiledeReferanser) {
			csvBuilder.append(feiletReferanse).append("\n");
		}
		return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
	}
}
