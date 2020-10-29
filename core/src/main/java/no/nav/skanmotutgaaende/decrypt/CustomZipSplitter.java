package no.nav.skanmotutgaaende.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipSplitter;

import java.io.InputStream;

@Slf4j
public class CustomZipSplitter extends ZipSplitter {
    public CustomZipSplitter() {
    }

    @Override
    public Object evaluate(Exchange exchange) {
        try {
            Message inputMessage = exchange.getIn();
            //TODO: putt inn passord fra vault
            String zipPassword = "changeme";
            ZipInputStream zip = new ZipInputStream(inputMessage.getBody(InputStream.class), zipPassword.toCharArray());

            return new CustomZipIterator(exchange, zip);
        } catch (Exception e) {
            //TODO: FIX
            throw new SkanmotutgaaendeFunctionalException("Feilet under dekryptering", e);
        }
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        Object result = this.evaluate(exchange);
        return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
    }
}
