package no.nav.skanmotutgaaende.exceptions.functional;

public class AbstractSkanmotutgaaendeFunctionalException extends RuntimeException {

    public AbstractSkanmotutgaaendeFunctionalException(String message) {
        super(message);
    }

    public AbstractSkanmotutgaaendeFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
