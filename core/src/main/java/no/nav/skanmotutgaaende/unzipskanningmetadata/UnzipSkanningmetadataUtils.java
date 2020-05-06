package no.nav.skanmotutgaaende.unzipskanningmetadata;

import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.utils.Utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class UnzipSkanningmetadataUtils {

    public static List<FilepairWithMetadata> pairFiles(List<Skanningmetadata> skanningmetadataList, Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return skanningmetadataList.stream().map(metadata -> {
            String pdfFilnavn = metadata.getJournalpost().getFilNavn();
            String xmlFilnavn = Utils.changeFiletypeInFilename(pdfFilnavn, "xml");
            if (!pdfs.containsKey(pdfFilnavn)) {
                throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende fant ikke tilhørende pdf-fil til journalpost " + metadata.getJournalpost().getJournalpostId());
            }
            if (!xmls.containsKey(xmlFilnavn)) {
                throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende fant ikke tilhørende xml-fil til journalpost " + metadata.getJournalpost().getJournalpostId());
            }
            return FilepairWithMetadata.builder()
                    .skanningmetadata(metadata)
                    .pdf(pdfs.get(pdfFilnavn))
                    .xml(xmls.get(xmlFilnavn))
                    .build();
        }).collect(Collectors.toList());
    }

    public static List<Filepair> pairFiles(Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return pdfs.keySet().stream().map(pdfName ->
                Filepair.builder()
                        .name(Utils.removeFileExtensionInFilename(pdfName))
                        .pdf(pdfs.get(pdfName))
                        .xml(xmls.get(Utils.changeFiletypeInFilename(pdfName, "xml")))
                        .build()
        ).collect(Collectors.toList());
    }

    public static FilepairWithMetadata extractMetadata(Filepair filepair) {
        return FilepairWithMetadata.builder()
                .skanningmetadata(bytesToSkanningmetadata(filepair.getXml()))
                .pdf(filepair.getPdf())
                .xml(filepair.getXml())
                .build();
    }

    public static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) {
        try {
            String xmlString = new String(bytes, "UTF-8");
            JAXBContext jaxbContext;
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));

            skanningmetadata.verifyFields();

            return skanningmetadata;
        } catch (UnsupportedEncodingException | JAXBException e) {
            throw new SkanmotutgaaendeUnzipperFunctionalException("Skanmotutgaaende klarte ikke lese metadata i zipfil", e);
        }
    }

    public static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }
}
