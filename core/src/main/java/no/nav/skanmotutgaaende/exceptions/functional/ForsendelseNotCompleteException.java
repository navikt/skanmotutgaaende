package no.nav.skanmotutgaaende.exceptions.functional;

/**
 * Brukes når en inngående forsendelse ikke er komplett.
 * Eks mangler pdf eller xml.
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
public class ForsendelseNotCompleteException extends AbstractSkanmotutgaaendeFunctionalException {
    public ForsendelseNotCompleteException(String message) {
        super(message);
    }
}
