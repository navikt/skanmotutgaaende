package no.nav.skanmotutgaaende.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.functional.FunctionalExceptionHandled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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
    private static final String FUNCTIONAL_ERROR_HANDLED = "functional_handled";
    public static final String DOMAIN = "domain";
    public static final String UTGAAENDE = "utgaaende";

    private static MeterRegistry meterRegistry;

    @Autowired
    public DokCounter(MeterRegistry meterRegistry){
        DokCounter.meterRegistry = meterRegistry;
    }

    public static void incrementCounter(Map<String, String> metadata){
        metadata.forEach(DokCounter::incrementCounter);
    }
    public static void incrementCounter(String key, List<String> tags) {
        Counter.builder(DOK_SKANMOTUTGAAENDE + key + TOTAL)
                .tags(tags.toArray(new String[0]))
                .register(meterRegistry)
                .increment();
    }

    private static void incrementCounter(String key, String value){
        Counter.builder(DOK_SKANMOTUTGAAENDE + key + TOTAL)
                .tags(key, value)
                .register(meterRegistry)
                .increment();
    }

    public static void incrementError(Throwable throwable){
        Counter.builder(DOK_SKANMOTUTGAAENDE + EXCEPTION)
                .tags(ERROR_TYPE, getErrorType(throwable))
                .tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
                .tag(DOMAIN, UTGAAENDE)
                .register(meterRegistry)
                .increment();
    }

    private static String getErrorType(Throwable e){
        if(e instanceof FunctionalExceptionHandled){
            return FUNCTIONAL_ERROR_HANDLED;
        }else if(e instanceof AbstractSkanmotutgaaendeFunctionalException){
            return FUNCTIONAL_ERROR;
        }else
            return TECHNICAL_ERROR;
    }

}
