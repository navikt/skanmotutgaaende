package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.skanmotutgaaende.config.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.filomraade.FilomraadeConsumer;
import no.nav.skanmotutgaaende.filomraade.FilomraadeService;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.LesFraFilomraadeOgLagreFildetaljer;
import no.nav.skanmotutgaaende.sftp.Sftp;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgLagreFildetaljerIT {

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/\\d+/mottaDokumentUtgaaendeSkanning";
    private final String URL_DOKARKIV_JOURNALPOST_001 = "/rest/intern/journalpostapi/v1/journalpost/001/mottaDokumentUtgaaendeSkanning";
    private final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";
    private final Path SKANMOTUTGAAENDE_PATH = Path.of("src/test/resources/inbound/SKANMOTUTGAAENDE");
    private final Path SKANMOTUTGAAENDE_FEIL_PATH = Path.of("src/test/resources/inbound/SKANMOTUTGAAENDE_FEIL");
    private final String HAPPY_ZIP_PATH = "__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip";

    LesFraFilomraadeOgLagreFildetaljer lesFraFilomraadeOgLagreFildetaljer;
    FilomraadeService filomraadeService;
    LagreFildetaljerService lagreFildetaljerService;

    private int PORT = 2222;
    private SshServer sshd = SshServer.setUpDefaultServer();
    private Sftp sftp;

    @Autowired
    SkanmotutgaaendeProperties skanmotutgaaendeProperties;

    @BeforeAll
    void startSftpServer() throws IOException {
        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get(VALID_PUBLIC_KEY_PATH)));
        sshd.start();
    }

    @AfterAll
    void shutdownSftpServer() throws IOException {
        sshd.stop();
        sshd.close();
    }

    @BeforeEach
    void setUpServices() throws IOException {
        sftp = new Sftp(skanmotutgaaendeProperties);
        filomraadeService = Mockito.spy(new FilomraadeService(new FilomraadeConsumer(sftp, skanmotutgaaendeProperties)));
        lagreFildetaljerService = new LagreFildetaljerService(new LagreFildetaljerConsumer(new RestTemplateBuilder(), skanmotutgaaendeProperties));
        lesFraFilomraadeOgLagreFildetaljer = new LesFraFilomraadeOgLagreFildetaljer(filomraadeService, lagreFildetaljerService);
        setUpStubs();
        cleanFolder(SKANMOTUTGAAENDE_PATH);
        cleanFolder(SKANMOTUTGAAENDE_FEIL_PATH);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
    }

    @Test
    public void shouldLesOgLagreHappy() throws IOException {
        copyFileToSkanmotutgaaendeFolder(HAPPY_ZIP_PATH);

        assertDoesNotThrow(() -> lesFraFilomraadeOgLagreFildetaljer.lesOgLagreZipfiler());
        Mockito.verify(filomraadeService, Mockito.times(2)).uploadFileToFeilomrade(any(), any(), any());
        verify(exactly(3), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldMoveZipAfterRead() throws IOException {
        copyFileToSkanmotutgaaendeFolder(HAPPY_ZIP_PATH);
        File movedFile = new File(Path.of(SKANMOTUTGAAENDE_PATH.toString(), "/processed/xml_pdf_pairs_testdata.zip.processed").toString());

        assertFalse(movedFile.exists());
        lesFraFilomraadeOgLagreFildetaljer.lesOgLagreZipfiler();
        Mockito.verify(filomraadeService, Mockito.times(2)).uploadFileToFeilomrade(any(), any(), any());
        assertTrue(movedFile.exists());
    }

    @Test
    public void shouldAddFileToFeilOmraadeWhenFailing() throws IOException {
        copyFileToSkanmotutgaaendeFolder(HAPPY_ZIP_PATH);
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_001))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

        lesFraFilomraadeOgLagreFildetaljer.lesOgLagreZipfiler();

        Mockito.verify(filomraadeService, Mockito.times(4)).uploadFileToFeilomrade(any(), any(), eq("xml_pdf_pairs_testdata"));

        sftp.disconnect();
    }

    private void cleanFolder(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    if (path.toFile().list().length == 0) {
                        Files.delete(path);
                    } else {
                        cleanFolder(path);
                    }
                }
                else if (!path.getFileName().toString().equals("dummy")) {
                    Files.delete(path);
                }
            }
        }
    }

    private Path copyFileToSkanmotutgaaendeFolder(String relativePath) throws IOException {
        Path source = getPathFromRelativePath(relativePath);
        Path dest = Path.of(SKANMOTUTGAAENDE_PATH.toAbsolutePath() + "/" + source.getFileName());
        return Files.copy(source, dest);
    }

    private Path getPathFromRelativePath(String relativePath) throws IOException {
        Resource onClasspath = new ClassPathResource(relativePath);
        return Paths.get(onClasspath.getFile().getAbsolutePath());
    }
}