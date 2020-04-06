package no.nav.skanmot1408.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("dokmot1408")
@Validated
public class Skanmot1408Properties {

    @NotNull
    private String dokarkivjournalposturl;

    @NotNull
    private ServiceUserProperties serviceuser;
}


