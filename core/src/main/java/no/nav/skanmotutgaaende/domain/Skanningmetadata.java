package no.nav.skanmotutgaaende.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.validator.SkanningmetadataValidator;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    SkanningmetadataValidator skanningmetadataValidator = new SkanningmetadataValidator();

    public void verifyFields() throws InvalidMetadataException {
        skanningmetadataValidator.validate(this);
    }

    @XmlElement(required = true)
    private Journalpost journalpost;

    @XmlElement(required = true)
    private SkanningInfo skanningInfo;
}
