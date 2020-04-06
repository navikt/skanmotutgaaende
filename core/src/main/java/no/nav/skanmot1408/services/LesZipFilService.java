package no.nav.skanmot1408.services;

import no.nav.skanmot1408.consumers.LesZipfilConsumer;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
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
