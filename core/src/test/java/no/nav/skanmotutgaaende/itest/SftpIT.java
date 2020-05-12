package no.nav.skanmotutgaaende.itest;

import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeSftpFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.sftp.Sftp;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = TestConfig.class)
public class SftpIT {

    private static final String FILES_FOLDER_PATH = "src/test/resources/__files";
    private static final String PAIR_FOLDER_PATH = FILES_FOLDER_PATH + "/xml_pdf_pairs";
    private static final String FEILOMRADE_FOLDER_PATH = "src/test/resources/inbound/SKANMOTUTGAAENDE_FEIL";
    private static final String ZIP_FILE_PATH = PAIR_FOLDER_PATH + "/xml_pdf_pairs_testdata.zip";
    private static final String XML_FILE_PATH = FILES_FOLDER_PATH + "/data_005.xml";
    private static final String DIR_ONE_FOLDER_PATH = "src/test/resources/sftp/dirOne";
    private static final String DIR_TWO_FOLDER_PATH = "src/test/resources/sftp/dirTwo";
    private static final String INVALID_FOLDER_PATH = "foo/bar/baz";
    private static final String INVALID_FILE_NAME = "invalidFilename.zip";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";
    private static final String TMP_FILE_NAME = "tmpfile.txt";

    //TODO: Gjør det mulig å bruke en random port
    private int PORT = 2222;

    @Autowired
    private SkanmotutgaaendeProperties skanmotutgaaendeProperties;

    private SshServer sshd = SshServer.setUpDefaultServer();
    private Sftp sftp;

    @BeforeAll
    void startSftpServer() throws IOException {
        skanmotutgaaendeProperties.getSftp().setPort(Integer.toString(PORT));
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
    void setUp() throws IOException {
        sftp = new Sftp(skanmotutgaaendeProperties);
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

            sftp.changeDirectory(homePath + PAIR_FOLDER_PATH);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(homePath + PAIR_FOLDER_PATH));
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
            Assert.assertEquals("Klarte ikke endre mappe, path: " + INVALID_FOLDER_PATH, e.getMessage());
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
            sftp.changeDirectory(PAIR_FOLDER_PATH);

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

            sftp.changeDirectory(FILES_FOLDER_PATH);
            sftp.getFile(INVALID_FILE_NAME);

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke laste ned " + INVALID_FILE_NAME, e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldDeleteFile() {
        File f = new File(FILES_FOLDER_PATH + "/" + TMP_FILE_NAME);
        try {
            sftp.connect();
            int initialNumberOfFiles = sftp.listFiles(FILES_FOLDER_PATH).size();

            f.createNewFile();
            Assert.assertEquals(initialNumberOfFiles + 1, sftp.listFiles(FILES_FOLDER_PATH).size());
            sftp.deleteFile(FILES_FOLDER_PATH, TMP_FILE_NAME);

            Assert.assertEquals(initialNumberOfFiles, sftp.listFiles(FILES_FOLDER_PATH).size());

            sftp.disconnect();
        } catch (Exception e) {
            f.delete();
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingFile() {
        try {
            sftp.connect();

            sftp.deleteFile(FILES_FOLDER_PATH, INVALID_FILE_NAME);

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette " + FILES_FOLDER_PATH + "/" + INVALID_FILE_NAME, e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingPath() {
        try {
            sftp.connect();

            sftp.deleteFile(INVALID_FOLDER_PATH, INVALID_FILE_NAME);

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette " + INVALID_FOLDER_PATH + "/" + INVALID_FILE_NAME, e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFile() {
        try {
            cleanFolder(Path.of(FEILOMRADE_FOLDER_PATH));
            File xmlFile = Paths.get(XML_FILE_PATH).toFile();
            String filename = "uploadedFile.xml";

            sftp.connect();

            sftp.uploadFile(new FileInputStream(xmlFile), FEILOMRADE_FOLDER_PATH, filename);

            Assert.assertTrue(sftp.listFiles(FEILOMRADE_FOLDER_PATH).contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFileToNewDirectory() {
        try {
            cleanFolder(Path.of(FEILOMRADE_FOLDER_PATH));
            File xmlFile = Paths.get(XML_FILE_PATH).toFile();
            String filename = "uploadedFile.xml";

            sftp.connect();

            sftp.uploadFile(new FileInputStream(xmlFile), FEILOMRADE_FOLDER_PATH + "/newDirectory", filename);

            Assert.assertTrue(sftp.listFiles(FEILOMRADE_FOLDER_PATH + "/newDirectory/").contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private void cleanFolder(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    Files.delete(path);
                } else {
                    FileUtils.deleteDirectory(path.toFile());
                }
            }
        }
    }
}
