package no.nav.skanmotutgaaende.leszipfil;

import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.sftp.Sftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class LesZipfilConsumer {

    private final String INBOUND_DIRECTORY = "";

    private Sftp sftp;

    @Autowired
    LesZipfilConsumer(Sftp sftp){
        this.sftp = sftp;
    }

    public File hentZipfil() {
        // TODO: Hent zipfil bestående av par av pdf'er og xml'er med metadata fra skyfilområde
        return new File("core/src/main/resources/tmp/__files/SKAN_NETS.zip");
    }

    public List<String> listZipFiles() throws Exception {
        try{
            sftp.connect();
            sftp.changeDirectory(INBOUND_DIRECTORY);
            log.info(sftp.getHomePath());
            List<String> files = sftp.listFiles("*.zip");
            sftp.disconnect();
            return files;
        } catch(Exception e) {
            throw e;
        }
    }

    public byte[] getFile(String filename) throws SftpException, IOException {
        sftp.connect();
        InputStream fileStream = sftp.getFile(INBOUND_DIRECTORY + "/" + filename);
        byte[] file = fileStream.readAllBytes();
        sftp.disconnect();
        return file;
    }

}
