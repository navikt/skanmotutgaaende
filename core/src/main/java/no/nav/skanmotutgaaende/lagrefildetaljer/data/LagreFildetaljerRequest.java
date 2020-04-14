package no.nav.skanmotutgaaende.lagrefildetaljer.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class LagreFildetaljerRequest {

    public static final String ENDORSER_NR = "endorsernr";
    public static final String FYSISK_POSTBOKS = "fysiskPostboks";
    public static final String STREKKODE_POSTBOKS = "strekkodePostboks";
    public static final String FILTYPE_PDFA = "PDFA";
    public static final String FILTYPE_XML = "XML";
    public static final String VARIANTFORMAT_ARKIV = "ARKIV";
    public static final String VARIANTFORMAT_SKANNING_META = "SKANNING_META";


    @JsonFormat(pattern="yyyy-MM-dd")
    private final Date datoMottatt;

    private final String batchnavn;

    @NotNull(message = "Mottakskanal kan ikke være null")
    private final String mottakskanal;

    private final List<Tilleggsopplysninger> tilleggsopplysninger;

    @NotNull(message = "Dokumentvarianter kan ikke være null")
    private final List<Dokumentvariant> dokumentvarianter;

    @Value
    @Builder
    public static class Dokumentvariant {
        @NotNull(message = "Filtype kan ikke være null")
        private final String filtype;

        @NotNull(message = "Variantformat kan ikke være null")
        private final String variantformat;

        @NotNull(message = "Fysisk dokument kan ikke være null")
        private final byte[] fysiskDokument;

        private final String filnavn;
    }

    @Value
    @Builder
    public static class Tilleggsopplysninger {

        private final String nokkel;

        private final String verdi;
    }
}
