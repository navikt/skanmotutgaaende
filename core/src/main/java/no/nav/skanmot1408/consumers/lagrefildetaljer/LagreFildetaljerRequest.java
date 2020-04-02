package no.nav.skanmot1408.consumers.lagrefildetaljer;

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

    @JsonFormat(pattern="yyyy-MM-dd")
    private final Date dato;

    private final String endorsernr;

    private final String mottattfra;

    private final String mottatti;

    private final String batchnavn;

    @NotNull(message = "Dokumentvarianter kan ikke være null")
    private final List<Dokumentvariant> dokumentvarianter;

    @Value
    @Builder
    public static class Dokumentvariant {
        @NotNull(message = "Filtype kan ikke være null")
        private final String filtype;

        @NotNull(message = "Variantformat kan ikke være null")
        private final String variantFormat;

        @NotNull(message = "Fysisk dokument kan ikke være null")
        private final byte[] fysiskDokument;

        private final String filnavn;
    }
}
