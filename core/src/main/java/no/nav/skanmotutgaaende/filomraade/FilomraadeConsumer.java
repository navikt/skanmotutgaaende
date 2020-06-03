package no.nav.skanmotutgaaende.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotutgaaende.sftp.Sftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class FilomraadeConsumer {

    private final String FEILOMRADE_DIRTY_FOLDER_EXTENSION = ".tmp";

    private Sftp sftp;
    private String inboundDirectory;
    private String feilDirectory;

    @Autowired
    public FilomraadeConsumer(Sftp sftp, SkanmotutgaaendeProperties skanmotutgaaendeProperties) {
        this.sftp = sftp;
        inboundDirectory = skanmotutgaaendeProperties.getFilomraade().getInngaaendemappe();
        feilDirectory = skanmotutgaaendeProperties.getFilomraade().getFeilmappe();
    }

    public List<String> listZipFiles() {
        try {
            log.info("Skanmotutgaaende henter zipfiler fra {}", sftp.getHomePath() + inboundDirectory);
            List<String> files = sftp.listFiles(inboundDirectory + "/*.zip");
            return files;
        } catch (Exception e) {
            throw new LesZipFilFuntionalException("Skanmotutgaaende klarte ikke hente zipfiler", e);
        }
    }

    public byte[] getFile(String filename) throws IOException {
        InputStream fileStream = sftp.getFile(inboundDirectory + "/" + filename);
        byte[] file = fileStream.readAllBytes();
        return file;
    }

    public void deleteFile(String filename) {
        log.info("Skanmotutgaaende sletter fil {}", filename);
        sftp.deleteFile(inboundDirectory, filename);
    }

    public void uploadFileToFeilomrade(InputStream file, String filename, String path) {
        log.info("Skanmotutgaaende laster opp fil {} til feilområde", filename);
        sftp.uploadFile(file, feilDirectory + "/" + path + FEILOMRADE_DIRTY_FOLDER_EXTENSION, filename);
    }

    public void cleanDirtyFeilomrade(String folderName) {
        String oldPath = feilDirectory + "/" + folderName + FEILOMRADE_DIRTY_FOLDER_EXTENSION;
        String newPath = feilDirectory + "/" + folderName;
        log.info("Skanmotutgaaende bytter navn på midlertidig feilmappe {} til {}", oldPath, newPath);
        sftp.rename(oldPath, newPath);
    }

    public void moveFile(String from, String to, String newFilename) {
        String fromPath = inboundDirectory + "/" + from;
        String toPath = inboundDirectory + "/" + to;
        log.info("Skanmotutgaende flytter fil {} til {}", fromPath, toPath);
        sftp.moveFile(fromPath, toPath, newFilename);
    }

    public void disconnectFromSftp() {
        sftp.disconnect();
    }
}
