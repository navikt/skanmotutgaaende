package no.nav.skanmotutgaaende.itest;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.CoreConfig;
import no.nav.skanmotutgaaende.config.props.IMVaultProperties;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.consumers.azure.AzureOAuthEnabledWebClientConfig;
import no.nav.skanmotutgaaende.consumers.azure.AzureProperties;
import no.nav.skanmotutgaaende.metrics.DokCounter;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties({
		SkanmotutgaaendeProperties.class,
		IMVaultProperties.class,
		AzureProperties.class
})
@Import({TestConfig.CamelTestStartupConfig.class,
		TestConfig.SshdSftpServerConfig.class,
		AzureOAuthEnabledWebClientConfig.class,
		CoreConfig.class,
		DokCounter.class})
public class TestConfig {

	@Value("${skanmotutgaaende.slack.url}")
	private String slackUrl;

	@Bean
	@Primary
	MethodsClient slackClient(SkanmotutgaaendeProperties skanmotutgaaendeProperties) {
		var slackClient = Slack.getInstance().methods(skanmotutgaaendeProperties.getSlack().getToken());
		slackClient.setEndpointUrlPrefix(slackUrl);
		return slackClient;
	}

	@Configuration
	static class CamelTestStartupConfig {
		private final AtomicInteger sshServerStartupCounter = new AtomicInteger(0);

		// Hindre at sshserver ikke er klar før camel starter å behandle.
		@Bean
		CamelContextConfiguration contextConfiguration(SshServer sshServer) {
			return new CamelContextConfiguration() {

				@Override
				public void beforeApplicationStart(CamelContext camelContext) {
					while (!sshServer.isStarted() && sshServerStartupCounter.get() <= 5) {
						try {
							// Busy wait
							Thread.sleep(1000);
							log.info("Forsøkt å starte sshserver. retry={}", sshServerStartupCounter.getAndIncrement());
						} catch (InterruptedException e) {
							// noop
						}
					}
				}

				@Override
				public void afterApplicationStart(CamelContext camelContext) {

				}
			};
		}
	}

	@Configuration
	static class SshdSftpServerConfig {
		@Bean
		public Path sshdPath() throws IOException {
			return Files.createTempDirectory("sshd");
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		public SshServer sshServer(Path sshdPath,
								   SkanmotutgaaendeProperties skanmotutgaaendeProperties) {
			SshServer sshd = SshServer.setUpDefaultServer();
			sshd.setPort(parseInt(skanmotutgaaendeProperties.getSftp().getPort()));
			sshd.setKeyPairProvider(new ClassLoadableResourceKeyPairProvider("sftp/server_id_rsa"));
			sshd.setCommandFactory(new ScpCommandFactory());
			sshd.setSubsystemFactories(singletonList(new SftpSubsystemFactory()));
			// aksepterer alle public keys som presenteres, behøver ikke authorized_keys
			sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
			sshd.setUserAuthFactories(singletonList(new UserAuthNoneFactory()));
			sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));
			return sshd;
		}
	}
}
