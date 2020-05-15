package no.nav.skanmotutgaaende.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FilomraadeService {

    private FilomraadeConsumer filomraadeConsumer;

    @Inject
    public FilomraadeService(FilomraadeConsumer filomraadeConsumer) {
        this.filomraadeConsumer = filomraadeConsumer;
    }

    public Map<String, byte[]> getZipFiles() throws SkanmotutgaaendeSftpTechnicalException {
        try {
            filomraadeConsumer.connectToSftp();
            List<String> fileNames = filomraadeConsumer.listZipFiles();
            Map<String, byte[]> files = new HashMap<>();

            for (String filename : fileNames) {
                byte[] zipFile = getZipFile(filename);
                if (zipFile != null) {
                    files.put(filename, zipFile);
                }
            }
            log.info("Skanmotutgaaende leser {} fra sftp", fileNames.toString());
            return files;
        } catch (LesZipFilFuntionalException e) {
            log.warn("Skanmotutgaaende klarte ikke hente zipfiler", e);
            throw e;
        } catch (SkanmotutgaaendeSftpTechnicalException e) {
            log.warn("Skanmotutgaaende klarte ikke koble til sftp", e);
            throw e;
        } finally {
            filomraadeConsumer.disconnectFromSftp();
        }
    }

    public void uploadFileToFeilomrade(byte[] file, String filename, String path) {
        try {
            filomraadeConsumer.connectToSftp();
            filomraadeConsumer.uploadFileToFeilomrade(new ByteArrayInputStream(file), filename, path);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke laste opp fil {}", filename, e);
        } finally {
            filomraadeConsumer.disconnectFromSftp();
        }
    }

    public void deleteZipFiles(List<String> zipFiles) {
        filomraadeConsumer.connectToSftp();
        zipFiles.stream().forEach(this::deleteZipFile);
        filomraadeConsumer.disconnectFromSftp();
    }

    public void moveZipFiles(List<String> files, String destination) {
        filomraadeConsumer.connectToSftp();
        files.stream().forEach(file -> {
            moveFile(file, destination, file + ".processed");
        });
        filomraadeConsumer.disconnectFromSftp();
    }

    private void deleteZipFile(String filename) {
        try {
            filomraadeConsumer.deleteFile(filename);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke slette fil {}", filename, e);
        }
    }

    private void moveFile(String from, String to, String newFilename) {
        try {
            filomraadeConsumer.moveFile(from, to, newFilename);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke flytte fil {} til {}/{}", from, to, newFilename, e);
        }
    }

    private byte[] getZipFile(String fileName) {
        try {
            return filomraadeConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke hente filen {}", fileName, e);
            return null;
        }
    }
}
