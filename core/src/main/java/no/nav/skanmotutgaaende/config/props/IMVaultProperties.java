package no.nav.skanmotutgaaende.config.props;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties("skanmotutgaaende.vault")
public class IMVaultProperties {

    @NotBlank
    private String backend;

    @NotBlank
    private String kubernetespath;

}