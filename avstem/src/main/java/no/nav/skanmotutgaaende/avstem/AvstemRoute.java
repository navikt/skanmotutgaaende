package no.nav.skanmotutgaaende.avstem;

import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.RemoveMdcProcessor;
import no.nav.skanmotutgaaende.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotutgaaende.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotutgaaende.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotutgaaende.jira.OpprettJiraService.finnForrigeVirkedag;
import static no.nav.skanmotutgaaende.jira.OpprettJiraService.parseDatoFraFilnavn;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMMINGSFIL_NAVN;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.builder.AggregationStrategies.useLatest;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 15000;
	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;
	private final TestService testService;
	private static String DD_MM_YYYY_WILDCARD = "??.??.????_*";

	public AvstemRoute(AvstemService avstemService,
					   OpprettJiraService opprettJiraService, TestService testService) {
		this.avstemService = avstemService;
		this.opprettJiraService = opprettJiraService;
		this.testService = testService;
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
						"&noop=true", //Setter no-op da vi ikke ønsker å flytte filene. Uten noop henter ikke routen filene fra sftp
						CONNECTION_TIMEOUT)
				.routeId("avstem_routeid")
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.aggregate(constant(true), new GroupedExchangeAggregationStrategy())
				.completionTimeout(2000) // wait 2 seconds for more files
				.process(exchange -> {
					System.out.println("test");
				})
				.choice()
					//.when(header(FILE_NAME).isNull())
					.when(exchange -> exchange.getIn().getBody() == null || exchange.getIn().getBody(String.class).isEmpty())
						.process(exchange -> exchange.setProperty(EXCHANGE_AVSTEMT_DATO, finnForrigeVirkedag()))
						.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og se opprettet Jira-sak.")
						.bean(opprettJiraService)
						.log(INFO, log, "Skanmotutgaaende opprettet jira-sak med key=${body.jiraIssueKey} for manglende avstemmingsfil.")
					.otherwise()
						.process(exchange -> {
							exchange.getIn().setBody(exchange.getIn().getBody(Set.class));
						})
					.bean(testService)
				.endChoice()
				.end();

		from("direct:behandle_liste")
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.process(exchange -> {
						System.out.println("Test");
						exchange.setProperty("originalExchange", exchange.getIn().getBody());
				})
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
				.endChoice()
				.log(INFO, log, "Skanmotutgaaende behandlet ferdig avstemmingsfil: ${file:name}")
				.process(exchange -> {
					System.out.println("Test");
					exchange.getIn().setBody(exchange.getProperty("originalExchange"));
				})
				//.toD("file:${header.camelFilePath}");
				.log(INFO, log, "Saknmotutgaaende flyutter fil til: {{skanmotutgaaende.filomraade.avstemmappe}}/processed/?move=${file:name}")
				.to("file:{{skanmotutgaaende.filomraade.avstemmappe}}/processed/${file:name}");
				//.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/processed");
		// @formatter:on
	}
}
