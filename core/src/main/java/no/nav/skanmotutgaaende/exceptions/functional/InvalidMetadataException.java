package no.nav.skanmotutgaaende.exceptions.functional;

public class InvalidMetadataException extends AbstractSkanmotutgaaendeFunctionalException {

    public InvalidMetadataException(String message) {
        super(message);
    }

    public InvalidMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
