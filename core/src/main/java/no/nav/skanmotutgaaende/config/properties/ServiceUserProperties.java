package no.nav.skanmotutgaaende.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@ConfigurationProperties("serviceuser")
@Validated
public class ServiceUserProperties {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

} 
