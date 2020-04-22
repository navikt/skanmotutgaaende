package no.nav.skanmotutgaaende.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotutgaaende")
@Validated
public class SkanmotutgaaendeProperties {

    @NotNull
    private String dokarkivjournalposturl;

    @NotNull
    private ServiceUserProperties serviceuser;
}


