package no.nav.skanmotutgaaende.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = true, name = "journalpostId")
    private String journalpostId;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlElement(required = true, name = "datoMottatt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchnavn;

    @XmlElement(required = false, name = "filnavn")
    private String filnavn;

    @XmlElement(required = false, name = "endorsernr")
    private String endorsernr;

    @XmlElement(required = false, name = "antallSider")
    private String antallSider;
}
