package no.nav.skanmotutgaaende.itest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PostboksUtgaaendeRouteIT extends AbstractIT {

	private final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456780_1_1000";
	private final String ZIP_FILE_NAME_NO_EXTENSION_ENCRYPTED = "01.07.2020_R123456780_1_1000.encrypted";
	private final String ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION = "01.07.2020_R200000000_1_1000_ordered_xml_first_big";

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		stubAzureToken();
		final Path inngaaende = sshdPath.resolve(INNGAAENDE);
		final Path processed = inngaaende.resolve("processed");
		final Path feilmappe = sshdPath.resolve(FEILMAPPE);
		try {
			preparePath(inngaaende);
			preparePath(processed);
			preparePath(feilmappe);
		} catch (Exception e) {
			//noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
		}
	}

	@Test
	public void shouldLesOgLagreHappy() throws IOException {
		// 01.07.2020_R123456780_1_1000.zip
		// OK   - 01.07.2020_R123456780_0001
		// OK   - 01.07.2020_R123456780_0002 (mangler filnavn og fysiskPostboks)
		// FEIL - 01.07.2020_R123456780_0003 (valideringsfeil, mangler journalpostid)
		// FEIL - 01.07.2020_R123456780_0004 (vil feile hos dokarkiv 400_Bad_Request)
		// FEIL - 01.07.2020_R123456780_0005 (mangler pdf)
		// FEIL - 01.07.2020_R123456780_0006 (mangler xml)
		setUpHappyStubs();
		setUpBadStubs();
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
						.resolve(ZIP_FILE_NAME_NO_EXTENSION)))
						.hasSize(2);
				assertThat(Files.list(sshdPath.resolve(FAGPOST_MAPPE)
						.resolve(ZIP_FILE_NAME_NO_EXTENSION)))
						.hasSize(2);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = fetchFileSecurely(sshdPath, FEILMAPPE, ZIP_FILE_NAME_NO_EXTENSION).stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.toList();

		assertTrue(feilmappeContents.containsAll(List.of(
				"01.07.2020_R123456780_0005-teknisk.zip",
				"01.07.2020_R123456780_0006-teknisk.zip"
		)));

		final List<String> fagpostmappeContents = fetchFileSecurely(sshdPath, FAGPOST_MAPPE, ZIP_FILE_NAME_NO_EXTENSION).stream()
				.map(p -> p.getFileName().toString())
				.toList();

		assertTrue(fagpostmappeContents.containsAll(List.of(
				"01.07.2020_R123456780_0003.zip",
				"01.07.2020_R123456780_0004.zip"
		)));

		verify(exactly(3), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}

	@Test
	public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
		// 01.07.2020_R200000000_1_1000_ordered_xml_first_big.zip
		// OK   - 01.07.2020_R100000000_0001
		// OK   - 01.07.2020_R100000000_0002 (mangler filnavn og fysiskPostboks)
		// FEIL - 01.07.2020_R100000000_0003 (valideringsfeil, mangler journalpostid)
		// FEIL - 01.07.2020_R200000000_0004 (vil feile hos dokarkiv 409_Conflict)
		// FEIL - 01.07.2020_R100000000_0005 (mangler pdf)
		// FEIL - 01.07.2020_R100000000_0006 (mangler xml)
		// OK   - 01.07.2020_R100000000_0007
		// ...
		// OK   - 01.07.2020_R100000000_0059
		setUpHappyStubs();
		setUpConflictStubs();
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION + ".zip");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
						.resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION)))
						.hasSize(2);
				assertThat(Files.list(sshdPath.resolve(FAGPOST_MAPPE)
						.resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION)))
						.hasSize(2);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = fetchFileSecurely(sshdPath, FEILMAPPE, ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION).stream()
				.map(Path::getFileName)
				.map(Path::toString)
				.toList();

		assertTrue(feilmappeContents.containsAll(List.of(
				"01.07.2020_R100000000_0006-teknisk.zip",
				"01.07.2020_R100000000_0005-teknisk.zip"
		)));

		final List<String> fagpostmappeContents = fetchFileSecurely(sshdPath, FAGPOST_MAPPE, ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION).stream()
				.map(p -> p.getFileName().toString())
				.toList();

		assertTrue(fagpostmappeContents.containsAll(List.of(
				"01.07.2020_R100000000_0003.zip",
				"01.07.2020_R200000000_0004.zip"
		)));

		verify(exactly(56), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}

	@Test
	public void shouldMoveZipToFeilomraadeWhenEncryptedZip() throws IOException {
		setUpHappyStubs();
		setUpBadStubs();
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION_ENCRYPTED + ".zip");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			final List<String> feilmappeContents = fetchFileSecurely(sshdPath, FEILMAPPE, null).stream()
					.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
					.toList();
			assertTrue(feilmappeContents.contains(ZIP_FILE_NAME_NO_EXTENSION_ENCRYPTED + ".zip"));
		});
	}

	private void preparePath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		} else {
			FileUtils.cleanDirectory(path.toFile());
		}
	}

	private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
		Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
	}
}