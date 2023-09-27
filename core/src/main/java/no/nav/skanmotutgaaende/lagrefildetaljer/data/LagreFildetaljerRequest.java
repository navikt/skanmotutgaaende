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
    private final Date datoMottatt;

    @NotNull(message = "Mottakskanal kan ikke være null")
    private final String mottakskanal;

    private final List<Tilleggsopplysning> tilleggsopplysninger;

    private final String batchnavn;

    @NotNull(message = "Dokumentvarianter kan ikke være null")
    private final List<DokumentVariant> dokumentvarianter;

}
