package no.nav.skanmotutgaaende.jira;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotutgaaende.client.JiraClient;
import no.nav.skanmotutgaaende.client.JiraService;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeTechnicalException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static no.nav.dok.jiracore.config.JiraConstant.ANSVARLIG_TEAM_FAGPOST;
import static no.nav.skanmotutgaaende.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME_ONLY;

@Slf4j
@Component
public class OpprettJiraService {

	private static final List<String> LABEL = List.of("Skanmotutgaaende_avvik");
	private static final String DESCRIPTION = "Se vedlegg for en oversikt over manglende avstemmingsreferanser for skannede dokumenter fra Skanmotutgaaende \n";
	private static final String SUMMARY = "Skanmotutgaaende: Manglende avstemmingsreferanser for skannede dokumenter";
	public static final String ANTALL_FILER_AVSTEMT = "Antall filer avstemt";
	public static final String ANTALL_FILER_FEILET = "Antall filer feilet";

	private final JiraService jiraService;
	private final JiraClient jiraClient;

	public OpprettJiraService(JiraService jiraService, JiraClient jiraClient) {
		this.jiraService = jiraService;
		this.jiraClient = jiraClient;
	}

	@Handler
	public JiraResponse opprettAvstemJiraOppgave(byte[] csvByte, Exchange exchange) {
		LocalDate avstemmingsfilDato = exchange.getProperty(EXCHANGE_AVSTEMT_DATO, LocalDate.class);
		Integer antallAvstemt = exchange.getProperty(ANTALL_FILER_AVSTEMT, Integer.class);
		Integer antallFeilet = exchange.getProperty(ANTALL_FILER_FEILET, Integer.class);
		return opprettAvstemJiraOppgave(csvByte, antallAvstemt, antallFeilet, avstemmingsfilDato);
	}

	private JiraResponse opprettAvstemJiraOppgave(byte[] csvByte, Integer antallAvstemt, Integer antallFeilet, LocalDate avstemmingsfilDato) {
		try {
			if (csvByte == null) {
				return opprettJiraForManglendeAvstemmingsfil(avstemmingsfilDato);
			}

			return opprettJiraForAvstemmingsfil(csvByte, antallAvstemt, antallFeilet, avstemmingsfilDato);
		} catch (JiraClientException e) {
			throw new SkanmotutgaaendeTechnicalException("kunne ikke opprette jira oppgave", e);
		}
	}

	private JiraResponse opprettJiraForManglendeAvstemmingsfil(LocalDate avstemmingsfilDato) {
		return jiraService.opprettJiraIKTOppgave(JiraRequest.builder()
						.summary("Skanmotutgaaende: Avstemmingfil mangler for " + avstemmingsfilDato)
						.description("Skanmotutgaaende fant ikke avstemmingsfil for " + avstemmingsfilDato + ". Undersøk tilfellet og kontakt evt. Iron Mountain.")
						.labels(LABEL)
						.build(),
				ANSVARLIG_TEAM_FAGPOST);
	}

	private JiraResponse opprettJiraForAvstemmingsfil(byte[] csvByte, Integer antallAvstemt, Integer antallFeilet, LocalDate avstemmingsfilDato) {
		JiraRequest jiraRequest = JiraRequest.builder()
				.summary(SUMMARY)
				.description(prettifySummary(DESCRIPTION, antallAvstemt, antallFeilet))
				.labels(LABEL)
				.vedlegg(csvByte)
				.avstemmingsfilDato(avstemmingsfilDato)
				.build();

		JiraResponse jiraResponse = jiraService.opprettJiraIKTOppgave(jiraRequest, ANSVARLIG_TEAM_FAGPOST);
		jiraClient.leggTilVedlegg(jiraResponse.jiraIssueKey(), jiraRequest);

		return jiraResponse;
	}

	public static String prettifySummary(String melding, int antallAvstemt, int antallFeilet) {
		var builder = new StringBuilder();
		return builder.append(melding)
				.append("\nAntall filer avstemt: ").append(antallAvstemt)
				.append("\nAntall filer funnet: ").append(antallAvstemt - antallFeilet)
				.append("\nAntall filer feilet: ").append(antallFeilet).toString();
	}

	/**
	 * finnForrigeVirkedag genererer datoen for forrige virkedag, som kan brukes i loggen og til å opprette Jira-saken.
	 */
	public static LocalDate finnForrigeVirkedag() {
		LocalDate todaysDate = LocalDate.now(ZoneId.of("Europe/Oslo"));
		return MONDAY.equals(todaysDate.getDayOfWeek()) ? todaysDate.minusDays(3) :
				todaysDate.minusDays(1);
	}

	public static LocalDate parseDatoFraFilnavn(Exchange exchange) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		return LocalDate.parse(exchange.getIn().getHeader(FILE_NAME_ONLY, String.class).substring(0, 10), formatter);
	}
}
