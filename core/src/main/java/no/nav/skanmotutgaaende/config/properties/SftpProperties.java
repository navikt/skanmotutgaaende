package no.nav.skanmotutgaaende.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Validated
public class SftpProperties {

    @NotNull
    private String host;

    @NotNull
    private String privateKey;

    @NotNull
    private String hostKey;

    @NotNull
    private String username;

    @NotNull
    private String port;
}
