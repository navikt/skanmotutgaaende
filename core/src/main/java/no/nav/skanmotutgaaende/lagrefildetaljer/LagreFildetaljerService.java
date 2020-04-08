package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
    }

    public LagreFildetaljerResponse lagreFildetaljer(LagreFildetaljerRequest request, String journalpostId) {
        return lagreFildetaljerConsumer.lagreFilDetaljer(request, journalpostId);
    }

    public LagreFildetaljerResponse lagreFildetaljer(FilepairWithMetadata filepairWithMetadata) {
        LagreFildetaljerRequest request = Utils.extractLagreFildetaljerRequestFromSkanningmetadata(filepairWithMetadata);
        return lagreFildetaljer(request, filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
    }

    public List<LagreFildetaljerResponse> lagreFildetaljer(List<FilepairWithMetadata> filepairWithMetadataList) {
        return filepairWithMetadataList.stream()
                .map(filepair -> lagreFildetaljer(filepair))
                .collect(Collectors.toList());
    }

}
