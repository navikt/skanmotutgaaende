package no.nav.skanmotutgaaende.itest;

import no.nav.skanmotutgaaende.config.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import no.nav.skanmotutgaaende.itest.config.TestConfig;
import no.nav.skanmotutgaaende.sftp.Sftp;
import org.apache.sshd.server.SshServer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = TestConfig.class)
public class SftpIT {

    private static final String INNGAAENDE = "inngaaende";

    private static final String ZIP_FILE_NAME = "01.07.2020_R123456789_1_1000.zip";
    private static final String ZIP_FILE_PATH = "__files/" + ZIP_FILE_NAME;
    private static final String XML_FILE_PATH = "__files/data_002.xml";
    private static final String DIR_ONE = "dirOne";
    private static final String DIR_TWO = "dirTwo";

    private Sftp sftp;

    @Autowired
    private SkanmotutgaaendeProperties skanmotutgaaendeProperties;

    @Inject
    private Path sshdPath;

    @Inject
    private SshServer sshd;

    @BeforeAll
    void beforeAll() {
        sftp = new Sftp(skanmotutgaaendeProperties);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path dir1 = sshdPath.resolve(DIR_ONE);
        final Path dir2 = sshdPath.resolve(DIR_TWO);
        preparePath(inngaaende);
        preparePath(dir1);
        preparePath(dir2);

        moveFilesToDirectory();
    }

    @Test
    public void shouldConnectToSftp() {
        try {
            sftp.connectIfNotConnected();

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
            sftp.changeDirectory(sftp.getHomePath() + DIR_ONE);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_ONE));
            Assert.assertTrue(sftp.listFiles().contains("fileOne"));

            sftp.changeDirectory(sftp.getHomePath() + DIR_TWO);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_TWO));
            Assert.assertTrue(sftp.listFiles().contains("fileTwo"));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldFailToChangeDirectoryToInvalidPath() {
        try {
            sftp.changeDirectory("ikke/en/gyldig/path");
            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke endre mappe, path: ikke/en/gyldig/path", e.getMessage());
        } catch (Exception e) {
            sftp.disconnect();
            Assert.fail();
        }
    }

    @Test
    void shouldGetFile() {
        try {
            sftp.changeDirectory(INNGAAENDE);

            InputStream inputStream = sftp.getFile(ZIP_FILE_NAME);
            Assert.assertArrayEquals(new ClassPathResource(ZIP_FILE_PATH).getInputStream().readAllBytes(), inputStream.readAllBytes());

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToGetFileWhenFileNameIsInvalid() {
        try {
            sftp.changeDirectory(INNGAAENDE);
            sftp.getFile("nonExistingFile.zip");

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke laste ned nonExistingFile.zip", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldDeleteFile() {
        File f = new File(sshdPath.resolve(INNGAAENDE).resolve("tmpfil.txt").toUri());
        try {
            int initialNumberOfFiles = sftp.listFiles(INNGAAENDE).size();

            f.createNewFile();
            Assert.assertEquals(initialNumberOfFiles + 1, sftp.listFiles(INNGAAENDE).size());
            sftp.deleteFile(INNGAAENDE, "tmpfil.txt");

            Assert.assertEquals(initialNumberOfFiles, sftp.listFiles(INNGAAENDE).size());

            sftp.disconnect();
        } catch (Exception e) {
            f.delete();
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingFile() {
        try {
            sftp.deleteFile(INNGAAENDE, "nonExistingFile.txt");

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette " + INNGAAENDE + "/nonExistingFile.txt", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingPath() {
        try {
            sftp.deleteFile("ikke/en/gyldig/path", "nonExistingFile.txt");

            Assert.fail();
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette ikke/en/gyldig/path/nonExistingFile.txt", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFile() {
        try {
            InputStream zipFile = new ClassPathResource(ZIP_FILE_PATH).getInputStream();
            String filename = "uploadedFile.xml";

            sftp.uploadFile(zipFile, INNGAAENDE, filename);

            Assert.assertTrue(sftp.listFiles(INNGAAENDE).contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFileToNewDirectory() {
        try {
            InputStream xmlFile = new ClassPathResource(XML_FILE_PATH).getInputStream();
            String filename = "uploadedFile.xml";

            sftp.uploadFile(xmlFile, INNGAAENDE + "/newDirectory", filename);

            Assert.assertTrue(sftp.listFiles(INNGAAENDE + "/newDirectory/").contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldMoveFile() {
        try {
            File f = new File(sshdPath.resolve(DIR_ONE).resolve("tmpFile.txt").toString());
            f.createNewFile();

            Assert.assertFalse(sftp.listFiles(DIR_TWO).contains("movedFile.txt"));

            sftp.moveFile(DIR_ONE + "/tmpFile.txt", DIR_TWO, "movedFile.txt");

            Assert.assertTrue(sftp.listFiles(DIR_TWO).contains("movedFile.txt"));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    private void moveFilesToDirectory() throws IOException {
        Files.copy(new ClassPathResource("sftp/" + DIR_ONE + "/fileOne").getInputStream(), sshdPath.resolve(DIR_ONE).resolve("fileOne"));
        Files.copy(new ClassPathResource("sftp/" + DIR_TWO + "/fileTwo").getInputStream(), sshdPath.resolve(DIR_TWO).resolve("fileTwo"));
        Files.copy(new ClassPathResource(ZIP_FILE_PATH).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME));
    }
}
