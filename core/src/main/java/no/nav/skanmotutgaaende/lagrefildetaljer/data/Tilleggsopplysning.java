package no.nav.skanmotutgaaende.lagrefildetaljer.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {

    private final String nokkel;

    private final String verdi;
}
