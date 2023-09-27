package no.nav.skanmotutgaaende.config.props;

import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties("https")
public record WebProxyProperties(String proxyHost, int proxyPort) {

	public Optional<HttpHost> getProxy() {
		return Optional.ofNullable(proxyHost)
				.map(spec -> new HttpHost(proxyHost, proxyPort));
	}
}
