package no.nav.skanmotutgaaende.unzipskanningmetadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if ("xml".equals(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                skanningmetadatas.add(UnzipSkanningmetadataUtils.bytesToSkanningmetadata(byteArrayOutputStream.toByteArray()));
                xmls.put(zipEntry.getName(), byteArrayOutputStream.toByteArray());
            }
            if ("pdf".equals(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                pdfs.put(zipEntry.getName(), byteArrayOutputStream.toByteArray());
            }
            byteArrayOutputStream.close();
            zipEntry = zipInputStream.getNextZipEntry();
        }
        zipInputStream.close();

        return UnzipSkanningmetadataUtils.pairFiles(skanningmetadatas, pdfs, xmls);
    }
}
