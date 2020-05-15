package no.nav.skanmotutgaaende.unzipskanningmetadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.utils.Utils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Unzipper {

    public static List<Filepair> unzipXmlPdf(byte[] zip) throws IOException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(new ByteArrayInputStream(zip));
        return unzipXmlPdf(zipInputStream);
    }

    public static List<Filepair> unzipXmlPdf(File zip) throws IOException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(new FileInputStream(zip));
        return unzipXmlPdf(zipInputStream);
    }

    public static List<Filepair> unzipXmlPdf(ZipArchiveInputStream zipInputStream) throws IOException {
        Map<String, byte[]> xmls = new HashMap<>();
        Map<String, byte[]> pdfs = new HashMap<>();
        byte[] buffer = new byte[1024];
        ZipArchiveEntry zipEntry = zipInputStream.getNextZipEntry();

        while (zipEntry != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len;
            while ((len = zipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            if ("xml".equals(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                xmls.put(Utils.removeFileExtensionInFilename(zipEntry.getName()), byteArrayOutputStream.toByteArray());
            }
            else if ("pdf".equals(UnzipSkanningmetadataUtils.getFileType(zipEntry))) {
                pdfs.put(Utils.removeFileExtensionInFilename(zipEntry.getName()), byteArrayOutputStream.toByteArray());
            }
            byteArrayOutputStream.close();
            zipEntry = zipInputStream.getNextZipEntry();
        }
        zipInputStream.close();

        return UnzipSkanningmetadataUtils.pairFiles(pdfs, xmls);
    }
}
