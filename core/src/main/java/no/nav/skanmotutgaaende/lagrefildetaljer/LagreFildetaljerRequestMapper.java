package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning;

import java.util.Date;
import java.util.List;

import static no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning.ANTALL_SIDER;
import static no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning.ENDORSER_NR;
import static no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning.FYSISK_POSTBOKS;
import static no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning.STREKKODE_POSTBOKS;

public class LagreFildetaljerRequestMapper {

    public static final String PDFA = "PDFA";
    public static final String XML = "XML";
    public static final String FILTYPE_XML = "xml";
    public static final String FILTYPE_PDF = "pdf";
    public static final String VARIANTFORMAT_ARKIV = "ARKIV";
    public static final String VARIANTFORMAT_SKANNING_META = "SKANNING_META";

    public LagreFildetaljerRequest mapMetadataToOpprettJournalpostRequest(FilepairWithMetadata filepairWithMetadata) {
        Journalpost journalpost = filepairWithMetadata.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = filepairWithMetadata.getSkanningmetadata().getSkanningInfo();
        Date datoMotatt = journalpost.getDatoMottatt();
        String batchnavn = journalpost.getBatchnavn();
        String mottakskanal = journalpost.getMottakskanal();

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(FYSISK_POSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODE_POSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(ENDORSER_NR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(ANTALL_SIDER, journalpost.getAntallSider())
        );

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(PDFA)
                .variantformat(VARIANTFORMAT_ARKIV)
                .fysiskDokument(filepairWithMetadata.getPdf())
                .filnavn(appendFileType(filepairWithMetadata.getName(), FILTYPE_PDF))
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(XML)
                .variantformat(VARIANTFORMAT_SKANNING_META)
                .fysiskDokument(filepairWithMetadata.getXml())
                .filnavn(appendFileType(filepairWithMetadata.getName(), FILTYPE_XML))
                .build();


        return LagreFildetaljerRequest.builder()
                .datoMottatt(datoMotatt)
                .mottakskanal(mottakskanal)
                .tilleggsopplysninger(tilleggsopplysninger)
                .batchnavn(batchnavn)
                .dokumentvarianter(List.of(pdf, xml))
                .build();
    }

    private static String appendFileType(String filename, String filetype) {
        return filename + "." + filetype;
    }
}
