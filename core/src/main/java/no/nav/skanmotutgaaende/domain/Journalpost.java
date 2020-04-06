package no.nav.skanmotutgaaende.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = true)
    private long journalpostId;

    @XmlElement(required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date datoMottatt;

    @XmlElement(required = true)
    private String batchNavn;

    @XmlElement(required = true)
    private String filNavn;

    @XmlElement(required = true)
    private String endorsernr;

    @XmlElement(required = true)
    private String mottattfra;

    @XmlElement(required = true)
    private String mottatti;
}
