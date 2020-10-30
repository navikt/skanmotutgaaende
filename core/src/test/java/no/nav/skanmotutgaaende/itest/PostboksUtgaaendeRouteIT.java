package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
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
import static java.lang.Thread.sleep;
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
public class PostboksUtgaaendeRouteIT {

    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";
    private final String URL_DOKARKIV_JOURNALPOST_BAD_REQUEST = "/rest/intern/journalpostapi/v1/journalpost/4000004/mottaDokumentUtgaaendeSkanning";
    private final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456789_1_1000";
    private final String ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION = "01.07.2020_R100000000_1_1000_ordered_xml_first_big";

    @Inject
    private Path sshdPath;

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        preparePath(inngaaende);
        preparePath(processed);
        try{
            preparePath(feilmappe);
        }
        catch(Exception e){
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        }
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
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
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip");

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
                "01.07.2020_R123456789_0003.zip",
                "01.07.2020_R123456789_0004.zip",
                "01.07.2020_R123456789_0005.zip",
                "01.07.2020_R123456789_0006.zip"
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
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION + ".zip");

        await().atMost(15, SECONDS).untilAsserted(() -> {
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
                "01.07.2020_R100000000_0003.zip",
                "01.07.2020_R100000000_0004.zip",
                "01.07.2020_R100000000_0005.zip",
                "01.07.2020_R100000000_0006.zip"
        )));
        verify(exactly(56), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
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