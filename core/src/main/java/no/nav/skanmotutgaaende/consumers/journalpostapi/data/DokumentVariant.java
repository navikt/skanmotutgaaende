package no.nav.skanmotutgaaende.consumers.journalpostapi.data;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;


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
