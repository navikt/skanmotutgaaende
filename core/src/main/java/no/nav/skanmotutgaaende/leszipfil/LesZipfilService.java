package no.nav.skanmotutgaaende.leszipfil;

import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;

@Service
public class LesZipfilService {

    private LesZipfilConsumer lesZipfilConsumer;

    @Inject
    public LesZipfilService(LesZipfilConsumer lesZipfilConsumer) {
        this.lesZipfilConsumer = lesZipfilConsumer;
    }

    public File lesZipFil() {
        return lesZipfilConsumer.hentZipFil();
    }
}
