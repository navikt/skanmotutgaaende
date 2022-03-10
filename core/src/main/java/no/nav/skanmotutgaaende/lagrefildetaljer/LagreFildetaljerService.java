package no.nav.skanmotutgaaende.lagrefildetaljer;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LagreFildetaljerService {

    private final LagreFildetaljerConsumer lagreFildetaljerConsumer;
    private final LagreFildetaljerRequestMapper lagreFildetaljerRequestMapper;

    @Autowired
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
        this.lagreFildetaljerRequestMapper = new LagreFildetaljerRequestMapper();
    }

    public void lagreFildetaljer(final Skanningmetadata skanningmetadata, final Filepair filepair) {
        String journalpostId = skanningmetadata.getJournalpost().getJournalpostId();
        log.info("Skanmotutgaaende forsøker å lagre fildetaljer for journalpost. journalpostId={}, fil={}, batch={}", journalpostId, filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn());
        LagreFildetaljerRequest request = lagreFildetaljerRequestMapper.mapMetadataToLagreFildetaljerRequest(skanningmetadata, filepair);
        lagreFildetaljerConsumer.lagreFilDetaljer(request, journalpostId);
        log.info("Skanmotutgaaende lagret fildetaljer for journalpost. journalpostId={}, fil={}, batch={}", journalpostId, filepair.getName(), skanningmetadata.getJournalpost().getBatchnavn());
    }
}
