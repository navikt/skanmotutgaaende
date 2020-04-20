package no.nav.skanmotutgaaende.validator;

import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;

import java.util.Date;

public class JournalpostValidator {

    public static boolean isValidJournalpostId(String journalpostId) {
        return isNumeric(journalpostId);
    }

    public static boolean isValidMottakskanal(String mottakskanal) {
        return isNonEmptyString(mottakskanal);
    }

    public static boolean isValidDatoMottatt(Date datoMottatt) {
        return null != datoMottatt;
    }

    public static boolean isValidBatchNavn(String batchnavn) {
        return isNonEmptyString(batchnavn);
    }

    public static boolean isValidFilnavn(String filnavn) {
        if (isNonEmptyString(filnavn) && filnavn.length() >= 5) {
            String fileEnding = filnavn.substring(filnavn.length() - 4);
            return '.' == fileEnding.charAt(0);
        }
        return false;
    }

    public static boolean isValidEndorsernr(String endorsernr) {
        return isNonEmptyString(endorsernr);
    }

    private static boolean isNumeric(String string) {
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

    private static boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
