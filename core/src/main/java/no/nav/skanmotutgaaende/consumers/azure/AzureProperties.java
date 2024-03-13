package no.nav.skanmotutgaaende.consumers.azure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/usage/#variables-for-acquiring-tokens:~:text=nais.io/azure/-,Variables%20for%20Acquiring%20Tokens,-%C2%B6
 */
@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
){}