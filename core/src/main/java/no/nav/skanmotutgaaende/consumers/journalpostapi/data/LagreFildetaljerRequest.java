package no.nav.skanmotutgaaende.consumers.journalpostapi.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class LagreFildetaljerRequest {

	@JsonFormat(pattern = "yyyy-MM-dd")
	Date datoMottatt;

	@NotBlank(message = "Mottakskanal kan ikke være null")
	String mottakskanal;

	List<Tilleggsopplysning> tilleggsopplysninger;

	String batchnavn;

	@NotNull(message = "Dokumentvarianter kan ikke være null")
	List<DokumentVariant> dokumentvarianter;

	String eksternReferanseId;
}
