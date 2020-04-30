package no.nav.skanmotutgaaende.sftp;

import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {TestConfig.class, SftpConfig.class})
public class SftpITest {

    private static final String RESOURCE_FOLDER_PATH = "src/test/resources/__files/xml_pdf_pairs";
    private static final String ZIP_FILE_PATH = RESOURCE_FOLDER_PATH + "/xml_pdf_pairs_testdata.zip";
    private static final String DIR_ONE_FOLDER_PATH = "src/test/resources/sftp/dirOne";
    private static final String DIR_TWO_FOLDER_PATH = "src/test/resources/sftp/dirTwo";
    private static final String INVALID_FOLDER_PATH = "foo/bar/baz";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";

    private int PORT = 2222;

    private SshServer sshd = SshServer.setUpDefaultServer();
    @Autowired
    private Sftp sftp;

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

    @Test
    public void shouldConnectToSftp() {
        try {
            sftp.connect();

            Assert.assertTrue(sftp.isConnected());
            Assert.assertEquals(1, sshd.getActiveSessions().size());
            Assert.assertEquals("itestUser", sshd.getActiveSessions().iterator().next().getUsername());
            sftp.disconnect();
            Assert.assertFalse(sftp.isConnected());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldChangeDirectoryAndListFiles() {
        try {

            sftp.connect();
            String homePath = sftp.getHomePath() + "/";

            sftp.changeDirectory(homePath + DIR_ONE_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + DIR_ONE_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().contains("fileOne"));

            sftp.changeDirectory(homePath + DIR_TWO_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + DIR_TWO_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().contains("fileTwo"));

            sftp.changeDirectory(homePath + RESOURCE_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + RESOURCE_FOLDER_PATH));
            Assert.assertTrue(sftp.listFiles().containsAll(
                    List.of(
                            "xml_pdf_pairs_invalid_testdata.zip",
                            "xml_pdf_pairs_testdata.zip"
                    )
            ));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldFailToChangeDirectoryToInvalidPath() {
        try {
            sftp.connect();

            sftp.changeDirectory(INVALID_FOLDER_PATH);
            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Failed to change directory, path: foo/bar/baz", e.getMessage());
        } catch (Exception e) {
            sftp.disconnect();
            Assert.fail();
        }
    }

    @Test
    void shouldGetFile() {
        try {
            File zipFile = Paths.get(ZIP_FILE_PATH).toFile();

            sftp.connect();
            sftp.changeDirectory(RESOURCE_FOLDER_PATH);

            InputStream inputStream = sftp.getFile("xml_pdf_pairs_testdata.zip");
            Assert.assertArrayEquals(inputStream.readAllBytes(), new FileInputStream(zipFile).readAllBytes());

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToGetFileWhenFileNameIsInvalid() {
        try {
            sftp.connect();

            sftp.changeDirectory(RESOURCE_FOLDER_PATH);
            sftp.getFile("invalidFileName.zip");

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Failed to download invalidFileName.zip", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
