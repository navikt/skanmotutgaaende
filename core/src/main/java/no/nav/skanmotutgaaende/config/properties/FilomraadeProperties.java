package no.nav.skanmotutgaaende.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@Validated
public class FilomraadeProperties {

    @NotEmpty
    private String inngaaendemappe;

    @NotEmpty
    private String utgaaendemappe;
}
