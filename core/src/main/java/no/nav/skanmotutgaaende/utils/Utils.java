package no.nav.skanmotutgaaende.utils;

public class Utils {
    public static String removeLeadingZeros(String s) {
        return s.replaceFirst("^0+(?!$)", "");
    }
}