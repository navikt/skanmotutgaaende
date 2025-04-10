package no.nav.skanmotutgaaende;

import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class CoreConfig {
	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotutgaaendeProperties properties) {
		return new JiraClient(jiraProperties(properties));
	}

	public JiraProperties jiraProperties(SkanmotutgaaendeProperties properties) {
		SkanmotutgaaendeProperties.JiraConfigProperties jira = properties.getJira();
		return JiraProperties.builder()
				.jiraServiceUser(new JiraProperties.JiraServiceUser(jira.getUsername(), jira.getPassword()))
				.url(jira.getUrl())
				.build();
	}
}
