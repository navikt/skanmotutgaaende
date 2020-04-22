package no.nav.skanmotutgaaende.validator;

public class SkanningInfoValidator {

    public boolean isValidFysiskPostboks(String fysiskPostboks) {
        return isNonEmptyString(fysiskPostboks);
    }

    public boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return "1408".equals(strekkodePostboks);
    }

    private boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
