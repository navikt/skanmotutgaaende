package no.nav.skanmotutgaaende.itest;

import no.nav.skanmotutgaaende.exceptions.functional.SkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.LagreFildetaljerConsumer;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.Tilleggsopplysning;
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

	private static final String FILEPAIR_NAME = "data_005";
	private static final byte[] DUMMY_FILE = "dummyfile".getBytes();
	private static final String JOURNALPOST_ID = "4000004";
	private static final String JOURNALPOST_ID_INVALID = "4000005";

	@Autowired
	private LagreFildetaljerConsumer lagrefildetaljerConsumer;

	@Test
	public void shouldLagreFildetaljer() {
		setUpHappyStubs();
		LagreFildetaljerRequest request = createLagreFildetaljerRequest();
		assertDoesNotThrow(() -> lagrefildetaljerConsumer.lagreFilDetaljer(request, JOURNALPOST_ID));
	}

	@Test
	public void shoulfFailIfInvalidRequest() {
		setUpBadStubs();
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
				.eksternReferanseId(FILEPAIR_NAME + ".pdf")
				.dokumentvarianter(Arrays.asList(
						DokumentVariant.builder()
								.filtype("pdf")
								.variantformat("ARKIV")
								.fysiskDokument(DUMMY_FILE)
								.filnavn(FILEPAIR_NAME + ".pdf")
								.build(),
						DokumentVariant.builder()
								.filtype("xml")
								.variantformat("ORIGINAL")
								.fysiskDokument(DUMMY_FILE)
								.filnavn(FILEPAIR_NAME + ".xml")
								.build()))
				.build();
	}
}
