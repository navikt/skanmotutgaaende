package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.validator.SkanningmetadataValidator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    public void verifyFields() throws InvalidMetadataException {
        SkanningmetadataValidator.validate(this);
    }

    @XmlElement(required = true)
    private Journalpost journalpost;

    @XmlElement(required = true)
    private SkanningInfo skanningInfo;
}
