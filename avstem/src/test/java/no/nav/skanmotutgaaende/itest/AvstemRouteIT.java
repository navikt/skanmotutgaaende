package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.spi.RouteController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AvstemRouteIT extends AbstractItest {

	private static final String AVSTEMMINGSFILMAPPE = "avstemmappe";
	private static final String PROCESSED = "processed";
	private static final String AVSTEMMINGSFIL = "04.01.2024_avstemmingsfil_1.txt";

	@Autowired
	private Path sshdPath;

	@Autowired
	private CamelContext camelContext;

	@BeforeEach
	void beforeEach() {
		super.setUpStubs();
		final Path avstem = sshdPath.resolve(AVSTEMMINGSFILMAPPE);
		final Path processed = avstem.resolve(PROCESSED);
		preparePath(avstem);
		preparePath(processed);
		startRoutes();
	}

	@SneakyThrows
	@AfterEach
	void cleanUp() {
		WireMock.removeAllMappings();
		stopRoutes();
	}

	@Test
	public void shouldOpprettJiraOppgaveForFeilendeAvstemreferanser() throws IOException {
		stubJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");

		copyFileFromClasspathToAvstem();

		// Vent til filer ligger klare
		await().atMost(ofSeconds(5))
				.untilAsserted(() -> {
					assertAntallUbehandledeFiler(1);
					assertAntallProsesserteFiler(0);
				});

		await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertAntallUbehandledeFiler(0);
					assertAntallProsesserteFiler(1);
					verifyRequest();
				});

		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))) {
			List<String> processedMappe = files.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
					.toList();
			assertThat(processedMappe).containsExactly(AVSTEMMINGSFIL);
		}
	}

	@Test
	public void shouldNotOpprettJiraWhenFeilendeAvstemReferanserIsNull() throws IOException {
		stubPostAvstemJournalpost("journalpostapi/null-avstem.json");

		copyFileFromClasspathToAvstem();

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);

		assertThat(Files.exists(filePath)).isTrue();
		assertAntallProsesserteFiler(0);

		await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(1);
					verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
				});
	}

	@Test
	public void shouldNotProcessAvstemmingsFileWhenJiraThrowException() throws IOException {
		stubBadRequestJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");

		copyFileFromClasspathToAvstem();

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);
		assertThat(Files.exists(filePath)).isTrue();

		assertAntallProsesserteFiler(0);

		await().atMost(ofSeconds(15))
				.untilAsserted(() -> {
					verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
				});
		assertAntallProsesserteFiler(0);
		assertAntallUbehandledeFiler(1);
	}

	@Test
	public void shouldOpprettJiraOppgaveWhenAvstemmingsfilIsMissing() {
		stubJiraOpprettOppgave();

		await().atMost(ofSeconds(10))
				.untilAsserted(() -> {
					verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
					verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
				});
	}

	@SneakyThrows
	private void startRoutes() {
		AdviceWith.adviceWith(camelContext, "ftp-trigger", a -> {
			a.replaceFromWith("{{skanmotutgaaende.endpointuri}}/{{skanmotutgaaende.filomraade.avstemmappe}}" +
					"?{{skanmotutgaaende.endpointconfig}}" +
					"&include=^.*" +
					"&sendEmptyMessageWhenIdle=true" +
					"&move=processed");
		});
		RouteController routeController = camelContext.getRouteController();
		routeController.startRoute("ftp-trigger");
	}

	@SneakyThrows
	private void stopRoutes() {
		RouteController routeController = camelContext.getRouteController();
		routeController.stopRoute("ftp-trigger");
	}

	private void verifyRequest() {
		verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
		verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
		verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
	}

	private void copyFileFromClasspathToAvstem() throws IOException {
		Files.copy(new ClassPathResource(AvstemRouteIT.AVSTEMMINGSFIL).getInputStream(), sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AvstemRouteIT.AVSTEMMINGSFIL));
	}

	private void preparePath(Path path) {
		try {
			if (!Files.exists(path)) {
				Files.createDirectory(path);
			} else {
				FileUtils.cleanDirectory(path.toFile());
			}
		} catch (IOException ignored) {

		}
	}

	@SneakyThrows
	private void assertAntallProsesserteFiler(int forventetAntallFiler) {
		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))) {
			assertThat(files.filter(path -> !Files.isDirectory(path))
					.collect(Collectors.toSet())).hasSize(forventetAntallFiler);
		}
	}

	@SneakyThrows
	private void assertAntallUbehandledeFiler(int forventetAntallFiler) {
		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE))) {
			assertThat(files.filter(path -> !Files.isDirectory(path))
					.collect(Collectors.toSet())).hasSize(forventetAntallFiler);
		}
	}
}
