package no.nav.skanmot1408.exceptions.technical;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class MottaDokumentUtgaaendeSkanningTechnicalException extends AbstractSkanmot1408TechnicalException {

    public MottaDokumentUtgaaendeSkanningTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
