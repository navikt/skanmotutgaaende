package no.nav.skanmotutgaaende.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipSplitter;

import java.io.InputStream;

@Slf4j
public class ZipSplitterEncrypted extends ZipSplitter {
    public ZipSplitterEncrypted() {
    }

    @Override
    public ZipIteratorEncrypted evaluate(Exchange exchange) {
        Message inputMessage = exchange.getIn();
        //TODO: putt inn passord fra vault
        String zipPassword = "changeme";
        ZipInputStream zip = new ZipInputStream(inputMessage.getBody(InputStream.class), zipPassword.toCharArray());
        return new ZipIteratorEncrypted(exchange, zip);
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        Object result = this.evaluate(exchange);
        return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
    }
}
