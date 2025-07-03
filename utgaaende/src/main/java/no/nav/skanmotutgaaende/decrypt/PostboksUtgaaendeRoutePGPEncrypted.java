package no.nav.skanmotutgaaende.decrypt;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.ErrorMetricsProcessor;
import no.nav.skanmotutgaaende.MdcRemoverProcessor;
import no.nav.skanmotutgaaende.MdcSetterProcessor;
import no.nav.skanmotutgaaende.SkanningmetadataUnmarshaller;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import no.nav.skanmotutgaaende.slack.SlackService;
import no.nav.skanmotutgaaende.utgaaende.PostboksUtgaaendeEnvelope;
import no.nav.skanmotutgaaende.utgaaende.PostboksUtgaaendeService;
import no.nav.skanmotutgaaende.utgaaende.PostboksUtgaaendeSkanningAggregator;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.skanmotutgaaende.metrics.DokCounter.DOMAIN;
import static no.nav.skanmotutgaaende.metrics.DokCounter.UTGAAENDE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Slf4j
@Component
public class PostboksUtgaaendeRoutePGPEncrypted extends RouteBuilder {

	private static final String PGP_AVVIK = "direct:pgp_encrypted_avvik_utgaaende";
	private static final String PGP_FAGPOST_AVVIK = "direct:pgp_encrypted_fagpost_avvik";
	private static final String PROCESS_PGP_ENCRYPTED = "direct:pgp_encrypted_process_utgaaende";
	private static final String SEND_SLACKMELDING_RUTE = "direct:sendSlackmelding";
	public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
	public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
	public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
	public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
	static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

	private final SkanmotutgaaendeProperties skanmotutgaaendeProperties;
	private final PostboksUtgaaendeService postboksUtgaaendeService;
	private final PgpDecryptService pgpDecryptService;
	private final SlackService slackService;

	public PostboksUtgaaendeRoutePGPEncrypted(
			SkanmotutgaaendeProperties skanmotutgaaendeProperties,
			PostboksUtgaaendeService postboksUtgaaendeService,
			PgpDecryptService pgpDecryptService,
			SlackService slackService) {
		this.skanmotutgaaendeProperties = skanmotutgaaendeProperties;
		this.postboksUtgaaendeService = postboksUtgaaendeService;
		this.pgpDecryptService = pgpDecryptService;
		this.slackService = slackService;
	}

	@Override
	public void configure() {
		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotutgaaende-pgp feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
				.to(PGP_AVVIK)
				.log(ERROR, log, "Skanmotutgaaende-pgp skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.setBody(simple("Innlesing av fil feilet teknisk med exception=${exception.getClass().getName()}."))
				.to(SEND_SLACKMELDING_RUTE);

		// Får ikke dekryptert .zip.pgp - mest sannsynlig mismatch mellom private key og public key
		onException(PGPException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotutgaaende-pgp feilet i dekryptering av .zip.pgp for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip.pgp"))
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}" +
						"?{{skanmotutgaaende.endpointconfig}}")
				.log(ERROR, log, "Skanmotutgaaende-pgp skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.end()
				.process(new MdcRemoverProcessor())
				.setBody(simple("Innlesing av fil feilet dekryptering med exception=${exception.getClass().getName()}."))
				.to(SEND_SLACKMELDING_RUTE);

		// Kjente funksjonelle feil
		onException(AbstractSkanmotutgaaendeFunctionalException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(WARN, log, "Skanmotutgaaende-pgp feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
				.to(PGP_FAGPOST_AVVIK);

		from(SEND_SLACKMELDING_RUTE)
				.bean(slackService, "sendMelding(${body})");

		from("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.inngaaendemappe}}" +
				"?{{skanmotutgaaende.endpointconfig}}" +
				"&delay=" + TimeUnit.SECONDS.toMillis(60) +
				"&antInclude=*.zip.pgp,*.ZIP.pgp" +
				"&initialDelay=1000" +
				"&maxMessagesPerPoll=10" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotutgaaende.utgaaende.schedule}}")
				.routeId("read_encrypted_pgp_utgaaende_zip_from_sftp")
				.log(INFO, log, "Skanmotutgaaende-pgp starter behandling av fil=${file:absolute.path}.")
				.setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
				.process(exchange -> exchange.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, cleanDotPgpExtension(simple("${file:name.noext.single}"), exchange)))
				.process(new MdcSetterProcessor())
				.bean(pgpDecryptService)
				.split(new ZipSplitter()).streaming()
					.aggregate(simple("${file:name.noext.single}"), new PostboksUtgaaendeSkanningAggregator())
						.completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
						.completionTimeout(skanmotutgaaendeProperties.getCompletiontimeout().toMillis())
						.setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
						.process(new MdcSetterProcessor())
						.process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DOMAIN, UTGAAENDE)))
						.process(exchange -> exchange.getIn().getBody(PostboksUtgaaendeEnvelope.class).validate())
						.bean(new SkanningmetadataUnmarshaller())
						.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
						.to(PROCESS_PGP_ENCRYPTED)
					.end() // aggregate
				.end() // split
				.process(new MdcRemoverProcessor())
				.log(INFO, log, "Skanmotutgaaende-pgp behandlet ferdig fil=${file:absolute.path}.");

		from(PROCESS_PGP_ENCRYPTED)
				.routeId(PROCESS_PGP_ENCRYPTED)
				.process(new MdcSetterProcessor())
				.bean(postboksUtgaaendeService)
				.process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DOMAIN, UTGAAENDE)))
				.process(new MdcRemoverProcessor());

		from(PGP_AVVIK)
				.routeId("pgp_encrypted_avvik_utgaaende")
				.choice().when(body().isInstanceOf(PostboksUtgaaendeEnvelope.class))
					.setBody(simple("${body.createZip}"))
					.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.feilmappe}}" +
							"?{{skanmotutgaaende.endpointconfig}}")
				.otherwise()
					.log(ERROR, log, "Skanmotutgaaende teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
				.end()
				.process(new MdcRemoverProcessor());

		from(PGP_FAGPOST_AVVIK)
				.routeId("pgp_encrypted_fagpost_avvik")
				.setBody(simple("${body.createZip}"))
				.to("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.fagpostmappe}}" +
						"?{{skanmotutgaaende.endpointconfig}}")
				.log(WARN, log, "Skanmotutgaaende-pgp skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.process(new MdcRemoverProcessor());

		// @formatter:on
	}

	// Input blir .zip siden .pgp er strippet bort
	private String cleanDotPgpExtension(ValueBuilder value1, Exchange exchange) {
		String stringRepresentation = value1.evaluate(exchange, String.class);
		if (stringRepresentation.contains(".zip")) {
			return stringRepresentation.replace(".zip", "");
		}
		return stringRepresentation;
	}
}
