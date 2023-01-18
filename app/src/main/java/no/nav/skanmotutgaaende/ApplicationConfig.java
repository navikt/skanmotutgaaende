package no.nav.skanmotutgaaende;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotutgaaende.config.props.IMVaultProperties;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.consumers.azure.AzureProperties;
import no.nav.skanmotutgaaende.metrics.DokTimedAspect;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties({
        SkanmotutgaaendeProperties.class,
        IMVaultProperties.class,
        AzureProperties.class
})
@Configuration
public class ApplicationConfig {

    @Bean
    public DokTimedAspect timedAspect(MeterRegistry registry) {
        return new DokTimedAspect(registry);
    }

}
