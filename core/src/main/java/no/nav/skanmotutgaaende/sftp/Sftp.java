package no.nav.skanmotutgaaende.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeSftpFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeSftpTechnicalException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Sftp {

    private final String APPLICATION = "skanmotutgaaende";

    private Session jschSession;
    private ChannelSftp channelSftp;
    private String homePath;

    private String host;
    private String username;
    private String port;
    private String privateKey;
    private String hostKey;

    public Sftp(SkanmotutgaaendeProperties properties) {
        this.host = properties.getSftp().getHost();
        this.username = properties.getSftp().getUsername();
        this.port = properties.getSftp().getPort();
        this.privateKey = properties.getSftp().getPrivateKey();
        this.hostKey = properties.getSftp().getHostKey();
    }

    public List<String> listFiles() {
        return listFiles("*");
    }

    public List<String> listFiles(String path) {
        checkSftpConnection();
        try {
            Vector<LsEntry> vector = channelSftp.ls(path);
            return vector.stream().map(ChannelSftp.LsEntry::getFilename).collect(Collectors.toList());
        } catch (SftpException e) {
            log.error("{} klarte ikke liste filene, path: {}", APPLICATION, path, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke liste filene, path: " + path, e);
        }
    }

    public String presentWorkingDirectory() {
        checkSftpConnection();
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            log.error("{} klarte ikke hente arbeidsmappe", APPLICATION, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke hente arbeidsmappe", e);
        }
    }

    public void changeDirectory(String path) {
        checkSftpConnection();
        try {
            channelSftp.cd(path);
        } catch (SftpException e) {
            log.error("{} klarte ikke å endre mappe, path: {}", APPLICATION, path, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke endre mappe, path: " + path, e);
        }
    }

    public InputStream getFile(String filename) {
        checkSftpConnection();
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            log.error("{} klarte ikke laste ned {} ", APPLICATION, filename, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke laste ned " + filename, e);
        }
    }

    public void deleteFile(String directory, String filename) {
        checkSftpConnection();
        try {
            if (!listFiles(directory).contains(filename)) {
                throw new SkanmotutgaaendeSftpFunctionalException("Prøvde å slette " + filename + ", men finnes ikke på filområdet");
            }
            channelSftp.rm(directory + "/" + filename);
        } catch (SftpException e) {
            log.error("{} klarte ikke slette {}", APPLICATION, filename, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke slette " + filename, e);
        }
    }

    public boolean isConnected() {
        return channelSftp.isConnected();
    }

    public void connect() {
        try {
            JSch jsch = new JSch();
            jschSession = jsch.getSession(username, host, Integer.parseInt(port));
            jsch.addIdentity(privateKey);
            jsch.setKnownHosts(hostKey);

            jschSession.connect();

            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect();
            setHomePath(channelSftp.getHome());
        } catch (JSchException | SftpException e) {
            log.error("{} klarte ikke koble til {}", APPLICATION, host, e);
            throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke koble til " + host, e);
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                channelSftp.exit();
                jschSession.disconnect();
                log.info("{} koblet fra {}", APPLICATION, host);
            } catch (Exception e) {
                log.error("{} klarte ikke koble fra {}", APPLICATION, host, e);
                throw new SkanmotutgaaendeSftpTechnicalException("Klarte ikke koble fra " + host, e);
            }
        } else {
            log.warn("{} prøvde å koble fra sftp, men var ikke koblet til", APPLICATION);
        }
    }

    public String getHomePath() {
        return homePath;
    }

    // A bit hacky, but ChannelSftp does not handle windows paths very well.
    public void setHomePath(String homePath) {
        Pattern windowsFileSystemPattern = Pattern.compile("^[a-zA-Z]:/");
        Matcher windowsFileSystemMatcher = windowsFileSystemPattern.matcher(homePath);
        if (windowsFileSystemMatcher.find()) {
            this.homePath = homePath.substring(2);
        } else {
            this.homePath = homePath;
        }
    }

    private void checkSftpConnection() {
        if (channelSftp == null || !isConnected()) {
            log.error("{} er ikke tilkoblet sftp, men prøver å gjøre behandlinger", APPLICATION);
            throw new SkanmotutgaaendeUnzipperFunctionalException("Er ikke tilkoblet sftp, men prøver å gjøre behandlinger");
        }
    }
}
