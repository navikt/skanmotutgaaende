package no.nav.skanmotutgaaende.utils;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;

import java.util.Arrays;

public class Utils {

    public static LagreFildetaljerRequest extractLagreFildetaljerRequestFromSkanningmetadata(FilepairWithMetadata filepairWithMetadata) {
        Journalpost journalpost = filepairWithMetadata.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = filepairWithMetadata.getSkanningmetadata().getSkanningInfo();
        return LagreFildetaljerRequest.builder()
                .dato(journalpost.getDatoMottatt())
                .endorsernr(journalpost.getEndorsernr())
                .mottattfra(skanningInfo.getStrekkodePostboks())
                .mottatti(skanningInfo.getFysiskPostboks())
                .batchnavn(journalpost.getBatchNavn())
                .dokumentvarianter(Arrays.asList(
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype("PDFA")
                                .variantFormat("ARKIV")
                                .fysiskDokument(filepairWithMetadata.getPdf())
                                .filnavn(journalpost.getFilNavn())
                                .build(),
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype("XML")
                                .variantFormat("SKANNING_META")
                                .fysiskDokument(filepairWithMetadata.getXml())
                                .filnavn(changeFiletypeInFilename(journalpost.getFilNavn(), "xml"))
                                .build()))
                .build();
    }

    public static String changeFiletypeInFilename(String originalName, String newFiletype) {
        String nameNoSuffix = originalName.substring(0, originalName.lastIndexOf(".") + 1);
        return nameNoSuffix + newFiletype;
    }
}
