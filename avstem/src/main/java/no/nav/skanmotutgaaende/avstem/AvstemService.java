package no.nav.skanmotutgaaende.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.consumers.journalpostapi.JournalpostConsumer;
import no.nav.skanmotutgaaende.consumers.journalpostapi.data.AvstemmingReferanser;
import no.nav.skanmotutgaaende.consumers.journalpostapi.data.FeilendeAvstemmingReferanser;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotutgaaende.jira.OpprettJiraService.prettifySummary;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class AvstemService {

	public static final String AVSTEMMINGSRAPPORT = "Skanmotutgaaende avstemmingsrapport:";

	private final JournalpostConsumer journalpostConsumer;

	public AvstemService(JournalpostConsumer journalpostConsumer) {
		this.journalpostConsumer = journalpostConsumer;
	}

	@Handler
	public Set<String> avstemmingsReferanser(Set<String> avstemReferenser) {
		if (isEmpty(avstemReferenser)) {
			return Set.of();
		}

		FeilendeAvstemmingReferanser feilendeAvstemmingReferanser = journalpostConsumer.avstemReferanser(new AvstemmingReferanser(avstemReferenser));
		if (feilendeAvstemmingReferanser == null || isEmpty(feilendeAvstemmingReferanser.referanserIkkeFunnet())) {
			log.info(prettifySummary("Skanmotutgaaende avstemmingsrapport:", avstemReferenser.size(), 0));
			return null;
		}
		Set<String> referanserIkkeFunnet = feilendeAvstemmingReferanser.referanserIkkeFunnet();
		log.info(prettifySummary(AVSTEMMINGSRAPPORT, avstemReferenser.size(), referanserIkkeFunnet.size()));
		return feilendeAvstemmingReferanser.referanserIkkeFunnet();
	}
}
