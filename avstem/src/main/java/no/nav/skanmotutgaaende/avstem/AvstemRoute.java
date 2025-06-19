package no.nav.skanmotutgaaende.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.RemoveMdcProcessor;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotutgaaende.jira.OpprettJiraService.finnForrigeVirkedag;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

@Slf4j
@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 1500;
	private final AvstemController avstemController;

	public static String FEIL_ROUTE = "direct:feil";
	public static String FERDIG_ROUTE = "direct:ferdig";

	@Value("${skanmotutgaaende.avstem.delay}")
	private static int delayMs;

	private final OpprettJiraService opprettJiraService;

	public AvstemRoute(AvstemController avstemController, OpprettJiraService opprettJiraService) {
		this.avstemController = avstemController;
		this.opprettJiraService = opprettJiraService;
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.process(new MdcSetterProcessor())
				.logStackTrace(true)
				.log(ERROR, log, "Skanmotutgaaende feilet teknisk. Exception:${exception}");

		onException(GenericFileOperationFailedException.class)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


		from("cron:tab?schedule={{skanmotutgaaende.avstem.schedule}}")
				.pollEnrich("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
						"?{{skanmotutgaaende.endpointconfig}}" +
						"&antInclude=*.txt,*.TXT" +
						"&noop=true", CONNECTION_TIMEOUT)
				.routeId("verifiser_avstem_routeid")
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.log(INFO, log, "Skanmotutgaaende starter cron jobb for å avstemme referanser...")
				.process(new MdcSetterProcessor())
				.process(exchange -> {
					String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
					log.info("Mottok exchange med filnavn: " + fileName);
					log.info("Headers: " + exchange.getIn().getHeaders().toString());
				})
				.choice()
					.when(header(FILE_NAME).isNull())
					.process(exchange -> exchange.setProperty(EXCHANGE_AVSTEMT_DATO, finnForrigeVirkedag()))
					.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og se opprettet Jira-sak.")
					.bean(opprettJiraService)
					.log(INFO, log, "Skanmotutgaaende opprettet jira-sak med key=${body.jiraIssueKey} for manglende avstemmingsfil.")
				.endChoice()
				.process(new RemoveMdcProcessor())
				.end();


				from("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
								"?{{skanmotutgaaende.endpointconfig}}" +
								"&antInclude=*.txt,*.TXT"+
								"&maxMessagesPerPoll=10" +
								"&move=historiske" +
						"&scheduler=spring&scheduler.cron={{skanmotutgaaende.utgaaende.schedule}}")
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.routeId("avstem_routeid")
				.delay(delayMs)
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotutgaaende starter cron jobb for å avstemme referanser")
				.aggregate(constant(true), new GroupedExchangeAggregationStrategy())
				 .completionTimeout(500)
				.convertBodyTo(Set.class)
				.bean(avstemController)
				.log(INFO, log, "Skanmotutgaaende har kjørt cron jobb for å avstemme referanser")
				.process(new RemoveMdcProcessor())
				.end();

		from(FERDIG_ROUTE)
				.routeId("avstem_ferdig_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotutgaaende avstemte referanser ferdig.")
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/processed?{{skanmotutgaaende.endpointconfig}}")
				.process(new RemoveMdcProcessor())
				.end();


		from(FEIL_ROUTE)
				.routeId("avstem_feil_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Sender feilet melding til feilmappe.")
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/feil?{{skanmotutgaaende.endpointconfig}}")
				.process(new RemoveMdcProcessor())
				.end();
		// @formatter:on
	}
}
