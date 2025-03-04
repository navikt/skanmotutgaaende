package no.nav.skanmotutgaaende.consumers.journalpostapi.data;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DokumentVariant {
    @NotBlank(message = "Filtype kan ikke være null")
    String filtype;

    @NotBlank(message = "Variantformat kan ikke være null")
    String variantformat;

	@NotNull(message = "Fysisk dokument kan ikke være null")
	byte[] fysiskDokument;

	String filnavn;
}
