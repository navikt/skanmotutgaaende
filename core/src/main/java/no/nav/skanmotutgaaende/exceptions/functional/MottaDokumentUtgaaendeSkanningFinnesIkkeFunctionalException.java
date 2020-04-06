package no.nav.skanmotutgaaende.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException extends MottaDokumentUtgaaendeSkanningFunctionalException {

    public MottaDokumentUtgaaendeSkanningFinnesIkkeFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
