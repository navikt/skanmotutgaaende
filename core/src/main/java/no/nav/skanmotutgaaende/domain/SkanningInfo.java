package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SkanningInfo {


    @XmlElement(required = true)
    private String fysiskPostboks;

    @XmlElement(required = true)
    private String strekkodePostboks;
}
