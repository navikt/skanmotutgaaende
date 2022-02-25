package no.nav.skanmotutgaaende.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import no.nav.skanmotutgaaende.ErrorMetricsProcessor;
import no.nav.skanmotutgaaende.MdcRemoverProcessor;
import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.PostboksUtgaaendeEnvelope;
import no.nav.skanmotutgaaende.PostboksUtgaaendeService;
import no.nav.skanmotutgaaende.PostboksUtgaaendeSkanningAggregator;
import no.nav.skanmotutgaaende.SkanningmetadataUnmarshaller;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@Slf4j
@Component
public class PostboksUtgaaendeRouteEncrypted extends RouteBuilder {
    public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
    public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
    static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

    private final SkanmotutgaaendeProperties skanmotutgaaendeProperties;
    private final PostboksUtgaaendeService postboksUtgaaendeService;
    private final ErrorMetricsProcessor errorMetricsProcessor;
    private final String passphrase;

    @Autowired
    public PostboksUtgaaendeRouteEncrypted(SkanmotutgaaendeProperties skanmotutgaaendeProperties,
                                           PostboksUtgaaendeService postboksUtgaaendeService,
                                           @Value("${passphrase}") String passphrase) {
        this.skanmotutgaaendeProperties = skanmotutgaaendeProperties;
        this.postboksUtgaaendeService = postboksUtgaaendeService;
        this.errorMetricsProcessor = new ErrorMetricsProcessor();
        this.passphrase = passphrase;
    }

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(errorMetricsProcessor)
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.enc.zip"))
                .to("direct:encrypted_avvik")
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        onException(ZipException.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(errorMetricsProcessor)
                .log(LoggingLevel.WARN, log, "Feil passord for en fil " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.enc.zip"))
                .to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}" +
                        "?{{skanmotutgaaende.endpointconfig}}")
                .log(LoggingLevel.WARN, log, "Skanmotutgaaende skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
                .end()
                .process(new MdcRemoverProcessor());

        // Kjente funksjonelle feil
        onException(AbstractSkanmotutgaaendeFunctionalException.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(errorMetricsProcessor)
                .log(LoggingLevel.WARN, log, "Skanmotutgaaende feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.enc.zip"))
                .to("direct:encrypted_avvik")
                .log(LoggingLevel.WARN, log, "Skanmotutgaaende skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        from("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.inngaaendemappe}}" +
                "?{{skanmotutgaaende.endpointconfig}}" +
                "&delay=" + TimeUnit.SECONDS.toMillis(60) +
                "&antInclude=*enc.zip,*enc.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=10" +
                "&move=processed" +
                "&scheduler=spring&scheduler.cron={{skanmotutgaaende.schedule}}")
                .routeId("read_encrypted_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "SkanmotutgaaendeDecrypt starter behandling av fil=${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .process(exchange -> exchange.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, cleanDotEncExtension(simple("${file:name.noext.single}"), exchange)))
                .process(new MdcSetterProcessor())
                .split(new ZipSplitterEncrypted(passphrase)).streaming()
                .aggregate(simple("${file:name.noext.single}"), new PostboksUtgaaendeSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(skanmotutgaaendeProperties.getCompletiontimeout().toMillis())
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(new MdcSetterProcessor())
                .process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DokCounter.DOMAIN, DokCounter.UTGAAENDE)))
                .process(exchange -> exchange.getIn().getBody(PostboksUtgaaendeEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
                .to("direct:encrypted_process_utgaaende")
                .end() // aggregate
                .end() // split
                .process(new MdcRemoverProcessor())
                .log(LoggingLevel.INFO, log, "Skanmotutgaaende behandlet ferdig fil=${file:absolute.path}.");

        from("direct:encrypted_process_utgaaende")
                .routeId("encrypted_process_utgaaende")
                .process(new MdcSetterProcessor())
                .bean(postboksUtgaaendeService)
                .process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DokCounter.DOMAIN, DokCounter.UTGAAENDE)))
                .process(new MdcRemoverProcessor());

        from("direct:encrypted_avvik")
                .routeId("encrypted_avvik")
                .choice().when(body().isInstanceOf(PostboksUtgaaendeEnvelope.class))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}" +
                        "?{{skanmotutgaaende.endpointconfig}}")
                .otherwise()
                .log(LoggingLevel.ERROR, log, "Skanmotutgaaende teknisk feil der " + KEY_LOGGING_INFO +
                        ". ikke ble flyttet til feilområde. Må analyseres. {{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}\" +\n" +
                        "                        \"?{{skanmotutgaaende.endpointconfig}}")
                .end()
                .process(new MdcRemoverProcessor());
    }

    private String cleanDotEncExtension(ValueBuilder value1, Exchange exchange) {
        String stringRepresentation = value1.evaluate(exchange, String.class);
        if (stringRepresentation.contains(".enc")) {
            return stringRepresentation.replace(".enc", "");
        }
        return stringRepresentation;
    }
}
