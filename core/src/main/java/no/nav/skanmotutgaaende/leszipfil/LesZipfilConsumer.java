package no.nav.skanmotutgaaende.leszipfil;

import com.jcraft.jsch.SftpException;
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
public class LesZipfilConsumer {

    private Sftp sftp;
    private String inboundDirectory;

    @Autowired
    public LesZipfilConsumer(Sftp sftp, SkanmotutgaaendeProperties skanmotutgaaendeProperties) {
        this.sftp = sftp;
        inboundDirectory = skanmotutgaaendeProperties.getFilomraade().getInngaaendemappe();
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

    public byte[] getFile(String filename) throws SftpException, IOException {
        InputStream fileStream = sftp.getFile(inboundDirectory + "/" + filename);
        byte[] file = fileStream.readAllBytes();
        return file;
    }

    public void connectToSftp() {
        sftp.connect();
    }

    public void disconnectFromSftp() {
        sftp.disconnect();
    }
}
