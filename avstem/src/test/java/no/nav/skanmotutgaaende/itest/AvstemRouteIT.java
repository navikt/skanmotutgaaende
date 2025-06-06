package no.nav.skanmotutgaaende.itest;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;

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
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AvstemRouteIT extends AbstractItest {

	private static final String AVSTEMMINGSFILMAPPE = "avstemmappe";
	private static final String PROCESSED = "processed";
	private static final String AVSTEMMINGSFIL = "04.01.2024_avstemmingsfil_1.txt";
	private static final String AVSTEMMINGSFIL2 = "04.01.2024_avstemmingsfil_2.txt";

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		super.setUpStubs();
		final Path avstem = sshdPath.resolve(AVSTEMMINGSFILMAPPE);
		final Path processed = avstem.resolve(PROCESSED);
		preparePath(avstem);
		preparePath(processed);
		System.out.println("processed: " + processed.toAbsolutePath());
		System.out.println("avstem: " + avstem.toAbsolutePath());
	}

	@Test
	public void shouldOpprettJiraOppgaveForFeilendeAvstemreferanser() throws IOException {
		stubJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");

		copyFileFromClasspathToAvstem();

		// Vent til filer ligger klare
		await().atMost(ofSeconds(5))
				.untilAsserted(() -> {
					assertAntallUbehandledeFiler(2);
					assertAntallProsesserteFiler(0);
				});

		await()
				.atMost(ofSeconds(70))
				.pollDelay(ofMillis(500))
				.untilAsserted(() -> {
					assertAntallUbehandledeFiler(0);
					assertAntallProsesserteFiler(2);
					verifyRequest();
				});

		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))) {
			List<String> processedMappe = files.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
					.toList();
			assertThat(processedMappe).containsExactly(AVSTEMMINGSFIL, AVSTEMMINGSFIL2);
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
				.atMost(ofSeconds(7))
				.pollDelay(ofMillis(500))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(2);
					verify(2, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
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

		await().atMost(ofSeconds(7))
				.pollDelay(ofMillis(500))
				.untilAsserted(() -> {
					verify(2, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
				});
		assertAntallProsesserteFiler(0);
		assertAntallUbehandledeFiler(2);
	}

	@Test
	public void shouldOpprettJiraOppgaveWhenAvstemmingsfilIsMissing() throws InterruptedException {
		stubJiraOpprettOppgave();
		Thread.sleep(1000);

		await().atMost(ofSeconds(7))
				.pollDelay(ofMillis(500))
				.untilAsserted(() -> {
					assertAntallUbehandledeFiler(0);
					assertAntallProsesserteFiler(0);
					verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
					verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
				});
	}

	private void verifyRequest() {
		verify(2, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
		verify(2, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
		verify(2, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
	}

	private void copyFileFromClasspathToAvstem() throws IOException {
		Files.copy(new ClassPathResource(AvstemRouteIT.AVSTEMMINGSFIL).getInputStream(), sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AvstemRouteIT.AVSTEMMINGSFIL));
		Files.copy(new ClassPathResource(AvstemRouteIT.AVSTEMMINGSFIL2).getInputStream(), sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AvstemRouteIT.AVSTEMMINGSFIL2));
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
