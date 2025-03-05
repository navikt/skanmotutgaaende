package no.nav.skanmotutgaaende.validator;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class JournalpostValidator {

    public boolean isValidJournalpostId(String journalpostId) {
        return isNumeric(journalpostId);
    }

    public boolean isValidMottakskanal(String mottakskanal) {
        return isNotEmpty(mottakskanal);
    }

    public boolean isValidBatchNavn(String batchnavn) {
        return isNotEmpty(batchnavn);
    }

    private boolean isNumeric(String string) {
        if (isNotEmpty(string)) {
            try {
                Float.parseFloat(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

}
