package no.nav.skanmotutgaaende.consumers.azure;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse (
		@JsonProperty(value = "access_token", required = true)
		String access_token
){}
