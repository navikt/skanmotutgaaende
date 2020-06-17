package no.nav.skanmotutgaaende.unzipskanningmetadata;

import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeUnzipperTechnicalException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnzipperTest {

    private final String ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip";
    private final String BROKEN_ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_broken_testdata.zip";
    private final String PDF_PATH = "src/test/resources/__files/data_002.pdf";
    private final String XML_PATH = "src/test/resources/__files/data_002.xml";

    private final String ZIPPED_PDF_NAME = "data_002.pdf";
    private final String JOURNALPOST_ID = "002";
    private final String MOTTAKSKANAL = "SKAN_IM";
    private final long DATO_MOTTATT = ***gammelt_fnr***00L;
    private final String BATCH_NAVN = "xml_pdf_pairs_testdata.zip";
    private final String ENDORSERNR = "3110190003NAV743506";
    private final String POSTBOKS = "1408";
    private final byte CR = 13;

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = Paths.get(ZIP_FILE_PATH).toFile();
        byte[] pdf = Files.readAllBytes(Path.of(PDF_PATH));
        byte[] xml = Files.readAllBytes(Path.of(XML_PATH));
        List<Filepair> filepairs = Unzipper.unzipXmlPdf(zip);
        List<FilepairWithMetadata> extracted = filepairs.stream().map(filepair -> UnzipSkanningmetadataUtils.extractMetadata(filepair)).collect(Collectors.toList());
        FilepairWithMetadata pair = getSkanningmetadataPdfPairFromPdfName(extracted, ZIPPED_PDF_NAME);
        Journalpost journalpost = pair.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = pair.getSkanningmetadata().getSkanningInfo();

        assertEquals(4, extracted.size());
        assertEquals(JOURNALPOST_ID, journalpost.getJournalpostId());
        assertEquals(MOTTAKSKANAL, journalpost.getMottakskanal());
        assertEquals(DATO_MOTTATT, journalpost.getDatoMottatt().getTime());
        assertEquals(BATCH_NAVN, journalpost.getBatchnavn());
        assertEquals(ZIPPED_PDF_NAME, journalpost.getFilnavn());
        assertEquals(ENDORSERNR, journalpost.getEndorsernr());
        assertEquals(POSTBOKS, skanningInfo.getFysiskPostboks());
        assertEquals(POSTBOKS, skanningInfo.getStrekkodePostboks());
        assertArrayEqualsIgnoreCR(xml, pair.getXml());
        assertArrayEqualsIgnoreCR(pdf, pair.getPdf());
    }

    @Test
    public void shouldThrowExceptionIfUnableToReadMetadata() {
        File zip = Paths.get(BROKEN_ZIP_FILE_PATH).toFile();
        assertThrows(SkanmotutgaaendeUnzipperTechnicalException.class, () ->
                Unzipper.unzipXmlPdf(zip).stream().map(filepair ->
                        UnzipSkanningmetadataUtils.extractMetadata(filepair))
                        .collect(Collectors.toList()));
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
        for (byte b : array) {
            if (CR != b) {
                byteArrayWithoutCR[i++] = b;
            }
        }
        return byteArrayWithoutCR;
    }

    private FilepairWithMetadata getSkanningmetadataPdfPairFromPdfName(List<FilepairWithMetadata> filepairWithMetadata, String name) {
        return filepairWithMetadata.stream()
                .filter(pair -> name.equals(pair.getSkanningmetadata().getJournalpost().getFilnavn()))
                .findFirst().get();
    }
}
