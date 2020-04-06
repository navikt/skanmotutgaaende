package no.nav.skanmot1408.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    @XmlElement(required = true)
    private Journalpost journalpost;
}
