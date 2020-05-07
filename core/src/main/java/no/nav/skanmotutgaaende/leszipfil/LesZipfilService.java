package no.nav.skanmotutgaaende.leszipfil;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LesZipfilService {

    private LesZipfilConsumer lesZipfilConsumer;

    @Inject
    public LesZipfilService(LesZipfilConsumer lesZipfilConsumer) {
        this.lesZipfilConsumer = lesZipfilConsumer;
    }

    public Map<String, byte[]> getZipFiles() throws SkanmotutgaaendeSftpTechnicalException {
        try {
            lesZipfilConsumer.connectToSftp();
            List<String> fileNames = lesZipfilConsumer.listZipFiles();
            Map<String, byte[]> files = new HashMap<>();

            for (String filename : fileNames) {
                byte[] zipFile = getZipFile(filename);
                if (null != zipFile) {
                    files.put(filename, zipFile);
                }
            }
            log.info("Skanmotutgaaende leser {} fra sftp", fileNames.toString());
            return files;
        } catch (LesZipFilFuntionalException e) {
            log.warn("Skanmotutgaaende klarte ikke hente zipfiler");
            throw e;
        } catch (Exception e) {
            log.warn("Skanmotutgaaende klarte ikke koble til sftp");
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke koble til sftp", e);
        } finally {
            lesZipfilConsumer.disconnectFromSftp();
        }
    }

    public void deleteZipFile(String filename) {
        try {
            lesZipfilConsumer.deleteFile(filename);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke slette fil {}", filename, e);
        }
    }

    private byte[] getZipFile(String fileName) {
        try {
            return lesZipfilConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke hente filen {}", fileName, e);
            return null;
        }
    }
}
