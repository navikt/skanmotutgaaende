package no.nav.skanmotutgaaende;

import no.nav.skanmotutgaaende.config.properties.SkanmotutgaaendeProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(SkanmotutgaaendeProperties.class)
@Configuration
public class ApplicationConfig {


}
