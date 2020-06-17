package no.nav.skanmotutgaaende.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class DokCounter {
    private final String DOK_SKANMOTUTGAAENDE = "dok_skanmotutgaaende_";
    private final String TOTAL = "_total";
    private final String EXCEPTION = "exception";
    private final String ERROR_TYPE = "error_type";
    private final String EXCEPTION_NAME = "exception_name";
    private final String FUNCTIONAL_ERROR = "functional";
    private final String TECHNICAL_ERROR = "technical";
    private final String DOMAIN = "domain";
    private final String UTGAAENDE = "utgaaende";

    private final MeterRegistry meterRegistry;
    @Inject
    public DokCounter(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementCounter(Map<String, String> metadata){
        metadata.forEach(this::incrementCounter);
    }

    private void incrementCounter(String key, String value) {
        Counter.builder(DOK_SKANMOTUTGAAENDE + key + TOTAL)
                .tags(key, value)
                .register(meterRegistry)
                .increment();
    }

    public void incrementError(Throwable throwable){
        Counter.builder(DOK_SKANMOTUTGAAENDE + EXCEPTION)
                .tags(ERROR_TYPE, isFunctionalException(throwable) ? FUNCTIONAL_ERROR : TECHNICAL_ERROR)
                .tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
                .tag(DOMAIN, UTGAAENDE)
                .register(meterRegistry)
                .increment();
    }

    private boolean isFunctionalException(Throwable e) {
        return e instanceof AbstractSkanmotutgaaendeFunctionalException;
    }

    private boolean isEmptyString(String string) {
        return string == null || string.isBlank();
    }
}
