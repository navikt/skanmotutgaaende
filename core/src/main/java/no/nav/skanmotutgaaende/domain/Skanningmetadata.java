package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    @XmlElement(required = true)
    private Journalpost journalpost;
}
