package no.nav.skanmotutgaaende.validator;

import java.util.Set;

public class SkanningInfoValidator {

    public static final Set<String> strekkodePostboksVerdier = Set.of("1408");

    public boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return strekkodePostboksVerdier.contains(strekkodePostboks);
    }
}
