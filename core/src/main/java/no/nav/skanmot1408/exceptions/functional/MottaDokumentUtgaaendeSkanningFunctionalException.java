package no.nav.skanmot1408.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MottaDokumentUtgaaendeSkanningFunctionalException extends AbstractSkanmot1408FunctionalException {

    public MottaDokumentUtgaaendeSkanningFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
