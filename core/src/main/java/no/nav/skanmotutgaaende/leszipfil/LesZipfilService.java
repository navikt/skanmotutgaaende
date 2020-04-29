package no.nav.skanmotutgaaende.leszipfil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LesZipfilService {

    private LesZipfilConsumer lesZipfilConsumer;

    @Inject
    public LesZipfilService(LesZipfilConsumer lesZipfilConsumer) {
        this.lesZipfilConsumer = lesZipfilConsumer;
    }

    public List<byte[]> getZipFiles() throws Exception {
        try{
            List<String> fileNames = lesZipfilConsumer.listZipFiles();
            List<byte[]> files = fileNames.stream().map(this::getZipFile).filter(Objects::nonNull).collect(Collectors.toList());
            log.info("Read " + fileNames.toString() + " from sftp");
            return files;
        } catch (Exception e) {
            log.info("failed to connect to sftp");
            throw e;
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

    public File lesZipfil() {
        return lesZipfilConsumer.hentZipfil();
    }
}
