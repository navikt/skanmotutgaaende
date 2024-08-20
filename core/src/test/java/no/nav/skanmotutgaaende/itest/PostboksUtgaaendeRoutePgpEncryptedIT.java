package no.nav.skanmotutgaaende.itest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

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
import static wiremock.org.apache.commons.io.FilenameUtils.getName;

public class PostboksUtgaaendeRoutePgpEncryptedIT extends AbstractIT {

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		final Path inngaaende = sshdPath.resolve(INNGAAENDE);
		final Path processed = inngaaende.resolve("processed");
		final Path feilmappe = sshdPath.resolve(FEILMAPPE);
		try {
			preparePath(inngaaende);
			preparePath(processed);
			preparePath(feilmappe);
		} catch (Exception e) {
			// noop
		}
	}

	private void preparePath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		} else {
			FileUtils.cleanDirectory(path.toFile());
		}
	}

	@Test
	public void shouldLesOgLagreHappy() throws IOException {
		// 01.07.2020_R123456784_1_4000.zip
		// OK   - 01.07.2020_R123456784_0001
		// OK   - 01.07.2020_R123456784_0002 (mangler filnavn og fysiskPostboks)
		// FEIL - 01.07.2020_R123456784_0003 (valideringsfeil, mangler journalpostid)
		// FEIL - 01.07.2020_R123456784_0004 (vil feile hos dokarkiv 400_Bad_Request)
		// FEIL - 01.07.2020_R123456784_0005 (mangler pdf)
		// FEIL - 01.07.2020_R123456784_0006 (mangler xml)
		setUpHappyStubs();
		setUpBadStubs();

		final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456784_1_4000";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

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

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> getName(p.toAbsolutePath().toString()))
				.toList();
		assertTrue(feilmappeContents.containsAll(List.of(
				"01.07.2020_R123456784_0005-teknisk.zip",
				"01.07.2020_R123456784_0006-teknisk.zip"
		)));

		final List<String> fagpostMappeContents = Files.list(sshdPath.resolve(FAGPOST_MAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> getName(p.toAbsolutePath().toString()))
				.toList();

		assertTrue(fagpostMappeContents.containsAll(List.of(
				"01.07.2020_R123456784_0003.zip",
				"01.07.2020_R123456784_0004.zip"
		)));

		verify(exactly(3), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}

// Skanmotutgaaende-pgp feilet funksjonelt for fil=01.07.2020_R300000000_0004, batch=01.07.2020_R3000000000. no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException: lagreFilDetaljer feilet funksjonelt med statusKode=400 BAD_REQUEST. Feilmelding=
// Skanmotutgaaende-pgp skrev feiletzip=feilmappe/01.07.2020_R3000000000/01.07.2020_R300000000_0004.zip til feilmappe. fil=01.07.2020_R300000000_0004, batch=01.07.2020_R3000000000.
// Skanmotutgaaende behandler fil=01.07.2020_R300000000_0010, batch=01.07.2020_R3000000000.

// Skanmotutgaaende-pgp feilet funksjonelt for fil=01.07.2020_R300000000_0003, batch=01.07.2020_R300000000_1_1000_ordered_xml_first_big. no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException: Kunne ikke unmarshalle xml: SAXParseException: cvc-elt.5.2.2.2.2: The value 'SKAN_NETS' of element 'mottakskanal' does not match the {value constraint} value 'SKAN_IM'.
// Skanmotutgaaende-pgp skrev feiletzip=feilmappe/01.07.2020_R300000000_1_1000_ordered_xml_first_big/01.07.2020_R300000000_0003.zip til feilmappe. fil=01.07.2020_R300000000_0003, batch=01.07.2020_R300000000_1_1000_ordered_xml_first_big.
// Skanmotutgaaende behandler fil=01.07.2020_R300000000_0017, batch=01.07.2020_R3000000000.

	@Test
	public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
		// 01.07.2020_R300000000_1_1000_ordered_xml_first_big.zip
		// OK   - 01.07.2020_R300000000_0001
		// OK   - 01.07.2020_R300000000_0002 (mangler filnavn og fysiskPostboks)
		// FEIL - 01.07.2020_R300000000_0003 (valideringsfeil, mangler journalpostid)
		// FEIL - 01.07.2020_R300000000_0005 (mangler pdf)
		// FEIL - 01.07.2020_R300000000_0006 (mangler xml)
		// OK   - 01.07.2020_R300000000_0007
		// ...
		// OK   - 01.07.2020_R300000000_0059
		setUpHappyStubs();
		setUpConflictStubs();

		final String ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION = "01.07.2020_R300000000_1_1000_ordered_xml_first_big";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION + ".zip.pgp");

		await().atMost(25, SECONDS).untilAsserted(() -> {
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

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
				.map(Path::getFileName)
				.map(Path::toString)
				.map(FilenameUtils::getName)
				.toList();

		assertTrue(feilmappeContents.containsAll(List.of(
				"01.07.2020_R300000000_0005-teknisk.zip",
				"01.07.2020_R300000000_0006-teknisk.zip"
		)));

		final List<String> fagpostMappeContents = Files.list(sshdPath.resolve(FAGPOST_MAPPE).resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
				.map(p -> getName(p.toAbsolutePath().toString()))
				.toList();

		assertTrue(fagpostMappeContents.containsAll(List.of(
				"01.07.2020_R300000000_0003.zip",
				"01.07.2020_R300000000_0004.zip"
		)));

		verify(exactly(56), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}

	@Test
	public void shouldFailWhenPrivateKeyDoesNotMatchPublicKey() throws IOException {
		// 01.07.2020_R123456780_1_1000.zip.pgp er kryptert med publicKeyElGamal (i stedet for publicKeyRSA)
		// Korresponderende RSA-private key vil da feile i forsøket på dekryptering

		final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456780_1_1000";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		assertTrue(Files.exists(sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));

		await().atMost(15, SECONDS).untilAsserted(() -> {
			assertTrue(Files.exists(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));
		});
	}

	private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
		Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
	}
}