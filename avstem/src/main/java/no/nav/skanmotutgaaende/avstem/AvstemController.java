package no.nav.skanmotutgaaende.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.springframework.util.CollectionUtils.isEmpty;

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
		log.info("TestService called with exchange: {}", exchange);
		Set<Exchange> exchanges = exchange.getIn().getBody(Set.class);
		for(Exchange e : exchanges){
			String body = e.getIn().getBody(String.class);
			Set<String> referanser = new HashSet<>(List.of(body.split("\\n+")));
			Set<String> feiledeReferanser = avstemService.avstemReferanser(referanser);

			if(!feiledeReferanser.isEmpty()){
				StringBuilder csvBuilder = new StringBuilder();
				for (String feiletReferanse : feiledeReferanser) {
					csvBuilder.append(feiletReferanse).append("\n");
				}
				byte[] csvByteArray = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
				log.info("Skanmotutgaaende fant {} feilende avstemmingsreferanser", feiledeReferanser.size());
				JiraResponse jiraResponse = opprettJiraService.opprettAvstemJiraOppgave(csvByteArray, referanser.size(), feiledeReferanser.size(),
						e.getProperty(EXCHANGE_AVSTEMT_DATO, LocalDate.class));
				log.info("Skanmotutgaaende har opprettet Jira-sak={} for feilende skanmotutgaaende avstemmingsreferanser", jiraResponse.jiraIssueKey());
			}
		}
	}
}
