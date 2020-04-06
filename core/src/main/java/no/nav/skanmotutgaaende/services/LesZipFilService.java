package no.nav.skanmotutgaaende.services;

import no.nav.skanmotutgaaende.consumers.LesZipfilConsumer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;

@Service
public class LesZipFilService {

    private LesZipfilConsumer lesZipfilConsumer;

    @Inject
    public LesZipFilService(LesZipfilConsumer lesZipfilConsumer) {
        this.lesZipfilConsumer = lesZipfilConsumer;
    }

    public File lesZipFil() {
        return lesZipfilConsumer.hentZipFil();
    }
}
