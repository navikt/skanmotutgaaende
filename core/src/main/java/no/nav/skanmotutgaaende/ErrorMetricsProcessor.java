package no.nav.skanmotutgaaende;

import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.web.client.HttpServerErrorException;

public class ErrorMetricsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Object exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        if (exception instanceof Throwable && !avoidMetrics(exception)) {
            DokCounter.incrementError((Throwable) exception);
        }

    }

    private boolean avoidMetrics(Object exception) {
        return exception instanceof AbstractSkanmotUtgaaendeFunctionalExceptionNoMetrics;
    }
}
