package no.nav.skanmotutgaaende.consumers.journalpostapi.data;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DokumentVariant {
	String filtype;
	String variantformat;
	byte[] fysiskDokument;
	String filnavn;
}
