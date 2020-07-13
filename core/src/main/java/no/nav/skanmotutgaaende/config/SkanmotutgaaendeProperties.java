package no.nav.skanmotutgaaende.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties("skanmotutgaaende")
public class SkanmotutgaaendeProperties {

    @NotNull
    private String dokarkivjournalposturl;

    @NotEmpty
    private String endpointuri;

    @NotEmpty
    private String endpointconfig;

    @NotNull
    private String schedule;

    private final FilomraadeProperties filomraade = new FilomraadeProperties();

    private final SftpProperties sftp = new SftpProperties();

    private final ServiceUserProperties serviceuser = new ServiceUserProperties();

    @Getter
    @Setter
    @Validated
    public static class FilomraadeProperties {

        @NotEmpty
        private String inngaaendemappe;

        @NotEmpty
        private String feilmappe;
    }

    @Getter
    @Setter
    @Validated
    public static class ServiceUserProperties {

        @NotEmpty
        private String username;

        @NotEmpty
        @ToString.Exclude
        private String password;

    }

    @Getter
    @Setter
    @Validated
    public static class SftpProperties {

        @NotNull
        private String host;

        @NotNull
        @ToString.Exclude
        private String privateKey;

        @NotNull
        @ToString.Exclude
        private String hostKey;

        @NotNull
        private String username;

        @NotNull
        private String port;
    }

}


