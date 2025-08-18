package no.nav.skanmotutgaaende;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraProperties.JiraServiceUser;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties.JiraConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
@Slf4j
public class CoreConfig {
	@Bean
	MethodsClient slackClient(SkanmotutgaaendeProperties skanmotutgaandeProperties) {
		return Slack.getInstance().methods(skanmotutgaandeProperties.getSlack().getToken());
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotutgaaendeProperties properties) {
		return new JiraClient(jiraProperties(properties));
	}

	public JiraProperties jiraProperties(SkanmotutgaaendeProperties properties) {
		JiraConfigProperties jira = properties.getJira();
		var xyzzy = JiraProperties.builder()
				.jiraServiceUser(new JiraServiceUser(jira.getUsername(), jira.getPassword()))
				.url(jira.getUrl())
				.build();
		log.info("url {} / user {}", xyzzy.url(), xyzzy.jiraServiceUser().username());
		return xyzzy;
	}
}
