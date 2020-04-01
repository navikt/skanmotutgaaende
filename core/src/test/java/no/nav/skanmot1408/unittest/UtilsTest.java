package no.nav.skanmot1408.unittest;

import no.nav.skanmot1408.entities.Skanningmetadata;
import no.nav.skanmot1408.zip.Unzipper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class UtilsTest {

    @Test
    public void shouldExtractContentFromZip() throws IOException {
        File zip = new File("src/test/resources/xml_pdf_pairs_testdata.zip");
        //List<File> extracted = Unzipper.extractContent(zip);
        List<Skanningmetadata> extracted = Unzipper.unzipAsByte(zip);
        extracted.stream().forEach(b -> {
            if (null != b) {
                System.out.println("JPID: " + b.getJournalpost().getJournalpostId());
                System.out.println("DatoMottatt: " + b.getJournalpost().getDatoMottatt());
                System.out.println("Filnavn: " + b.getJournalpost().getFilNavn());
            }
        });
    }
}
