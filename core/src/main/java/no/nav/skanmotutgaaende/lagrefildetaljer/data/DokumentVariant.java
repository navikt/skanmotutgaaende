package no.nav.skanmotutgaaende.lagrefildetaljer.data;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class DokumentVariant {
    @NotNull(message = "Filtype kan ikke være null")
    private final String filtype;

    @NotNull(message = "Variantformat kan ikke være null")
    private final String variantformat;

    @NotNull(message = "Fysisk dokument kan ikke være null")
    private final byte[] fysiskDokument;

    private final String filnavn;
}
