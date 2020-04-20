package no.nav.skanmotutgaaende.utils;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

@Slf4j
public class Unzipper {

    public static List<FilepairWithMetadata> unzipXmlPdf(File zip) throws IOException {
        List<Skanningmetadata> skanningmetadatas = new ArrayList<>();
        Map<String, byte[]> xmls = new HashMap<>();
        Map<String, byte[]> pdfs = new HashMap<>();
        byte[] buffer = new byte[1024];
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(new FileInputStream(zip));
        ZipArchiveEntry zipEntry = zipInputStream.getNextZipEntry();
        while (zipEntry != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len;
            while ((len = zipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            if ("xml".equals(getFileType(zipEntry))) {
                skanningmetadatas.add(bytesToSkanningmetadata(byteArrayOutputStream.toByteArray()));
                xmls.put(zipEntry.getName(), byteArrayOutputStream.toByteArray());
            }
            if ("pdf".equals(getFileType(zipEntry))) {
                pdfs.put(zipEntry.getName(), byteArrayOutputStream.toByteArray());
            }
            byteArrayOutputStream.close();
            zipEntry = zipInputStream.getNextZipEntry();
        }
        zipInputStream.close();

        return pairFiles(skanningmetadatas, pdfs, xmls);
    }

    private static List<FilepairWithMetadata> pairFiles(List<Skanningmetadata> skanningmetadataList, Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return skanningmetadataList.stream().map(metadata -> {
            String pdfFilnavn = metadata.getJournalpost().getFilNavn();
            String xmlFilnavn = Utils.changeFiletypeInFilename(pdfFilnavn, "xml");
            if (!pdfs.containsKey(pdfFilnavn)) {
                throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende fant ikke tilhørende pdf-fil til journalpost " + metadata.getJournalpost().getJournalpostId());
            } if (!xmls.containsKey(xmlFilnavn)) {
                throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende fant ikke tilhørende xml-fil til journalpost " + metadata.getJournalpost().getJournalpostId());
            }
            return FilepairWithMetadata.builder()
                    .skanningmetadata(metadata)
                    .pdf(pdfs.get(pdfFilnavn))
                    .xml(xmls.get(xmlFilnavn))
                    .build();
        }).collect(Collectors.toList());
    }

    private static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) throws UnsupportedEncodingException {
        String xmlString = new String(bytes, "UTF-8");
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));

            skanningmetadata.verifyFields();

            return skanningmetadata;
        } catch (JAXBException e) {
            throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende klarte ikke lese metadata i zipfil", e);
        }
    }

    private static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }
}
