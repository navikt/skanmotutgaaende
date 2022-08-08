package no.nav.skanmotutgaaende.config.props;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties("skanmotutgaaende")
public class SkanmotutgaaendeProperties {

    private final Proxy proxy = new Proxy();
    private final Endpoints endpoints = new Endpoints();
    private final SftpProperties sftp = new SftpProperties();
    private final FilomraadeProperties filomraade = new FilomraadeProperties();
    private final ServiceUserProperties serviceuser = new ServiceUserProperties();

    @NotEmpty
    private String endpointuri;

    @NotNull
    private String schedule;

    @NotNull
    private Duration completiontimeout;

    @Data
    @Validated
    public static class Proxy {
        private String host;
        private int port;

        public boolean isSet() {
            return (host != null && !host.equals(""));
        }
    }

    @Data
    @Validated
    public static class Endpoints {

        @NotNull
        private AzureEndpoint dokarkiv;
    }

    @Data
    @Validated
    public static class AzureEndpoint {
        /**
         * Url til tjeneste som har azure autorisasjon
         */
        @NotEmpty
        private String url;
        /**
         * Scope til azure client credential flow
         */
        @NotEmpty
        private String scope;
    }

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


