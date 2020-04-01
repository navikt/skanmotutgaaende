package no.nav.skanmot1408.zip;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmot1408.entities.Skanningmetadata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class Unzipper {

    public static List<Skanningmetadata> unzipAsByte(File zip) throws IOException {
        List<byte[]> output = new ArrayList<>();
        List<Skanningmetadata> skanningmetadatas = new ArrayList<>();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            output.add(baos.toByteArray());
            skanningmetadatas.add(bytesToSkanningmetadata(baos.toByteArray()));
            baos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return skanningmetadatas;
    }

    private static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) throws UnsupportedEncodingException {
        String xmlString = new String(bytes, "UTF-8");
        JAXBContext jaxbContext;
        try
        {
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));

            return skanningmetadata;
        }
        catch (JAXBException e)
        {
            log.warn("Could not convert file to skanningmetadata");//e.printStackTrace();
            return null;
        }
    }
}
