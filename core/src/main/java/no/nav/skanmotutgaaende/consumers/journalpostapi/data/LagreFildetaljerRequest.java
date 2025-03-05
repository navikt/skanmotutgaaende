package no.nav.skanmotutgaaende.consumers.journalpostapi.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class LagreFildetaljerRequest {

	@JsonFormat(pattern = "yyyy-MM-dd")
	Date datoMottatt;

	String mottakskanal;

	List<Tilleggsopplysning> tilleggsopplysninger;

	String batchnavn;

	List<DokumentVariant> dokumentvarianter;

	String eksternReferanseId;
}
