package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostboksUtgaaendeService {
    private final LagreFildetaljerService lagreFildetaljerService;

    @Autowired
    public PostboksUtgaaendeService(LagreFildetaljerService lagreFildetaljerService) {
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    @Handler
    public void behandleForsendelse(@Body PostboksUtgaaendeEnvelope envelope) {
        final Skanningmetadata skanningmetadata = envelope.getSkanningmetadata();
        skanningmetadata.verifyFields();
        lagreFildetaljerService.lagreFildetaljer(skanningmetadata, Filepair.builder()
                .name(envelope.getFilebasename())
                .xml(envelope.getXml())
                .pdf(envelope.getPdf())
                .build());
    }
}
