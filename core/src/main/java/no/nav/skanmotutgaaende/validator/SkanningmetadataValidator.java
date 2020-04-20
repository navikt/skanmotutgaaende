package no.nav.skanmotutgaaende.validator;

import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.InvalidMetadataException;

public class SkanningmetadataValidator {

    private SkanningmetadataValidator() {

    }

    public static void validate(Skanningmetadata skanningmetadata) {
        verfiyMetadataIsValid(skanningmetadata);
    }

    private static void verfiyMetadataIsValid(Skanningmetadata skanningmetadata) {
        if (null == skanningmetadata) {
            throw new InvalidMetadataException("Skanningmetadata is null");
        }
        verifyJournalpostIdIsValid(skanningmetadata.getJournalpost());
        verifySkanningInfoIsValid(skanningmetadata.getSkanningInfo());
    }

    private static void verifyJournalpostIdIsValid(Journalpost journalpost) {
        if (null == journalpost) {
            throw new InvalidMetadataException("Journalpost is null");
        }
        if (!JournalpostValidator.isValidJournalpostId(journalpost.getJournalpostId())) {
            throw new InvalidMetadataException("JournalpostId is not valid: " + journalpost.getJournalpostId());
        }
        if (!JournalpostValidator.isValidMottakskanal(journalpost.getMottakskanal())) {
            throw new InvalidMetadataException("Mottakskanal is not valid: " + journalpost.getMottakskanal());
        }
        if (!JournalpostValidator.isValidDatoMottatt(journalpost.getDatoMottatt())) {
            throw new InvalidMetadataException("DatoMottatt is not valid: " + journalpost.getDatoMottatt());
        }
        if (!JournalpostValidator.isValidBatchNavn(journalpost.getBatchNavn())) {
            throw new InvalidMetadataException("Batchnavn is not valid: " + journalpost.getBatchNavn());
        }
        if (!JournalpostValidator.isValidFilnavn(journalpost.getFilNavn())) {
            throw new InvalidMetadataException("Filnavn is not valid: " + journalpost.getFilNavn());
        }
        if (!JournalpostValidator.isValidEndorsernr(journalpost.getEndorsernr())) {
            throw new InvalidMetadataException("Endorsernr is not valid: " + journalpost.getEndorsernr());
        }
    }

    private static void verifySkanningInfoIsValid(SkanningInfo skanningInfo) {
        if (null == skanningInfo) {
            throw new InvalidMetadataException("SkanningInfo is null");
        }
        if (!SkanningInfoValidator.isValidFysiskPostboks(skanningInfo.getFysiskPostboks())) {
            throw new InvalidMetadataException("FysiskPostboks is not valid: " + skanningInfo.getFysiskPostboks());
        }
        if (!SkanningInfoValidator.isValidStrekkodePostboks(skanningInfo.getStrekkodePostboks())) {
            throw new   InvalidMetadataException("StrekkodePostboks is not valid: " + skanningInfo.getStrekkodePostboks());
        }
    }
}
