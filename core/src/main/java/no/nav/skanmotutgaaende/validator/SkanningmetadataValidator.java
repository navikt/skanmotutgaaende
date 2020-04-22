package no.nav.skanmotutgaaende.validator;

import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;

public class SkanningmetadataValidator {

    private JournalpostValidator journalpostValidator = new JournalpostValidator();
    private SkanningInfoValidator skanningInfoValidator = new SkanningInfoValidator();

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
        if (!journalpostValidator.isValidBatchNavn(journalpost.getBatchNavn())) {
            throw new InvalidMetadataException("Batchnavn is not valid: " + journalpost.getBatchNavn());
        }
        if (!journalpostValidator.isValidFilnavn(journalpost.getFilNavn())) {
            throw new InvalidMetadataException("Filnavn is not valid: " + journalpost.getFilNavn());
        }
        if (!journalpostValidator.isValidEndorsernr(journalpost.getEndorsernr())) {
            throw new InvalidMetadataException("Endorsernr is not valid: " + journalpost.getEndorsernr());
        }
    }

    private void verifySkanningInfoIsValid(SkanningInfo skanningInfo) {
        if (null == skanningInfo) {
            throw new InvalidMetadataException("SkanningInfo is null");
        }
        if (!skanningInfoValidator.isValidFysiskPostboks(skanningInfo.getFysiskPostboks())) {
            throw new InvalidMetadataException("FysiskPostboks is not valid: " + skanningInfo.getFysiskPostboks());
        }
        if (!skanningInfoValidator.isValidStrekkodePostboks(skanningInfo.getStrekkodePostboks())) {
            throw new   InvalidMetadataException("StrekkodePostboks is not valid: " + skanningInfo.getStrekkodePostboks());
        }
    }
}
