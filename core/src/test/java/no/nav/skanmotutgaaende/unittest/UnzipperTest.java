package no.nav.skanmotutgaaende.unittest;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.utils.Unzipper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnzipperTest {

    private final String JOURNALPOST_ID = "005";
    private final String ZIP_FILE_PATH = "src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip";
    private final String ZIPPED_PDF_NAME = "1408-005.pdf";

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = new File(ZIP_FILE_PATH);
        List<FilepairWithMetadata> extracted = Unzipper.unzipXmlPdf(zip);
        FilepairWithMetadata pair = getSkanningmetadataPdfPairFromPdfName(extracted, ZIPPED_PDF_NAME);

        assertEquals(10, extracted.size());
        assertEquals(JOURNALPOST_ID, pair.getSkanningmetadata().getJournalpost().getJournalpostId());
    }

    private FilepairWithMetadata getSkanningmetadataPdfPairFromPdfName(List<FilepairWithMetadata> filepairWithMetadata, String name) {
        return filepairWithMetadata.stream()
                .filter(pair -> name.equals(pair.getSkanningmetadata().getJournalpost().getFilNavn()))
                .findFirst().get();
    }
}
