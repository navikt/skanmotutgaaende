package no.nav.skanmotutgaaende.validator;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;

@Slf4j
public class SkanningmetadataValidator {

    private final JournalpostValidator journalpostValidator = new JournalpostValidator();
    private final SkanningInfoValidator skanningInfoValidator = new SkanningInfoValidator();

    public void validate(Skanningmetadata skanningmetadata) {
        verfiyMetadataIsValid(skanningmetadata);
    }

    private void verfiyMetadataIsValid(Skanningmetadata skanningmetadata) {
        if (null == skanningmetadata) {
            throw new InvalidMetadataException("Skanningmetadata is null");
        }
        verifyJournalpostIdIsValid(skanningmetadata.getJournalpost());
        verifySkanningInfoIsValid(skanningmetadata.getSkanningInfo());
    }

    private void verifyJournalpostIdIsValid(Journalpost journalpost) {
        if (null == journalpost) {
            throw new InvalidMetadataException("Journalpost is null");
        }
        if (!journalpostValidator.isValidJournalpostId(journalpost.getJournalpostId())) {
            throw new InvalidMetadataException("JournalpostId is not valid: " + journalpost.getJournalpostId());
        }
        if (!journalpostValidator.isValidMottakskanal(journalpost.getMottakskanal())) {
            throw new InvalidMetadataException("Mottakskanal is not valid: " + journalpost.getMottakskanal());
        }
        if (!journalpostValidator.isValidDatoMottatt(journalpost.getDatoMottatt())) {
            throw new InvalidMetadataException("DatoMottatt is not valid: " + journalpost.getDatoMottatt());
        }
        if (!journalpostValidator.isValidBatchNavn(journalpost.getBatchnavn())) {
            throw new InvalidMetadataException("Batchnavn is not valid: " + journalpost.getBatchnavn());
        }
    }

    private void verifySkanningInfoIsValid(SkanningInfo skanningInfo) {
        if (null == skanningInfo) {
            throw new InvalidMetadataException("SkanningInfo is null");
        }
        if (!skanningInfoValidator.isValidStrekkodePostboks(skanningInfo.getStrekkodePostboks())) {
            throw new InvalidMetadataException("Strekkodepostboks is not valid: " + skanningInfo.getStrekkodePostboks() +
                    ". Must be one of: " + SkanningInfoValidator.strekkodePostboksVerdier);
        }
    }
}
