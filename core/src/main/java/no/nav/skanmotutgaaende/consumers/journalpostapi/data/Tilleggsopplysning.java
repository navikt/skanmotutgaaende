package no.nav.skanmotutgaaende.consumers.journalpostapi.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Tilleggsopplysning {
	String nokkel;
	String verdi;
}
