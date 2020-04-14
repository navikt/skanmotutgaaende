package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    @XmlElement(required = true)
    private Journalpost journalpost;

    @XmlElement(required = true)
    private SkanningInfo skanningInfo;
}
