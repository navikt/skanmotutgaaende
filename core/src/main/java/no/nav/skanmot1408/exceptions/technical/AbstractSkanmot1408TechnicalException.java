package no.nav.skanmot1408.exceptions.technical;

public class AbstractSkanmot1408TechnicalException extends RuntimeException {

    public AbstractSkanmot1408TechnicalException(String message) {
        super(message);
    }

    public AbstractSkanmot1408TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
