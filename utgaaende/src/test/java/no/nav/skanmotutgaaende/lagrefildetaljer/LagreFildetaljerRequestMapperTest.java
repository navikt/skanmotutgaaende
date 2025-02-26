package no.nav.skanmotutgaaende.lagrefildetaljer;

import no.nav.skanmotutgaaende.consumers.journalpostapi.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.consumers.journalpostapi.data.Tilleggsopplysning;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.SkanningInfo;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.mapper.LagreFildetaljerRequestMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class LagreFildetaljerRequestMapperTest {

	private final String JOURNALPOSTID = "11111111";
	private final String MOTTAKSKANAL = "SKAN_IM";
	private final String BATCHNAVN = "navnPaaBatch.zip";
	private final String FILNAVN_I_XML = "abc.pdf";
	private final String FILNAVN = "filnavn";
	private final String FILNAVN_PDF = "filnavn.pdf";
	private final String FILNAVN_XML = "filnavn.xml";
	private final String STREKKODE_POSTBOKS = "1004";
	private final byte[] DUMMY_FILE = "dummyfile".getBytes();

	private final LagreFildetaljerRequestMapper lagreFildetaljerRequestMapper = new LagreFildetaljerRequestMapper();

	@Test
	public void shouldMapSkanningmetadataToLagreFildetaljerRequest() {
		String ENDORSERNR = "222111NAV456";
		String FYSISK_POSTBOKS = "1002";
		LagreFildetaljerRequest lagreFildetaljerRequest = lagreFildetaljerRequestMapper.mapMetadataToLagreFildetaljerRequest(
				Skanningmetadata.builder()
						.journalpost(Journalpost.builder()
								.journalpostId(JOURNALPOSTID)
								.mottakskanal(MOTTAKSKANAL)
								.datoMottatt(new Date())
								.batchnavn(BATCHNAVN)
								.filnavn(FILNAVN_I_XML)
								.endorsernr(ENDORSERNR)
								.build())
						.skanningInfo(SkanningInfo.builder()
								.fysiskPostboks(FYSISK_POSTBOKS)
								.strekkodePostboks(STREKKODE_POSTBOKS)
								.build())
						.build(),
				Filepair.builder()
						.name(FILNAVN)
						.pdf(DUMMY_FILE)
						.xml(DUMMY_FILE)
						.build()
		);

		Assertions.assertEquals(BATCHNAVN, lagreFildetaljerRequest.getBatchnavn());
		Assertions.assertEquals(MOTTAKSKANAL, lagreFildetaljerRequest.getMottakskanal());
		assertEquals(FYSISK_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequestMapper.FYSISK_POSTBOKS_NOKKEL));
		assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequestMapper.STREKKODE_POSTBOKS_NOKKEL));
		assertEquals(ENDORSERNR, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), LagreFildetaljerRequestMapper.ENDORSERNR_NOKKEL));
		Assertions.assertEquals(FILNAVN_PDF, lagreFildetaljerRequest.getEksternReferanseId());

		AtomicInteger pdfCounter = new AtomicInteger();
		AtomicInteger xmlCounter = new AtomicInteger();
		lagreFildetaljerRequest.getDokumentvarianter().forEach(dokumentVariant -> {
			switch (dokumentVariant.getFiltype()) {
				case "PDFA" -> {
					pdfCounter.getAndIncrement();
					Assertions.assertEquals(FILNAVN_PDF, dokumentVariant.getFilnavn());
					Assertions.assertEquals("ARKIV", dokumentVariant.getVariantformat());
					Assertions.assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
				}
				case "XML" -> {
					xmlCounter.getAndIncrement();
					Assertions.assertEquals(FILNAVN_XML, dokumentVariant.getFilnavn());
					Assertions.assertEquals("SKANNING_META", dokumentVariant.getVariantformat());
					Assertions.assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
				}
				default -> fail();
			}
		});
		assertEquals(1, pdfCounter.get());
		assertEquals(1, xmlCounter.get());
	}

	@Test
	public void shouldFilterEmptyTilleggsopplysninger() {
		LagreFildetaljerRequest lagreFildetaljerRequest = lagreFildetaljerRequestMapper.mapMetadataToLagreFildetaljerRequest(
				Skanningmetadata.builder()
						.journalpost(Journalpost.builder()
								.journalpostId(JOURNALPOSTID)
								.mottakskanal(MOTTAKSKANAL)
								.datoMottatt(new Date())
								.batchnavn(BATCHNAVN)
								.filnavn(FILNAVN_I_XML)
								.endorsernr(null)
								.build())
						.skanningInfo(SkanningInfo.builder()
								.fysiskPostboks("")
								.strekkodePostboks(STREKKODE_POSTBOKS)
								.build())
						.build(),
				Filepair.builder()
						.name(FILNAVN)
						.pdf(DUMMY_FILE)
						.xml(DUMMY_FILE)
						.build()
		);
		Assertions.assertEquals(1, lagreFildetaljerRequest.getTilleggsopplysninger().size());
		assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), "strekkodePostboks"));
		Assertions.assertEquals(FILNAVN_PDF, lagreFildetaljerRequest.getEksternReferanseId());
		assertThrows(NoSuchElementException.class, () -> getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), "endorsernr"));
		assertThrows(NoSuchElementException.class, () -> getTillegsopplysningerVerdiFromNokkel(lagreFildetaljerRequest.getTilleggsopplysninger(), "fysiskPostboks"));
	}

	private String getTillegsopplysningerVerdiFromNokkel(List<Tilleggsopplysning> tilleggsopplysninger, String nokkel) {
		return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
	}

}
