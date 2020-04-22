package no.nav.skanmotutgaaende.utils;

public class Utils {

    public static String changeFiletypeInFilename(String originalName, String newFiletype) {
        String nameNoSuffix = originalName.substring(0, originalName.lastIndexOf(".") + 1);
        return nameNoSuffix + newFiletype;
    }
}
