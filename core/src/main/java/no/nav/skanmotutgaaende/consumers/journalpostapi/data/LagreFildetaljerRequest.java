package no.nav.skanmotutgaaende.consumers.journalpostapi.data;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class LagreFildetaljerRequest {

	Date datoMottatt;

	String mottakskanal;

	List<Tilleggsopplysning> tilleggsopplysninger;

	String batchnavn;

	@NotNull(message = "Dokumentvarianter kan ikke være null")
	List<DokumentVariant> dokumentvarianter;

	String eksternReferanseId;
}
