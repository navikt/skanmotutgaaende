package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning;

import java.util.Date;
import java.util.List;

public class LagreFildetaljerRequestMapper {


    public static final String ENDORSERNR_NOKKEL = "endorsernr";
    public static final String FYSISK_POSTBOKS_NOKKEL = "fysiskPostboks";
    public static final String STREKKODE_POSTBOKS_NOKKEL = "strekkodePostboks";
    public static final String ANTALL_SIDER_NOKKEL = "antallSider";
    public static final String PDFA = "PDFA";
    public static final String XML = "XML";
    public static final String FILTYPE_XML = "xml";
    public static final String FILTYPE_PDF = "pdf";
    public static final String VARIANTFORMAT_ARKIV = "ARKIV";
    public static final String VARIANTFORMAT_SKANNING_META = "SKANNING_META";

    public LagreFildetaljerRequest mapMetadataToLagreFildetaljerRequest(Skanningmetadata skanningmetadata, Filepair filepair) {
        Journalpost journalpost = skanningmetadata.getJournalpost();
        SkanningInfo skanningInfo = skanningmetadata.getSkanningInfo();
        Date datoMotatt = journalpost.getDatoMottatt();
        String batchnavn = journalpost.getBatchnavn();
        String mottakskanal = journalpost.getMottakskanal();

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(FYSISK_POSTBOKS_NOKKEL, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODE_POSTBOKS_NOKKEL, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(ENDORSERNR_NOKKEL, journalpost.getEndorsernr()),
                new Tilleggsopplysning(ANTALL_SIDER_NOKKEL, journalpost.getAntallSider())
        );

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(PDFA)
                .variantformat(VARIANTFORMAT_ARKIV)
                .fysiskDokument(filepair.getPdf())
                .filnavn(appendFileType(filepair.getName(), FILTYPE_PDF))
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(XML)
                .variantformat(VARIANTFORMAT_SKANNING_META)
                .fysiskDokument(filepair.getXml())
                .filnavn(appendFileType(filepair.getName(), FILTYPE_XML))
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
