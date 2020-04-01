package no.nav.skanmot1408.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmot1408.zip.Unzipper;
import no.nav.skanmot1408.zip.MetadataPdfPair;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
public class XmlPdfPairConsumer {

    private final String ZIP_LOCATION = "";

    public List<MetadataPdfPair> getXmlPdfPairs() {
        File zipFile = new File(ZIP_LOCATION);
        try {
            Unzipper.unzipXmlPdf(zipFile);
        } catch (IOException e) {
            log.error("Could not unzip zip-file", e);
        }
        return null;
    }

}
