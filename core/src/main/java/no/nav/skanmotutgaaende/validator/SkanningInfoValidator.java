package no.nav.skanmotutgaaende.validator;

import java.util.Set;

public class SkanningInfoValidator {

    public static final Set<String> strekkodePostboksVerdier = Set.of("1408");

    public boolean isValidFysiskPostboks(String fysiskPostboks) {
        return isNonEmptyString(fysiskPostboks);
    }

    public boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return strekkodePostboksVerdier.contains(strekkodePostboks);
    }

    private boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
