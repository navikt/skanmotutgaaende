package no.nav.skanmotutgaaende.services;

import no.nav.skanmotutgaaende.consumers.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.consumers.lagrefildetaljer.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.consumers.lagrefildetaljer.LagreFildetaljerResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
    }

    public LagreFildetaljerResponse lagreFilDetaljer(LagreFildetaljerRequest request, String journalpostId) {
        return lagreFildetaljerConsumer.lagreFilDetaljer(request, journalpostId);
    }
}
