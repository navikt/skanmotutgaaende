package no.nav.skanmotutgaaende.unittest;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.unzipskanningmetadata.Unzipper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnzipperTest {

    private final String ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip";
    private final String BROKEN_ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_broken_testdata.zip";
    private final String INVALID_ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_invalid_testdata.zip";
    private final String PDF_PATH = "src/test/resources/__files/xml_pdf_pairs/1408-005.pdf";
    private final String XML_PATH = "src/test/resources/__files/xml_pdf_pairs/1408-005.xml";
    private final String ZIPPED_PDF_NAME = "1408-005.pdf";
    private final String JOURNALPOST_ID = "005";
    private final String MOTTAKSKANAL = "SKAN_IM";
    private final long DATO_MOTTATT = ***gammelt_fnr***00L;
    private final String BATCH_NAVN = "xml_pdf_pairs_testdata.zip";
    private final String FIL_NAVN = "1408-005.pdf";
    private final String ENDORSERNR = "3110190003NAV743506";
    private final String FYSISK_POSTBOKS = "1400";
    private final String STREKKODE_POSTBOKS = "1400";
    private final byte CR = 13;

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = Paths.get(ZIP_FILE_PATH).toFile();
        byte[] pdf = Files.readAllBytes(Path.of(PDF_PATH));
        byte[] xml = Files.readAllBytes(Path.of(XML_PATH));
        List<FilepairWithMetadata> extracted = Unzipper.unzipXmlPdf(zip);
        FilepairWithMetadata pair = getSkanningmetadataPdfPairFromPdfName(extracted, ZIPPED_PDF_NAME);
        Journalpost journalpost = pair.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = pair.getSkanningmetadata().getSkanningInfo();

        assertEquals(10, extracted.size());
        assertEquals(JOURNALPOST_ID, journalpost.getJournalpostId());
        assertEquals(MOTTAKSKANAL, journalpost.getMottakskanal());
        assertEquals(DATO_MOTTATT, journalpost.getDatoMottatt().getTime());
        assertEquals(BATCH_NAVN, journalpost.getBatchNavn());
        assertEquals(FIL_NAVN, journalpost.getFilNavn());
        assertEquals(ENDORSERNR, journalpost.getEndorsernr());
        assertEquals(FYSISK_POSTBOKS, skanningInfo.getFysiskPostboks());
        assertEquals(STREKKODE_POSTBOKS, skanningInfo.getStrekkodePostboks());
        assertArrayEqualsIgnoreCR(xml, pair.getXml());
        assertArrayEqualsIgnoreCR(pdf, pair.getPdf());
    }

    @Test
    public void shouldThrowExceptionIfUnableToReadMetadata() {
        File zip = Paths.get(BROKEN_ZIP_FILE_PATH).toFile();
        assertThrows(SkanmotutgaaendeUnzipperFunctionalException.class, () -> Unzipper.unzipXmlPdf(zip));
    }

    @Test
    public void shouldThrowExceptionIfInvalidMetadata() {
        File zip = Paths.get(INVALID_ZIP_FILE_PATH).toFile();
        assertThrows(InvalidMetadataException.class, () -> Unzipper.unzipXmlPdf(zip));
    }

    private void assertArrayEqualsIgnoreCR(byte[] expected, byte[] actual) {
        int len = Math.min(expected.length, actual.length);
        byte[] expectedIgnored = removeCR(expected, len);
        byte[] actualIgnored = removeCR(actual, len);
        assertArrayEquals(expectedIgnored, actualIgnored);
    }

    private byte[] removeCR(byte[] array, int len) {
        int i = 0;
        byte[] byteArrayWithoutCR = new byte[len];
        for (byte b: array) {
            if (CR != b) {
                byteArrayWithoutCR[i++] = b;
            }
        }
        return byteArrayWithoutCR;
    }

    private FilepairWithMetadata getSkanningmetadataPdfPairFromPdfName(List<FilepairWithMetadata> filepairWithMetadata, String name) {
        return filepairWithMetadata.stream()
                .filter(pair -> name.equals(pair.getSkanningmetadata().getJournalpost().getFilNavn()))
                .findFirst().get();
    }
}
