package no.nav.skanmotutgaaende.leszipfil;

import lombok.extern.slf4j.Slf4j;
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
            List<String> fileNames = lesZipfilConsumer.listZipFiles();
            Map<String, byte[]> files = new HashMap<>();

            for (String filename : fileNames) {
                byte[] zipFile = getZipFile(filename);
                if (null != zipFile) {
                    files.put(filename, zipFile);
                }
            }
            log.info("Read " + fileNames.toString() + " from sftp");
            return files;
        } catch (Exception e) {
            log.warn("failed to connect to sftp");
            throw new SkanmotutgaaendeSftpTechnicalException("Failed to connect to sftp", e);
        }
    }

    private byte[] getZipFile(String fileName) {
        try {
            return lesZipfilConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Failed to get file " + fileName, e);
            return null;
        }
    }
}
