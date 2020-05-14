package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.domain.FilepairWithMetadata;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotutgaaende.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;

@Service
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
    }

    public LagreFildetaljerResponse lagreFildetaljer(LagreFildetaljerRequest request, String journalpostId) {
        return lagreFildetaljerConsumer.lagreFilDetaljer(request, journalpostId);
    }

    public LagreFildetaljerResponse lagreFildetaljer(FilepairWithMetadata filepairWithMetadata) {
        LagreFildetaljerRequest request = extractLagreFildetaljerRequestFromSkanningmetadata(filepairWithMetadata);
        return lagreFildetaljer(request, filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
    }

    public static LagreFildetaljerRequest extractLagreFildetaljerRequestFromSkanningmetadata(FilepairWithMetadata filepairWithMetadata) {
        Journalpost journalpost = filepairWithMetadata.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = filepairWithMetadata.getSkanningmetadata().getSkanningInfo();
        return LagreFildetaljerRequest.builder()
                .datoMottatt(journalpost.getDatoMottatt())
                .batchnavn(journalpost.getBatchNavn())
                .mottakskanal(journalpost.getMottakskanal())
                .tilleggsopplysninger(Arrays.asList(
                        LagreFildetaljerRequest.Tilleggsopplysninger.builder()
                                .nokkel(LagreFildetaljerRequest.FYSISK_POSTBOKS)
                                .verdi(skanningInfo.getFysiskPostboks())
                                .build(),
                        LagreFildetaljerRequest.Tilleggsopplysninger.builder()
                                .nokkel(LagreFildetaljerRequest.STREKKODE_POSTBOKS)
                                .verdi(skanningInfo.getStrekkodePostboks())
                                .build(),
                        LagreFildetaljerRequest.Tilleggsopplysninger.builder()
                                .nokkel(LagreFildetaljerRequest.ENDORSER_NR)
                                .verdi(journalpost.getEndorsernr())
                                .build(),
                        LagreFildetaljerRequest.Tilleggsopplysninger.builder()
                                .nokkel(LagreFildetaljerRequest.ANTALL_SIDER)
                                .verdi(journalpost.getAntallSider())
                                .build()
                ))
                .dokumentvarianter(Arrays.asList(
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype(LagreFildetaljerRequest.FILTYPE_PDFA)
                                .variantformat(LagreFildetaljerRequest.VARIANTFORMAT_ARKIV)
                                .fysiskDokument(filepairWithMetadata.getPdf())
                                .filnavn(journalpost.getFilNavn())
                                .build(),
                        LagreFildetaljerRequest.Dokumentvariant.builder()
                                .filtype(LagreFildetaljerRequest.FILTYPE_XML)
                                .variantformat(LagreFildetaljerRequest.VARIANTFORMAT_SKANNING_META)
                                .fysiskDokument(filepairWithMetadata.getXml())
                                .filnavn(Utils.changeFiletypeInFilename(journalpost.getFilNavn(), "xml"))
                                .build()))
                .build();
    }

}
