package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wiremock.org.apache.commons.io.FileUtils;
import wiremock.org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.cloud.vault.token=123456")
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class PostboksUtgaaendeRouteEncryptedIT {

    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";
    private final String URL_DOKARKIV_JOURNALPOST_BAD_REQUEST = "/rest/intern/journalpostapi/v1/journalpost/4000004/mottaDokumentUtgaaendeSkanning";
    private final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456789_1_1000";
    private final String ZIP_FILENAME_NO_EXTENSION_BAD_PASSWORD = "29.10.2020_R123456789_6_9999";
    private final String ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION = "01.07.2020_R123456789_2_1000";
    private final String ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION = "01.07.2020_R100000000_1_1000_ordered_xml_first_big";
    private final String ZIP_FILE_NAME_NOT_ENCRYPTED_ENC = "01.07.2020_R123456789_1_1000_NotEncrypted";

    @Inject
    private Path sshdPath;

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        try {
            preparePath(inngaaende);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        } try {
            preparePath(processed);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        }
        try {
            preparePath(feilmappe);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        }
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
        File dir = sshdPath.toFile();
        for (File file : dir.listFiles()) {
            file.delete();
        }
        File dirr = new File(String.valueOf(sshdPath.toAbsolutePath()));
        dirr.delete();
    }

    @Test
    public void shouldLesOgLagreHappy() throws IOException {
        // 01.07.2020_R123456789_1_1000.zip
        // OK   - 01.07.2020_R123456789_0001
        // OK   - 01.07.2020_R123456789_0002 (mangler filnavn og fysiskPostboks)
        // FEIL - 01.07.2020_R123456789_0003 (valideringsfeil, mangler journalpostid)
        // FEIL - 01.07.2020_R123456789_0004 (vil feile hos dokarkiv 400_Bad_Request)
        // FEIL - 01.07.2020_R123456789_0005 (mangler pdf)
        // FEIL - 01.07.2020_R123456789_0006 (mangler xml)
        setUpHappyStubs();
        setUpBadStubs();
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
                        .resolve(ZIP_FILE_NAME_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(4);
            } catch (NoSuchFileException e) {
                fail();
            }
        });

        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertTrue(feilmappeContents.containsAll(List.of(
                "01.07.2020_R123456789_0003.enc.zip",
                "01.07.2020_R123456789_0004.enc.zip",
                "01.07.2020_R123456789_0005.enc.zip",
                "01.07.2020_R123456789_0006.enc.zip"
        )));
        verify(exactly(3), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
        // 01.07.2020_R100000000_1_1000_ordered_xml_first_big.zip
        // OK   - 01.07.2020_R100000000_0001
        // OK   - 01.07.2020_R100000000_0002 (mangler filnavn og fysiskPostboks)
        // FEIL - 01.07.2020_R100000000_0003 (valideringsfeil, mangler journalpostid)
        // FEIL - 01.07.2020_R100000000_0004 (vil feile hos dokarkiv 400_Bad_Request)
        // FEIL - 01.07.2020_R100000000_0005 (mangler pdf)
        // FEIL - 01.07.2020_R100000000_0006 (mangler xml)
        // OK   - 01.07.2020_R100000000_0007
        // ...
        // OK   - 01.07.2020_R100000000_0059
        setUpHappyStubs();
        setUpBadStubs();
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION + ".enc.zip");

        await().atMost(25, SECONDS).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
                        .resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(4);
            } catch (NoSuchFileException e) {
                fail();
            }
        });

        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertTrue(feilmappeContents.containsAll(List.of(
                "01.07.2020_R100000000_0003.enc.zip",
                "01.07.2020_R100000000_0004.enc.zip",
                "01.07.2020_R100000000_0005.enc.zip",
                "01.07.2020_R100000000_0006.enc.zip"
        )));
        verify(exactly(56), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldMoveZipToFeilomraadeWhenBadPassword() throws IOException {

        //ZipException: Bad password
        //should be sent to feilmappe

        copyFileFromClasspathToInngaaende(ZIP_FILENAME_NO_EXTENSION_BAD_PASSWORD + ".enc.zip");

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                        .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                        .collect(Collectors.toList());
                assertTrue(feilmappeContents.contains(ZIP_FILENAME_NO_EXTENSION_BAD_PASSWORD + ".enc.zip"));
            } catch (NoSuchFileException e) {
                fail();
            }
        });
    }

    @Test
    public void shouldMoveZipToFeilomraadeWhenNotEncryptedEncFile() throws IOException {

        //ZipException: En .enc-file kom inn men filene er ukrypterte
        //should be sent to feilmappe

        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NOT_ENCRYPTED_ENC + ".enc.zip");

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                        .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                        .collect(Collectors.toList());
                assertTrue(feilmappeContents.contains(ZIP_FILE_NAME_NOT_ENCRYPTED_ENC + ".enc.zip"));
            } catch (NoSuchFileException e) {
                fail();
            }
        });
    }

    @Test
    public void shouldMoveZipToFeilomraadeWhenBadEncryption() throws IOException {

        //ZipException: Filene er ikke kryptert med AES men en annen krypteringsmetode
        //should be sent to feilmappe

        copyFileFromClasspathToInngaaende(ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION + ".enc.zip");

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                        .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                        .collect(Collectors.toList());
                assertTrue(feilmappeContents.contains(ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION + ".enc.zip"));
            } catch (NoSuchFileException e) {
                fail();
            }
        });
    }

    private void setUpHappyStubs() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
    }

    private void setUpBadStubs() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_BAD_REQUEST))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
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