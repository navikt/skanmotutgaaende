package no.nav.skanmotutgaaende.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;

@Slf4j
public class ZipSplitterEncrypted extends ZipSplitter {

    private final String passphrase;

    public ZipSplitterEncrypted(@Value("${skanmotutgaaende.secret.passphrase}") String passphrase) {
        this.passphrase = passphrase;
    }

    @Override
    public ZipIteratorEncrypted evaluate(Exchange exchange) {
        Message inputMessage = exchange.getIn();
        String zipPassword = passphrase;
        ZipInputStream zip = new ZipInputStream(inputMessage.getBody(InputStream.class), zipPassword.toCharArray());
        return new ZipIteratorEncrypted(exchange, zip);
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        Object result = this.evaluate(exchange);
        return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
    }
}
