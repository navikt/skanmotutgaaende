package no.nav.skanmotutgaaende.leszipfil;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class LesZipfilConsumer {

    public File hentZipfil() {
        // TODO: Hent zipfil bestående av par av pdf'er og xml'er med metadata fra skyfilområde
        return new File("core/src/main/resources/tmp/__files/SKAN_NETS.zip");
    }
}
