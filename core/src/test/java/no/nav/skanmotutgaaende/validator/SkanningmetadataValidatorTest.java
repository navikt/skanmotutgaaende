package no.nav.skanmotutgaaende.validator;

import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotutgaaende.validator.SkanningmetadataValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

public class SkanningmetadataValidatorTest {

    private final SkanningmetadataValidator skanningmetadataValidator = new SkanningmetadataValidator();

    private final String JPID_VALID = "123456789";
    private final String JPID_INVALID = "ugyldig id";
    private final String FILE_NAME_VALID = "filtest.zip";
    private final String FILE_NAME_INVALID = "ikkeEtZipFilNavn";


    @Test
    public void shouldValidateSkanningmetadata() {
        assertSkanningmetadataIsValid(getSkanningmetadata(JPID_VALID, FILE_NAME_VALID));
    }

    @Test
    public void shouldNotValidateInvalidJournalpostId() {
        assertThrows(InvalidMetadataException.class, () -> skanningmetadataValidator.validate(getSkanningmetadata(JPID_INVALID, FILE_NAME_VALID)));
    }

    @Test
    public void shouldNotValidateInvalidFileName() {
        assertThrows(InvalidMetadataException.class, () -> skanningmetadataValidator.validate(getSkanningmetadata(JPID_VALID, FILE_NAME_INVALID)));
    }

    @Test
    public void shouldNotValidateMissingSkanningInfo() {
        assertThrows(InvalidMetadataException.class, () ->
                skanningmetadataValidator.validate(Skanningmetadata.builder()
                        .journalpost(Journalpost.builder()
                                .journalpostId(JPID_VALID)
                                .mottakskanal("SKAN_IM")
                                .datoMottatt(new Date())
                                .batchnavn("test.zip")
                                .filnavn(FILE_NAME_VALID)
                                .endorsernr("00001111NAV22")
                                .build())
                        .build()));
    }

    private void assertSkanningmetadataIsValid(Skanningmetadata skanningmetadata) {
        skanningmetadataValidator.validate(skanningmetadata);
    }

    private Skanningmetadata getSkanningmetadata(String journalpostId, String filNavn) {
        return Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .journalpostId(journalpostId)
                        .mottakskanal("SKAN_IM")
                        .datoMottatt(new Date())
                        .batchnavn("test.zip")
                        .filnavn(filNavn)
                        .endorsernr("00001111NAV22")
                        .build())
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks("1408")
                        .strekkodePostboks("1408")
                        .build())
                .build();
    }
}
