package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.config.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksUtgaaendeRoute extends RouteBuilder {
    public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
    public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
    static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

    private final SkanmotutgaaendeProperties skanmotutgaaendeProperties;
    private final PostboksUtgaaendeService postboksUtgaaendeService;
    private final ErrorMetricsProcessor errorMetricsProcessor;

    @Inject
    public PostboksUtgaaendeRoute(SkanmotutgaaendeProperties skanmotutgaaendeProperties, PostboksUtgaaendeService postboksUtgaaendeService) {
        this.skanmotutgaaendeProperties = skanmotutgaaendeProperties;
        this.postboksUtgaaendeService = postboksUtgaaendeService;
        this.errorMetricsProcessor = new ErrorMetricsProcessor();
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(errorMetricsProcessor)
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

       // Kjente funksjonelle feil
        onException(AbstractSkanmotutgaaendeFunctionalException.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(errorMetricsProcessor)
                .log(LoggingLevel.WARN, log, "Skanmotutgaaende feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.WARN, log, "Skanmotutgaaende skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        from("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.inngaaendemappe}}" +
                "?{{skanmotutgaaende.endpointconfig}}" +
                "&delay=" + TimeUnit.SECONDS.toMillis(60) +
                "&antInclude=*.zip,*.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=10" +
                "&move=processed" +
                "&scheduler=spring&scheduler.cron={{skanmotutgaaende.schedule}}")
                .routeId("read_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Skanmotutgaaende starter behandling av fil=${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${file:name.noext.single}"))
                .process(new MdcSetterProcessor())
                .split(new ZipSplitter()).streaming()
                .aggregate(simple("${file:name.noext.single}"), new PostboksUtgaaendeSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(skanmotutgaaendeProperties.getCompletiontimeout().toMillis())
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(new MdcSetterProcessor())
                .process(exchange -> exchange.getIn().getBody(PostboksUtgaaendeEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
                .to("direct:process_utgaaende")
                .end() // aggregate
                .end() // split
                .process(new MdcRemoverProcessor())
                .log(LoggingLevel.INFO, log, "Skanmotutgaaende behandlet ferdig fil=${file:absolute.path}.");

        from("direct:process_utgaaende")
                .routeId("process_utgaaende")
                .process(new MdcSetterProcessor())
                .bean(postboksUtgaaendeService)
                .process(new MdcRemoverProcessor());

        from("direct:avvik")
                .routeId("avvik")
                .choice().when(body().isInstanceOf(PostboksUtgaaendeEnvelope.class))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}" +
                        "?{{skanmotutgaaende.endpointconfig}}")
                .otherwise()
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
                .end()
                .process(new MdcRemoverProcessor());
    }
}
