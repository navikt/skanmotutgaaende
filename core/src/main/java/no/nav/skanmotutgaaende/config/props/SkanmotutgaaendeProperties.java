package no.nav.skanmotutgaaende.config.props;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties("skanmotutgaaende")
public class SkanmotutgaaendeProperties {

    private final Endpoints endpoints = new Endpoints();
    private final Utgaaende utgaaende = new Utgaaende();
    private final Avstem avstem = new Avstem();
    private final SftpProperties sftp = new SftpProperties();
    private final FilomraadeProperties filomraade = new FilomraadeProperties();
    private final ServiceUserProperties serviceuser = new ServiceUserProperties();
    private final JiraConfigProperties jira = new JiraConfigProperties();
    private final SlackProperties slack = new SlackProperties();

    @NotEmpty
    private String endpointuri;

    @NotEmpty
    private String endpointconfig;

    @NotNull
    private Duration completiontimeout;

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

    @Data
    @Validated
    public static class Utgaaende {
        @NotEmpty
        private String schedule;
    }

    @Data
    @Validated
    public static class Avstem {
        @NotEmpty
        private String schedule;

        private boolean startup;

    }

    @Getter
    @Setter
    @Validated
    public static class FilomraadeProperties {

        @NotEmpty
        private String inngaaendemappe;

        @NotEmpty
        private String feilmappe;

        @NotEmpty
        private String avstemmappe;
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

    @Data
    @Validated
    public static class JiraConfigProperties {
        @NotEmpty
        private String username;

        @NotEmpty
        private String password;

        @NotEmpty
        private String url;
    }

    @Data
    @Validated
    public static class SlackProperties {
        @NotEmpty
        @ToString.Exclude
        private String token;

        @NotEmpty
        private String channel;

        private boolean enabled;
    }
}


