package no.nav.skanmotutgaaende.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.lesoglagre.LesFraFilomraadeOgLagreFildetaljer;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilConsumer;
import no.nav.skanmotutgaaende.leszipfil.LesZipfilService;
import no.nav.skanmotutgaaende.sftp.Sftp;
import no.nav.skanmotutgaaende.sftp.SftpConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class, SftpConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgLagreFildetaljerIT {

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/intern/journalpostapi/v1/journalpost/.+/mottaDokumentUtgaaendeSkanning";
    private final String URL_DOKARKIV_JOURNALPOST_003 = "/rest/intern/journalpostapi/v1/journalpost/003/mottaDokumentUtgaaendeSkanning";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";

    LesFraFilomraadeOgLagreFildetaljer lesFraFilomraadeOgLagreFildetaljer;
    LesZipfilService lesZipfilService;
    LagreFildetaljerService lagreFildetaljerService;

    private int PORT = 2222;
    private SshServer sshd = SshServer.setUpDefaultServer();

    @Autowired
    SkanmotutgaaendeProperties skanmotutgaaendeProperties;
    @Autowired
    Sftp sftp;

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
    void setUpServices() {
        lesZipfilService = new LesZipfilService(new LesZipfilConsumer(sftp, skanmotutgaaendeProperties));
        lagreFildetaljerService = new LagreFildetaljerService(new LagreFildetaljerConsumer(new RestTemplateBuilder(), skanmotutgaaendeProperties));
        lesFraFilomraadeOgLagreFildetaljer = new LesFraFilomraadeOgLagreFildetaljer(lesZipfilService, lagreFildetaljerService);
        setUpStubs();
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
    public void shouldConnect() {
        lesFraFilomraadeOgLagreFildetaljer.tryToConnect();
    }

    @Test
    public void shouldLesOgLagreHappy() {
        assertDoesNotThrow(() -> lesFraFilomraadeOgLagreFildetaljer.lesOgLagre());
        verify(exactly(10), putRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldContinueIfFailingToLagreFildetaljer() {
        stubFor(put(urlMatching(URL_DOKARKIV_JOURNALPOST_003))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
        List<List<LagreFildetaljerResponse>> responses = lesFraFilomraadeOgLagreFildetaljer.lesOgLagre();
        assertEquals(9, responses.get(0).size());
    }
}
