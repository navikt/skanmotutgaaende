package no.nav.skanmotutgaaende;

import no.nav.skanmotutgaaende.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ErrorMetricsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Object exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        if (exception instanceof Throwable) {
            DokCounter.incrementError((Throwable) exception);
        }

    }
}
