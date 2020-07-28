package no.nav.skanmotutgaaende.exceptions.functional;

public class AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics extends RuntimeException{

	public AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics(String message) {
		super(message);
	}

	public AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics(String message, Throwable cause) {
		super(message, cause);
	}
}
