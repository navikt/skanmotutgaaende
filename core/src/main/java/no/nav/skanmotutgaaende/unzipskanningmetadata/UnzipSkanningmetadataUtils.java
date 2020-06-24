package no.nav.skanmotutgaaende.unzipskanningmetadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeUnzipperFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.SkanmotutgaaendeUnzipperTechnicalException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

@Slf4j
public class UnzipSkanningmetadataUtils {

    public static List<Filepair> pairFiles(Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return pdfs.keySet().stream().map(key ->
                Filepair.builder()
                        .name(key)
                        .pdf(pdfs.get(key))
                        .xml(xmls.get(key))
                        .build())
                .collect(Collectors.toList());
    }

    public static FilepairWithMetadata extractMetadata(Filepair filepair) {
        return FilepairWithMetadata.builder()
                .name(filepair.getName())
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

            return skanningmetadata;
        } catch (UnsupportedEncodingException | JAXBException e) {
            log.error("Skanmotutgaaende klarte ikke lese metadata i fil, feilmelding={}", e.getMessage(), e);
            throw new SkanmotutgaaendeUnzipperTechnicalException("Skanmotutgaaende klarte ikke unmarshalle skanningmetadata fra xml", e);
        } catch (NullPointerException e) {
            throw new SkanmotutgaaendeUnzipperFunctionalException("Xml fil mangler");
        }
    }

    public static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
    }
}
