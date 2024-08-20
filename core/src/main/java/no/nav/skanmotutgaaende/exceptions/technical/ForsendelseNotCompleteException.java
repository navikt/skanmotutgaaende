package no.nav.skanmotutgaaende.exceptions.technical;


/**
 * Brukes når en inngående forsendelse ikke er komplett.
 * Eks mangler pdf eller xml.
 */
public class ForsendelseNotCompleteException extends AbstractSkanmotutgaaendeTechnicalException {
    public ForsendelseNotCompleteException(String message) {
        super(message);
    }
}
