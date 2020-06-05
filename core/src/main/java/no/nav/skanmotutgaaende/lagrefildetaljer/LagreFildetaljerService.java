package no.nav.skanmotutgaaende.lagrefildetaljer;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
@Slf4j
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;
    private LagreFildetaljerRequestMapper lagreFildetaljerRequestMapper;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
        this.lagreFildetaljerRequestMapper = new LagreFildetaljerRequestMapper();
    }

    public void lagreFildetaljer(Skanningmetadata skanningmetadata, Filepair filepair) {
        String jpid = skanningmetadata.getJournalpost().getJournalpostId();
        log.info("Skanmotutgaaende lagrer fildetaljer for journalpost, id={}, fil={}, batch={}", jpid, filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn());
        LagreFildetaljerRequest request = lagreFildetaljerRequestMapper.mapMetadataToLagreFildetaljerRequest(skanningmetadata, filepair);
        lagreFildetaljerConsumer.lagreFilDetaljer(request, jpid);

    }

}
