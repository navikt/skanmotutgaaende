package no.nav.skanmotutgaaende.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

    private final String NAME_NO_EXTENSION = "data_005";
    private final String PDF_NAME = NAME_NO_EXTENSION + ".pdf";
    private final String XML_NAME = NAME_NO_EXTENSION + ".xml";

    @Test
    public void shouldConvertPdfFilenameToXmlFilename() {
        String xmlName = Utils.changeFiletypeInFilename(PDF_NAME, "xml");
        assertEquals(XML_NAME, xmlName);
    }

    @Test
    public void shouldRemoveFileExtension() {
        String noPdfExtension = Utils.removeFileExtensionInFilename(PDF_NAME);
        String noXmlExtension = Utils.removeFileExtensionInFilename(XML_NAME);
        String noExtension = Utils.removeFileExtensionInFilename(NAME_NO_EXTENSION);
        assertEquals(NAME_NO_EXTENSION, noPdfExtension);
        assertEquals(NAME_NO_EXTENSION, noXmlExtension);
        assertEquals(NAME_NO_EXTENSION, noExtension);
    }

    @Test
    public void shouldRemoveLeadingZeros() {
        String s1 = Utils.removeLeadingZeros("0002301");
        String s2 = Utils.removeLeadingZeros("1230");
        String s3 = Utils.removeLeadingZeros("0");
        assertEquals("2301", s1);
        assertEquals("1230", s2);
        assertEquals("0", s3);
    }
}
