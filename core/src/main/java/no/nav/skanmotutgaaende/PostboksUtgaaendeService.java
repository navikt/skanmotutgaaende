package no.nav.skanmotutgaaende;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerService;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksUtgaaendeService {
    private final LagreFildetaljerService lagreFildetaljerService;

    @Inject
    public PostboksUtgaaendeService(LagreFildetaljerService lagreFildetaljerService) {
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    public Exchange openWithPassword(Exchange exchange){
        String zipPassword = "password";

        exchange.getIn().setBody(new ZipInputStream(exchange.getIn().getBody(InputStream.class), zipPassword.toCharArray()));
        return exchange;
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
