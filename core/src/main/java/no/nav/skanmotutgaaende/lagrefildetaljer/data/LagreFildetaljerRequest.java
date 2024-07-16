package no.nav.skanmotutgaaende.lagrefildetaljer.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class LagreFildetaljerRequest {

    @JsonFormat(pattern="yyyy-MM-dd")
    Date datoMottatt;

    @NotNull(message = "Mottakskanal kan ikke være null")
    String mottakskanal;

    List<Tilleggsopplysning> tilleggsopplysninger;

    String batchnavn;

    @NotNull(message = "Dokumentvarianter kan ikke være null")
    List<DokumentVariant> dokumentvarianter;

    String eksternReferanseId;
}
