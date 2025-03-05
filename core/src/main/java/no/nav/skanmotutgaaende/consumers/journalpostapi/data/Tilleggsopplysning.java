package no.nav.skanmotutgaaende.consumers.journalpostapi.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {
	String nokkel;
	String verdi;
}
