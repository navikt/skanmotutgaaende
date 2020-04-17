package no.nav.skanmotutgaaende.unittest;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.utils.Unzipper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnzipperTest {

    private final String ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip";
    private final String INVALID_ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_invalid_testdata.zip";
    private final String ZIPPED_PDF_NAME = "1408-005.pdf";
    private final String JOURNALPOST_ID = "005";
    private final String MOTTAKSKANAL = "SKAN_IM";
    private final long DATO_MOTTATT = ***gammelt_fnr***00L;
    private final String BATCH_NAVN = "xml_pdf_pairs_testdata.zip";
    private final String FIL_NAVN = "1408-005.pdf";
    private final String ENDORSERNR = "3110190003NAV743506";
    private final String FYSISK_POSTBOKS = "1400";
    private final String STREKKODE_POSTBOKS = "1400";

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = new File(ZIP_FILE_PATH);
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
        assertNotNull(pair.getXml());
        assertNotNull(pair.getPdf());
    }

    @Test
    public void shouldThrowExceptionIfUnableToReadMetadata() {
        File zip = new File(INVALID_ZIP_FILE_PATH);
        assertThrows(SkanmotutgaaendeUnzipperFunctionalException.class, () -> Unzipper.unzipXmlPdf(zip));
    }

    private FilepairWithMetadata getSkanningmetadataPdfPairFromPdfName(List<FilepairWithMetadata> filepairWithMetadata, String name) {
        return filepairWithMetadata.stream()
                .filter(pair -> name.equals(pair.getSkanningmetadata().getJournalpost().getFilNavn()))
                .findFirst().get();
    }
}
