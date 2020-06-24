package no.nav.skanmotutgaaende.itest.config;

import no.nav.skanmotutgaaende.config.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.config.CoreConfig;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotutgaaendeProperties.class)
@Import({CoreConfig.class, DokCounter.class})
public class TestConfig {
}
