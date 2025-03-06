package no.nav.skanmotutgaaende;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotutgaaende.mdc.MDCConstants.MDC_CALL_ID;

public class RemoveMdcProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		MDC.remove(MDC_CALL_ID);
	}
}
