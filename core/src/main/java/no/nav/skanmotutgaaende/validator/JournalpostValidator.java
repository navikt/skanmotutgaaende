package no.nav.skanmotutgaaende.validator;

import java.util.Date;

public class JournalpostValidator {

    public boolean isValidJournalpostId(String journalpostId) {
        return isNumeric(journalpostId);
    }

    public boolean isValidMottakskanal(String mottakskanal) {
        return isNonEmptyString(mottakskanal);
    }

    public boolean isValidDatoMottatt(Date datoMottatt) {
        return true; // No current restrictions
    }

    public boolean isValidBatchNavn(String batchnavn) {
        return isNonEmptyString(batchnavn);
    }

    public boolean isValidFilnavn(String filnavn) {
        if (isNonEmptyString(filnavn) && filnavn.length() >= 5) {
            String fileEnding = filnavn.substring(filnavn.length() - 4);
            return '.' == fileEnding.charAt(0);
        }
        return false;
    }

    public boolean isValidEndorsernr(String endorsernr) {
        return isNonEmptyString(endorsernr);
    }

    private boolean isNumeric(String string) {
        if (isNonEmptyString(string)) {
            try {
                Float.parseFloat(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
