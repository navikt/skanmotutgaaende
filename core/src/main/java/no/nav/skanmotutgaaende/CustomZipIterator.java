package no.nav.skanmotutgaaende;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;

public class CustomZipIterator implements Iterator<Message>, Closeable {
    static final Logger LOGGER = LoggerFactory.getLogger(org.apache.camel.dataformat.zipfile.ZipIterator.class);

    private final Exchange exchange;
    private boolean allowEmptyDirectory;
    private volatile ZipInputStream zipInputStream;
    private volatile Message parent;

    public CustomZipIterator(Exchange exchange, InputStream inputStream) {
        this.exchange = exchange;
        this.allowEmptyDirectory = false;
        if (inputStream instanceof ZipInputStream) {
            zipInputStream = (ZipInputStream) inputStream;
        } else {
            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
        }
        parent = null;
    }

    @Override
    public boolean hasNext() {
        try {
            if (zipInputStream == null) {
                return false;
            }
            boolean availableDataInCurrentEntry = zipInputStream.getAvailableBytesInPushBackInputStream() >= 1;
            if (!availableDataInCurrentEntry) {
                // advance to the next entry.
                parent = getNextElement();
                if (parent == null) {
                    zipInputStream.close();
                    availableDataInCurrentEntry = false;
                } else {
                    availableDataInCurrentEntry = true;
                }
            }
            return availableDataInCurrentEntry;
        } catch (IOException exception) {
            throw new RuntimeCamelException(exception);
        }
    }

    @Override
    public Message next() {
        if (parent == null) {
            parent = getNextElement();
        }
        Message answer = parent;
        parent = null;
        checkNullAnswer(answer);

        return answer;
    }

    private Message getNextElement() {
        if (zipInputStream == null) {
            return null;
        }

        try {
            LocalFileHeader current = getNextEntry();

            if (current != null) {
                LOGGER.debug("read zipEntry {}", current.getFileName());
                Message answer = new DefaultMessage(exchange.getContext());
                answer.getHeaders().putAll(exchange.getIn().getHeaders());
                answer.setHeader("zipFileName", current.getFileName());
                answer.setHeader(Exchange.FILE_NAME, current.getFileName());
                answer.setBody(new ZipInputStreamWrapper(zipInputStream));
                return answer;
            } else {
                LOGGER.trace("close zipInputStream");
                return null;
            }
        } catch (IOException exception) {
            throw new RuntimeCamelException(exception);
        }
    }

    public void checkNullAnswer(Message answer) {
        if (answer == null && zipInputStream != null) {
            IOHelper.close(zipInputStream);
            zipInputStream = null;
        }
    }

    private LocalFileHeader getNextEntry() throws IOException {
        LocalFileHeader entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                return entry;
            } else {
                if (allowEmptyDirectory) {
                    return entry;
                }
            }
        }

        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        IOHelper.close(zipInputStream);
        zipInputStream = null;
    }

    public boolean isSupportIteratorForEmptyDirectory() {
        return allowEmptyDirectory;
    }

    public void setAllowEmptyDirectory(boolean allowEmptyDirectory) {
        this.allowEmptyDirectory = allowEmptyDirectory;
    }
}