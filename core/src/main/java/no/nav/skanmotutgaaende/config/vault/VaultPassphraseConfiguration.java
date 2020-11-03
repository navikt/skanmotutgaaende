package no.nav.skanmotutgaaende.config.vault;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.annotation.VaultPropertySource;

@Configuration
@VaultPropertySource(
        value = "${skanmotutgaaende.vault.secretpath}",
        propertyNamePrefix = "skanmotutgaaende.secret.",
        ignoreSecretNotFound = false
)
@ConditionalOnProperty("spring.cloud.vault.enabled")
public class VaultPassphraseConfiguration {

}
