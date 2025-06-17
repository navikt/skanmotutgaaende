package no.nav.skanmotutgaaende.avstem;

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
				.log(ERROR, log, "Skanmotutgaaende feilet teknisk. Exception:${exception}");

		onException(GenericFileOperationFailedException.class)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotutgaaende fant ikke avstemmingsfil. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


		from("cron:tab?schedule={{skanmotutgaaende.avstem.schedule}}")
				.pollEnrich("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
								"?{{skanmotutgaaende.endpointconfig}}" +
								"&antInclude=*.txt,*.TXT"+
								"&maxMessagesPerPoll=10" +
								"&move=historiske",
								CONNECTION_TIMEOUT)
				.autoStartup("{{skanmotutgaaende.avstem.startup}}")
				.routeId("avstem_routeid")
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
