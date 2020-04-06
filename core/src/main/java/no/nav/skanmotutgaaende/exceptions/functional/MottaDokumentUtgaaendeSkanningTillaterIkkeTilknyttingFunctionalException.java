package no.nav.skanmotutgaaende.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class MottaDokumentUtgaaendeSkanningTillaterIkkeTilknyttingFunctionalException extends MottaDokumentUtgaaendeSkanningFunctionalException {

    public MottaDokumentUtgaaendeSkanningTillaterIkkeTilknyttingFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
