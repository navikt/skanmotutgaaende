package no.nav.skanmotutgaaende.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class DokCounter {
    private static final String DOK_SKANMOTUTGAAENDE = "dok_skanmotutgaaende_";
    private static final String TOTAL = "_total";
    private static final String EXCEPTION = "exception";
    private static final String ERROR_TYPE = "error_type";
    private static final String EXCEPTION_NAME = "exception_name";
    private static final String FUNCTIONAL_ERROR = "functional";
    private static final String TECHNICAL_ERROR = "technical";
    private static final String DOMAIN = "domain";
    private static final String UTGAAENDE = "utgaaende";

    private static MeterRegistry meterRegistry;
    @Inject
    public DokCounter(MeterRegistry meterRegistry){
        DokCounter.meterRegistry = meterRegistry;
    }

    public static void incrementCounter(Map<String, String> metadata){
        metadata.forEach(DokCounter::incrementCounter);
    }

    private static void incrementCounter(String key, String value) {
        Counter.builder(DOK_SKANMOTUTGAAENDE + key + TOTAL)
                .tags(key, value)
                .register(meterRegistry)
                .increment();
    }

    public static void incrementError(Throwable throwable){
        Counter.builder(DOK_SKANMOTUTGAAENDE + EXCEPTION)
                .tags(ERROR_TYPE, isFunctionalException(throwable) ? FUNCTIONAL_ERROR : TECHNICAL_ERROR)
                .tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
                .tag(DOMAIN, UTGAAENDE)
                .register(meterRegistry)
                .increment();
    }

    private static boolean isFunctionalException(Throwable e) {
        return e instanceof AbstractSkanmotutgaaendeFunctionalException;
    }

    private static boolean isEmptyString(String string) {
        return string == null || string.isBlank();
    }
}
