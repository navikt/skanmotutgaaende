package no.nav.skanmotutgaaende.exceptions.technical;

public class AbstractSkanmotutgaaendeTechnicalException extends RuntimeException {

    public AbstractSkanmotutgaaendeTechnicalException(String message) {
        super(message);
    }

    public AbstractSkanmotutgaaendeTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
