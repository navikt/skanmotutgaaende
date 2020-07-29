package no.nav.skanmotutgaaende.exceptions.functional;

public class FunctionalExceptionHandled extends AbstractSkanmotutgaaendeFunctionalException{

	public FunctionalExceptionHandled(String message) {
		super(message);
	}

	public FunctionalExceptionHandled(String message, Throwable cause) {
		super(message, cause);
	}
}
