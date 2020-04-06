package no.nav.skanmotutgaaende.unittest;

import no.nav.skanmotutgaaende.zip.Unzipper;
import no.nav.skanmotutgaaende.zip.MetadataPdfPair;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

    private final String ZIP_FILE_PATH = "src/test/resources/xml_pdf_pairs_testdata.zip";
    private final String ZIPPED_PDF_NAME = "1408-005.pdf";

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = new File(ZIP_FILE_PATH);
        List<MetadataPdfPair> extracted = Unzipper.unzipXmlPdf(zip);
        long jpid = getSkanningmetadataPdfPairFromPdfName(extracted, ZIPPED_PDF_NAME).getSkanningmetadata().getJournalpost().getJournalpostId();
        assertEquals(10, extracted.size());
        assertEquals(5, jpid);
    }

    private MetadataPdfPair getSkanningmetadataPdfPairFromPdfName(List<MetadataPdfPair> metadataPdfPairs, String name) {
        return metadataPdfPairs.stream()
                .filter(pair -> name.equals(pair.getSkanningmetadata().getJournalpost().getFilNavn()))
                .findFirst().get();
    }
}
