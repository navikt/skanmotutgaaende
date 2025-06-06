package no.nav.skanmotutgaaende.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class TestService {

	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;

	public TestService(AvstemService avstemService, OpprettJiraService opprettJiraService) {
		// Default constructor
		this.avstemService = avstemService;
		this.opprettJiraService = opprettJiraService;
	}

	@Handler
	public void avstemmingsReferanser(Exchange exchange) {
		log.info("TestService called with exchange: {}", exchange);
		exchange.getIn().getBody();
		Set<Exchange> exchanges = exchange.getIn().getBody(Set.class);
		for(Exchange e : exchanges){
			exchange.getContext().createProducerTemplate().send("direct:behandle_liste", e);
		}
	}

		/*.autoStartup("{{skanmotutgaaende.avstem.startup}}")
	//.aggregate(constant(true), useLatest())
	//.completionSize(1)
				.process(exchange ->
			exchange.getIn().setBody(exchange.getIn().getExchange().getIn().getBody()))
			.log(INFO, log, "Skanmotutgaaende starter cron jobb for å avstemme referanser...")
				.process(new MdcSetterProcessor())
			.log(INFO, log, "Skanmotutgaaende starter behandling av avstemmingsfil=${file:name}.")
				.process(exchange -> {
		exchange.setProperty(EXCHANGE_AVSTEMMINGSFIL_NAVN, simple("${file:name}"));
		exchange.setProperty(EXCHANGE_AVSTEMT_DATO,  parseDatoFraFilnavn(exchange));
	})
			.split(body().tokenize())
			.streaming()
					.aggregationStrategy(new AvstemAggregationStrategy())
			.convertBodyTo(Set.class)
				.end()
				.process(exchange -> {
		Set<String> avstemmingsReferanser = exchange.getIn().getBody(Set.class);
		exchange.getIn().setBody(avstemmingsReferanser);
	})
			.setProperty(ANTALL_FILER_AVSTEMT, simple("${body.size}"))
			.log(INFO, log, "Skanmotutgaaende hentet ${body.size} avstemmingReferanser fra sftp server")
				.bean(avstemService)
				.choice()
					.when(simple("${body}").isNotNull())
			.setProperty(ANTALL_FILER_FEILET, simple("${body.size}"))
			.log(INFO, log, "Skanmotutgaaende fant ${body.size} feilende avstemmingsreferanser")
						.marshal().csv()
						.bean(opprettJiraService)
						.log(INFO, log, "Skanmotutgaaende har opprettet Jira-sak=${body.jiraIssueKey} for feilende skanmotutgaaende avstemmingsreferanser")
						.process(new RemoveMdcProcessor())
			.end()
				.log(INFO, log, "Skanmotutgaaende behandlet ferdig avstemmingsfil: ${file:name}");*/
}
