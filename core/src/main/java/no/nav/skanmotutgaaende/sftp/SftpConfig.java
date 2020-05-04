package no.nav.skanmotutgaaende.sftp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SftpConfig {

    @Bean()
    Sftp sftp(
            @Value("${skanmotutgaaende.sftp.host}") String sftpHost,
            @Value("${skanmotutgaaende.sftp.privatekey}") String privateKey,
            @Value("${skanmotutgaaende.sftp.hostkey}") String hostKey,
            @Value("${skanmotutgaaende.sftp.username}") String sftpUsername,
            @Value("${skanmotutgaaende.sftp.port}") String sftpPort
    ) {
        try {
            return new Sftp(sftpHost, sftpUsername, sftpPort, privateKey, hostKey);
        } catch (Exception e) {
            log.error("Klarte ikke initialisere SFTP");
            throw e;
        }
    }
}
