package no.nav.skanmot1408;

import no.nav.skanmot1408.config.properties.Skanmot1408Properties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(Skanmot1408Properties.class)
@Configuration
public class ApplicationConfig {


}
