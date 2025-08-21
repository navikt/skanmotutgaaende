package no.nav.skanmotutgaaende;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraProperties.JiraServiceUser;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotutgaaende.config.props.JiraAuthProperties;
import no.nav.skanmotutgaaende.config.props.SkanmotutgaaendeProperties;
import no.nav.skanmotutgaaende.config.props.SlackProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@ComponentScan
@Configuration
@Slf4j
public class CoreConfig {

	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

	@Bean
	MethodsClient slackClient(SlackProperties slackProperties) {
		return Slack.getInstance().methods(slackProperties.token());
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	Clock clock() {
		return Clock.system(DEFAULT_ZONE_ID);
	}

	@Bean
	public JiraClient jiraClient(SkanmotutgaaendeProperties properties, JiraAuthProperties jiraAuthProperties) {
		return new JiraClient(JiraProperties.builder()
				.jiraServiceUser(new JiraServiceUser(jiraAuthProperties.username(), jiraAuthProperties.password()))
				.url(properties.getJira().getUrl())
				.build());
	}

}
