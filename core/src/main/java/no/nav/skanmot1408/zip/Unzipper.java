package no.nav.skanmot1408.zip;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmot1408.entities.Skanningmetadata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class Unzipper {

    public static List<MetadataPdfPair> unzipXmlPdf(File zip) throws IOException {
        List<Skanningmetadata> skanningmetadatas = new ArrayList<>();
        Map<String, byte[]> pdfs = new HashMap<>();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            if ("xml".equals(getFileType(zipEntry))) {
                skanningmetadatas.add(bytesToSkanningmetadata(baos.toByteArray()));
            }
            if ("pdf".equals(getFileType(zipEntry))) {
                pdfs.put(zipEntry.getName(), baos.toByteArray());
            }
            baos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        return pairFiles(skanningmetadatas, pdfs);
    }

    private static List<MetadataPdfPair> pairFiles(List<Skanningmetadata> xmls, Map<String, byte[]> pdfs) {
        List<MetadataPdfPair> combined = new ArrayList<>();
        for (Skanningmetadata xml : xmls) {
            for (String pdfName : pdfs.keySet()) {
                if (pdfName.equals(xml.getJournalpost().getFilNavn())) {
                    combined.add(MetadataPdfPair.builder()
                            .skanningmetadata(xml)
                            .pdf(pdfs.get(pdfName))
                            .build()
                    );
                    break;
                }
            }
        }
        return combined;
    }

    private static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) throws UnsupportedEncodingException {
        String xmlString = new String(bytes, "UTF-8");
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));

            return skanningmetadata;
        } catch (JAXBException e) {
            log.warn("Could not convert file to skanningmetadata");//e.printStackTrace();
            return null;
        }
    }

    private static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }
}
