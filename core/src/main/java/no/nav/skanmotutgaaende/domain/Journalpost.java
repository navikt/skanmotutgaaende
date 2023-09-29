package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = true, name = "journalpostId")
    @XmlJavaTypeAdapter(type = String.class, value = JournalpostIdAdapter.class)
    private String journalpostId;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlElement(required = true, name = "datoMottatt")
    private Date datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchnavn;

    @XmlElement(name = "filnavn")
    private String filnavn;

    @XmlElement(name = "endorsernr")
    private String endorsernr;

    @XmlElement(name = "antallSider")
    private String antallSider;
}
