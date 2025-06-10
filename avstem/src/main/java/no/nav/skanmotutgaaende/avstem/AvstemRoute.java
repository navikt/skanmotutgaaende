package no.nav.skanmotutgaaende.avstem;

import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.RemoveMdcProcessor;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.stereotype.Component;

import static no.nav.skanmotutgaaende.jira.OpprettJiraService.finnForrigeVirkedag;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 1500;
	private final OpprettJiraService opprettJiraService;
	private final AvstemController avstemController;

	public AvstemRoute(OpprettJiraService opprettJiraService,
					   AvstemController avstemController) {
		this.opprettJiraService = opprettJiraService;
		this.avstemController = avstemController;
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.process(new MdcSetterProcessor())
				.logStackTrace(true)
				.log(ERROR, log, "Skanmotutgaaende feilet teknisk. ${exception}");

		onException(GenericFileOperationFailedException.class)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");

		from("cron:tab?schedule={{skanmotutgaaende.avstem.schedule}}")
				.pollEnrich("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
						"?{{skanmotutgaaende.endpointconfig}}" +
						"&antInclude=*.txt,*.TXT" +
						"&move=processed",
						CONNECTION_TIMEOUT)
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.routeId("avstem_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotutgaaende starter cron jobb for å avstemme referanser...")
				.aggregate(constant(true), new GroupedExchangeAggregationStrategy()).completionTimeout(2000)
				.choice()
					.when(exchange -> exchange.getIn().getBody() == null || exchange.getIn().getBody(String.class).isEmpty())
						.process(exchange -> exchange.setProperty(EXCHANGE_AVSTEMT_DATO, finnForrigeVirkedag()))
						.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og se opprettet Jira-sak.")
						.bean(opprettJiraService)
						.log(INFO, log, "Skanmotutgaaende opprettet jira-sak med key=${body.jiraIssueKey} for manglende avstemmingsfil.")
					.otherwise()
						.bean(avstemController)
				.endChoice()
				.process(new RemoveMdcProcessor())
				.end();
		// @formatter:on
	}
}
