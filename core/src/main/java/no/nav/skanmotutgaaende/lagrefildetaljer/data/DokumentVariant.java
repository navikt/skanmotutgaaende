package no.nav.skanmotutgaaende.lagrefildetaljer.data;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class DokumentVariant {
    @NotNull(message = "Filtype kan ikke være null")
    String filtype;

    @NotNull(message = "Variantformat kan ikke være null")
    String variantformat;

    @NotNull(message = "Fysisk dokument kan ikke være null")
    byte[] fysiskDokument;

    String filnavn;
}
