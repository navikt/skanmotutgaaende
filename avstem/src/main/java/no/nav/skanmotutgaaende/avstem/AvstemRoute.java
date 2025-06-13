package no.nav.skanmotutgaaende.avstem;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.RemoveMdcProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 1500;
	private final AvstemController avstemController;

	public static String FEIL_ROUTE = "direct:feil";
	public static String FERDIG_ROUTE = "direct:ferdig";

	public AvstemRoute(AvstemController avstemController) {
		this.avstemController = avstemController;
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.process(new MdcSetterProcessor())
				.logStackTrace(true)
				.log(ERROR, log, "Skanmotutgaaende feilet teknisk. ${exception}");

		//For testing
		//Den feilende testen "shouldNotProcessAvstemmingsFileWhenJiraThrowException" blir fanget her..
		//Hvordan fullfører da routen slik at messagen blir moved til processed?
		//Setter her handled eksplisitt til false for å være 100% siker på at den ikke blir håndtert
		onException(JiraClientException.class)
				.handled(false)
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/feilet")
				.log(ERROR, log, "JiraError ${exception}");

		onException(GenericFileOperationFailedException.class)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");

		from("cron:tab?schedule={{skanmotutgaaende.avstem.schedule}}")
				.pollEnrich("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
						"?{{skanmotutgaaende.endpointconfig}}" +
						"&antInclude=*.txt,*.TXT" + //Prøver her å stoppe filen fra å bli overført til processed om det skjer en feil i routen.. Funker dårlig :(
						CONNECTION_TIMEOUT)
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.routeId("avstem_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotutgaaende starter cron jobb for å avstemme referanser...")
				.aggregate(constant(true), new GroupedExchangeAggregationStrategy())
				.completionTimeout(500)
				.convertBodyTo(Set.class)
				.bean(avstemController)
				.process(new RemoveMdcProcessor());

		from(FERDIG_ROUTE)
				.routeId("avstem_ferdig_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotutgaaende avstemte referanser ferdig.")
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/processed")
				.process(new RemoveMdcProcessor());


		from(FEIL_ROUTE)
				.routeId("avstem_feil_routeid")
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Sender feilet melding til feilmappe.")
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}/feil")
				.process(new RemoveMdcProcessor());
		// @formatter:on
	}
}
