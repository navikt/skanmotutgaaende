package no.nav.skanmotutgaaende.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MottaDokumentUtgaaendeSkanningFunctionalException extends AbstractSkanmotutgaaendeFunctionalException {

    public MottaDokumentUtgaaendeSkanningFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
