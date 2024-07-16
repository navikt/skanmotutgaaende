package no.nav.skanmotutgaaende.itest;

import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;

import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.ENDORSERNR_NOKKEL;
import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.FYSISK_POSTBOKS_NOKKEL;
import static no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerRequestMapper.STREKKODE_POSTBOKS_NOKKEL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LagreFildetaljerIT extends AbstractIT {

	public static final String FILPAIR_NAVN = "data_005";
	private final byte[] DUMMY_FILE = "dummyfile".getBytes();
	private final String JOURNALPOST_ID = "001";
	private final String JOURNALPOST_ID_INVALID = "002";

	@Autowired
	private LagreFildetaljerConsumer lagrefildetaljerConsumer;

	@BeforeEach
	void setUpConsumer() {
		super.setUp();
	}

	@Test
	public void shouldLagreFildetaljer() {
		LagreFildetaljerRequest request = createLagreFildetaljerRequest();
		assertDoesNotThrow(() -> lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID));
	}

	@Test
	public void shoulfFailIfInvalidRequest() {
		LagreFildetaljerRequest request = createLagreFildetaljerRequest();
		assertThrows(SkanmotutgaaendeFunctionalException.class, () -> lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID_INVALID));
	}

	private LagreFildetaljerRequest createLagreFildetaljerRequest() {
		return LagreFildetaljerRequest.builder()
				.datoMottatt(new Date())
				.batchnavn("xml_pdf_pairs_testdata.zip")
				.tilleggsopplysninger(Arrays.asList(
						Tilleggsopplysning.builder()
								.nokkel(ENDORSERNR_NOKKEL)
								.verdi("3110190003NAV743506")
								.build(),
						Tilleggsopplysning.builder()
								.nokkel(FYSISK_POSTBOKS_NOKKEL)
								.verdi("1408")
								.build(),
						Tilleggsopplysning.builder()
								.nokkel(STREKKODE_POSTBOKS_NOKKEL)
								.verdi("1408")
								.build()
				))
				.eksternReferanseId(FILPAIR_NAVN + ".pdf")
				.dokumentvarianter(Arrays.asList(
						DokumentVariant.builder()
								.filtype("pdf")
								.variantformat("ARKIV")
								.fysiskDokument(DUMMY_FILE)
								.filnavn(FILPAIR_NAVN + ".pdf")
								.build(),
						DokumentVariant.builder()
								.filtype("xml")
								.variantformat("ORIGINAL")
								.fysiskDokument(DUMMY_FILE)
								.filnavn(FILPAIR_NAVN + ".xml")
								.build()))
				.build();
	}
}
