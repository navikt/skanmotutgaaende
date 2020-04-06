package no.nav.skanmot1408.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@Validated
public class ServiceUserProperties {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

} 
