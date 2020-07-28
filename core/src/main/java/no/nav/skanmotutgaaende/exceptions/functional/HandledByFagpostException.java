package no.nav.skanmotutgaaende.exceptions.functional;

public class HandledByFagpostException extends AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics {

	public HandledByFagpostException(String message) {
		super(message);
	}

	public HandledByFagpostException(String message, Throwable cause) {
		super(message, cause);
	}
}
