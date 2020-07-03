package no.nav.skanmotutgaaende.utils;

public class Utils {

    public static String changeFiletypeInFilename(String originalName, String newFiletype) {
        return removeFileExtensionInFilename(originalName) + "." + newFiletype;
    }

    public static String removeFileExtensionInFilename(String originalName) {
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex == -1) {
            return originalName;
        }
        return originalName.substring(0, dotIndex);
    }

    public static String removeLeadingZeros(String s) {
        return s.replaceFirst("^0+(?!$)", "");
    }
}