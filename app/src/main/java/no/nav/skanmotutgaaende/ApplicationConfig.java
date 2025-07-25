package no.nav.skanmotutgaaende;

import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.consumers.azure.AzureProperties;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties({
		SkanmotutgaaendeProperties.class,
		AzureProperties.class
})
@Configuration
@ComponentScan
public class ApplicationConfig {

	@Bean
	public ClientHttpRequestFactory azureTokenHttpRequestFactory() {

		var readTimeout = SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(20)).build();
		var connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultSocketConfig(readTimeout);

		var httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.useSystemProperties()
				.build();

		var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		clientHttpRequestFactory.setConnectTimeout(3_000);

		return clientHttpRequestFactory;
	}

}
