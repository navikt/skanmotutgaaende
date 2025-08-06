package no.nav.skanmotutgaaende.jira.client;

import jakarta.validation.ValidationException;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiracore.interndomain.AnsvarligTeam;
import no.nav.dok.jiracore.interndomain.CompleteJiraIssue;
import no.nav.dok.jiracore.interndomain.Issue;

import java.net.URI;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dok.jiracore.config.JiraConstant.BEROERT_TJENESTE_DOKUMENTLOESNINGER;
import static no.nav.dok.jiracore.config.JiraConstant.BROWSE;
import static no.nav.dok.jiracore.config.JiraConstant.NO_CONTENT_STATUS_CODE;
import static no.nav.dok.jiracore.config.JiraConstant.OK_STATUS_CODE;
import static no.nav.dok.jiracore.config.JiraConstant.SAKSKATEGORI_TJENESTE_UTILGJENGELIG;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class JiraService {
	private final JiraClient jiraClient;

	public JiraService(JiraClient jiraClient) {
		this.jiraClient = jiraClient;
	}

	public JiraResponse opprettJiraOppgave(JiraRequest jiraRequest) {

		Issue issue = jiraClient.opprettMMAOppgaveJira(jiraRequest);

		jiraClient.oppdaterJiraStatusTilKlarForArbeid(issue.key());

		return JiraResponse.builder().jiraIssueKey(issue.key())
				.message(responseUrl(issue.self(), issue.key()))
				.httpStatusCode(OK_STATUS_CODE)
				.build();
	}

	/**
	 * @param jiraRequest jira requesten mapper JiraRequest og requesten bruker til å opprette jira sak.
	 * @return metoden opprette jira oppgave ved vedlegg og returnerer jira key, melding og httpstatus
	 */
	public JiraResponse opprettJiraOppgaveMedVedlegg(JiraRequest jiraRequest) {

		if (jiraRequest.vedlegg() == null || jiraRequest.vedlegg().length == 0) {
			return JiraResponse.builder()
					.message("Kan ikke opprette Jira-sak. Fant ingen vedlegg fil")
					.httpStatusCode(NO_CONTENT_STATUS_CODE)
					.build();
		}

		Issue issue = jiraClient.opprettMMAOppgaveJira(jiraRequest);

		assertNotNullOrEmpty("key", issue.key());
		jiraClient.leggTilVedlegg(issue.key(), jiraRequest);

		Issue oppdaterOppgave = jiraClient.oppdaterJiraStatusTilKlarForArbeid(issue.key());

		return JiraResponse.builder().jiraIssueKey(issue.key())
				.message(responseUrl(issue.self(), issue.key()))
				.status(getStatus(oppdaterOppgave))
				.httpStatusCode(OK_STATUS_CODE)
				.build();
	}

	public JiraResponse opprettJiraIKTOppgave(JiraRequest jiraRequest, AnsvarligTeam ansvarligTeam) {
		CompleteJiraIssue issue = jiraClient.opprettIKTOppgaveJira(jiraRequest,
				SAKSKATEGORI_TJENESTE_UTILGJENGELIG,
				ansvarligTeam,
				BEROERT_TJENESTE_DOKUMENTLOESNINGER
		);

		return JiraResponse.builder().jiraIssueKey(issue.getKey())
				.message(responseUrl(issue.getSelf(), issue.getKey()))
				.httpStatusCode(OK_STATUS_CODE)
				.build();
	}

	public static void assertNotNullOrEmpty(String field, String value) {
		if (isBlank(value)) {
			throw new ValidationException(format("Feltet %s kan ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	private String responseUrl(String self, String key) {
		URI uri = URI.create(self);
		return "https://" + uri.getHost() + BROWSE + key;
	}

	private String getStatus(Issue issue) {
		if (isNull(issue.fields()) || isNull(issue.fields().status())) {
			return null;
		}
		return issue.fields().status().name();
	}
}
