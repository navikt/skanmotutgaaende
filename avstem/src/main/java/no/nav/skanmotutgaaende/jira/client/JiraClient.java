package no.nav.skanmotutgaaende.jira.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiracore.config.JiraMapper;
import no.nav.dok.jiracore.config.JsonBodyHandler;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.dok.jiracore.interndomain.AnsvarligTeam;
import no.nav.dok.jiracore.interndomain.CompleteJiraIssue;
import no.nav.dok.jiracore.interndomain.CustomField;
import no.nav.dok.jiracore.interndomain.FlexibleInputFields;
import no.nav.dok.jiracore.interndomain.Issue;
import no.nav.dok.jiracore.interndomain.IssueInput;
import no.nav.dok.jiracore.interndomain.IssueType;
import no.nav.dok.jiracore.interndomain.IssueUpdateInput;
import no.nav.dok.jiracore.interndomain.JiraTransition;
import no.nav.dok.jiracore.interndomain.Project;
import no.nav.dok.jiracore.interndomain.SaksKategori;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.nav.dok.jiracore.config.JiraConstant.ATTACHMENT;
import static no.nav.dok.jiracore.config.JiraConstant.ISSUE_PATH;
import static no.nav.dok.jiracore.config.JiraConstant.ISSUE_TYPE_IKT_INCIDENT;
import static no.nav.dok.jiracore.config.JiraConstant.ISSUE_TYPE_MMA_OPPGAVE;
import static no.nav.dok.jiracore.config.JiraConstant.PROJECT_KEY_IKT;
import static no.nav.dok.jiracore.config.JiraConstant.PROJECT_KEY_TDH;
import static no.nav.dok.jiracore.config.JiraConstant.PROJECT_PATH;
import static no.nav.dok.jiracore.config.JiraConstant.TRANSITION;
import static no.nav.dok.jiracore.config.JiraConstant.TRANSITION_ID_KLAR_TIL_ARBEID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Jira api client bruker Java HttpClient til å gjøre kall mot jira
 */

@Slf4j
public class JiraClient {

	private final HttpClient httpClient;
	private final RestClient restClient;
	private final JiraProperties jiraProperties;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public JiraClient(JiraProperties jiraProperties) {

		this.jiraProperties = jiraProperties;
		objectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

		this.httpClient = HttpClient.newBuilder()
				.proxy(ProxySelector.getDefault())
				.connectTimeout(Duration.ofSeconds(15))
				.build();

		this.restClient = RestClient.builder()
				.baseUrl(jiraProperties.url())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setBasicAuth(jiraProperties.jiraServiceUser().username(), jiraProperties.jiraServiceUser().password());
					httpHeaders.set("X-Atlassian-Token", "no-check");
				})
				.build();
	}

	public Issue opprettMMAOppgaveJira(JiraRequest request) {
		return opprettJira(request, PROJECT_KEY_TDH, ISSUE_TYPE_MMA_OPPGAVE, Issue.class, Stream.empty());
	}

	public CompleteJiraIssue opprettIKTOppgaveJira(JiraRequest request, SaksKategori saksKategori, AnsvarligTeam ansvarligTeam, CustomField... customFields) {
		return opprettJira(request, PROJECT_KEY_IKT, ISSUE_TYPE_IKT_INCIDENT, CompleteJiraIssue.class,
				Stream.concat(Stream.of(saksKategori, ansvarligTeam), Stream.of(customFields)));
	}

	/**
	 * Opprett et Issue i MMA-prosjektet med type Oppgave
	 *
	 * @param request en request med de nødvendige feltene
	 * @return det nye issuet
	 * @deprecated Erstattes med opprettMMAOppgaveJira med klarere navngivning
	 */
	@Deprecated(forRemoval = true)
	public Issue opprettJira(JiraRequest request) {
		return opprettMMAOppgaveJira(request);
	}

	public <T> T opprettJira(JiraRequest request, String projectKey, Predicate<IssueType> issueTypePredicate, Class<T> responseType, Stream<CustomField> customFields) {
		Project project = hentProject(projectKey);
		IssueInput issueInput = JiraMapper.map(request, project, issueTypePredicate, customFields);
			String issueInputAsString = serialize(issueInput);
			log.info("issue request body: \n {}", issueInputAsString);

		try {
			HttpRequest httpRequest = httpRequestBuilder()
					.uri(URI.create(jiraProperties.url() + ISSUE_PATH))
					.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
					.POST(HttpRequest.BodyPublishers.ofString(issueInputAsString))
					.build();

			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			log.info("status={} body={}", response.statusCode(), response.body());
			if (response.statusCode() != HTTP_CREATED) {
				throw new JiraClientException(format("opprettJira feilet med status=%s feilmelding=%s", response.statusCode(), response.body()));
			}
			try {
				return objectMapper.readValue(response.body(), responseType);
			} catch (JsonProcessingException e) {
				throw new JiraClientException(format("opprettJira dekode feilet med feilmelding=%s body=%s", e.getMessage(), response.body()), e);
			}
		} catch (IOException | InterruptedException e) {
			throw new JiraClientException(format("opprettJira feilet med feilmelding=%s", e.getMessage()), e);
		}
	}

	public static ByteArrayResource vedleggFraByteArray(JiraRequest jiraRequest) {
		return new ByteArrayResource(jiraRequest.vedlegg());
	}

	public int leggTilVedlegg(String key, JiraRequest request) {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder
				.part("file", vedleggFraByteArray(request))
				.filename(request.filnavn() + ".csv");
		return restClient.post()
				.uri(uriBuilder -> uriBuilder.path(ISSUE_PATH + "/" + key + ATTACHMENT)
						.build())
				.header(CONTENT_TYPE, MULTIPART_FORM_DATA_VALUE)
				.body(multipartBodyBuilder.build())
				.exchange((req, res) -> {
					if (!res.getStatusCode().is2xxSuccessful()) {
						throw new JiraClientException(format("leggTilVedlegg feilet med feilmelding=%s og status=%s", res.getBody(), res.getStatusCode()));
					}
					return res.getStatusCode().value();
				});
	}

	private Project hentProject(String projectKey) {
		try {
			HttpRequest httpRequest = httpRequestBuilder()
					.uri(URI.create(jiraProperties.url() + PROJECT_PATH + projectKey))
					.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
					.timeout(Duration.ofSeconds(30))
					.GET()
					.build();

			HttpResponse<Project> response = httpClient.send(httpRequest, new JsonBodyHandler<>(Project.class));

			if (response.statusCode() != HTTP_OK) {
				throw new JiraClientException(format("hentProject feilet med status=%s, feilmelding=%s", response.statusCode(), response.headers()));
			}
			return response.body();
		} catch (Exception e) {
			throw new JiraClientException(format("hentProject feilet med feilmelding=%s", e.getMessage()), e);
		}
	}


	public <T> T oppdaterJiraIssue(String key, Class<T> responseType, Stream<CustomField> customFields) {
		return oppdaterJiraIssue(key, responseType, new IssueUpdateInput(new FlexibleInputFields(JiraMapper.mapCustomFields(customFields))));
	}

	public <T> T oppdaterJiraIssue(String key, Class<T> responseType, IssueUpdateInput issueUpdateInput) {
		try {
			HttpRequest httpRequest = httpRequestBuilder().uri(URI.create(jiraProperties.url() + ISSUE_PATH + "/" + key))
					.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
					.PUT(HttpRequest.BodyPublishers.ofString(serialize(issueUpdateInput)))
					.build();
			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 400) {
				throw new JiraClientException(format("oppdaterJiraIssue feilet med status=%s feilmelding=%s", response.statusCode(), response.body()));
			}
			return objectMapper.readValue(response.body(), responseType);

		} catch (Exception e) {
			throw new JiraClientException(format("oppdaterJiraStatus feilet med feilmelding=%s", e.getMessage()), e.getCause());
		}
	}


	public Issue oppdaterJiraStatusTilKlarForArbeid(final String key) {
		try {
			JiraTransition transition = new JiraTransition(new JiraTransition.Transition(TRANSITION_ID_KLAR_TIL_ARBEID));
			HttpRequest httpRequest = httpRequestBuilder().uri(URI.create(jiraProperties.url() + ISSUE_PATH + "/" + key + TRANSITION))
					.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
					.POST(HttpRequest.BodyPublishers.ofString(serialize(transition)))
					.build();
			httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			return hentIssue(key);

		} catch (Exception e) {
			throw new JiraClientException(format("oppdaterJiraStatus feilet med feilmelding=%s", e.getMessage()), e.getCause());
		}
	}

	private Issue hentIssue(final String key) {
		HttpRequest httpRequest = httpRequestBuilder().uri(URI.create(jiraProperties.url() + ISSUE_PATH + "/" + key))
				.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.GET()
				.build();
		try {
			HttpResponse<Issue> response = httpClient.send(httpRequest, new JsonBodyHandler<>(Issue.class));
			if (response.statusCode() != HTTP_OK) {
				throw new JiraClientException(format("hentIssue feilet med status=%s, feilmelding=%s", response.statusCode(), response.headers()));
			}
			return response.body();
		} catch (IOException | InterruptedException e) {
			throw new JiraClientException(format("hentIssue feilet med feilmelding=%s", e.getMessage()), e.getCause());
		}
	}

	private String getBasicAuthenticationHeader() {
		String valueToEncode = jiraProperties.jiraServiceUser().username() + ":" + jiraProperties.jiraServiceUser().password();
		return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}

	private String serialize(Object object) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private HttpRequest.Builder httpRequestBuilder() {
		return HttpRequest.newBuilder()
				.header(AUTHORIZATION, getBasicAuthenticationHeader())
				.header("X-Atlassian-Token", "no-check");
	}
}
