package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;

@Service
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;
    private LagreFildetaljerRequestMapper lagreFildetaljerRequestMapper;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
        this.lagreFildetaljerRequestMapper = new LagreFildetaljerRequestMapper();
    }

    public LagreFildetaljerResponse lagreFildetaljer(LagreFildetaljerRequest request, String journalpostId) {
        return lagreFildetaljerConsumer.lagreFilDetaljer(request, journalpostId);
    }

    public LagreFildetaljerResponse lagreFildetaljer(FilepairWithMetadata filepairWithMetadata) {
        LagreFildetaljerRequest request = lagreFildetaljerRequestMapper.mapMetadataToOpprettJournalpostRequest(filepairWithMetadata);
        return lagreFildetaljer(request, filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
    }

}
