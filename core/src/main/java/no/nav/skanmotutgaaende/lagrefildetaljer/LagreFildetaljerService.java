package no.nav.skanmotutgaaende.lagrefildetaljer;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotutgaaende.domain.Filepair;
import no.nav.skanmotutgaaende.domain.Journalpost;
import no.nav.skanmotutgaaende.domain.Skanningmetadata;
import no.nav.skanmotutgaaende.exceptions.functional.AbstractSkanmotutgaaendeFunctionalException;
import no.nav.skanmotutgaaende.exceptions.technical.AbstractSkanmotutgaaendeTechnicalException;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerRequest;
import no.nav.skanmotutgaaende.lagrefildetaljer.data.LagreFildetaljerResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;

@Service
@Slf4j
public class LagreFildetaljerService {

    private LagreFildetaljerConsumer lagreFildetaljerConsumer;
    private LagreFildetaljerRequestMapper lagreFildetaljerRequestMapper;

    @Inject
    public LagreFildetaljerService(LagreFildetaljerConsumer lagreFildetaljerConsumer) {
        this.lagreFildetaljerConsumer = lagreFildetaljerConsumer;
        this.lagreFildetaljerRequestMapper = new LagreFildetaljerRequestMapper();
    }

    public Optional<LagreFildetaljerResponse> lagreFildetaljer(Optional<Skanningmetadata> skanningmetadata, Filepair filepair) {
        if (skanningmetadata.isEmpty()) {
            return Optional.empty();
        }
        String batchnavn = skanningmetadata.map(Skanningmetadata::getJournalpost).map(Journalpost::getBatchnavn).orElse(null);
        try {
            String jpid = skanningmetadata.get().getJournalpost().getJournalpostId();
            log.info("Skanmotutgaaende lagrer fildetaljer for journalpost, id={}, fil={}, batch={}", jpid, filepair.getName(), batchnavn);
            LagreFildetaljerRequest request = lagreFildetaljerRequestMapper.mapMetadataToLagreFildetaljerRequest(skanningmetadata.get(), filepair);
            LagreFildetaljerResponse response = lagreFildetaljerConsumer.lagreFilDetaljer(request, jpid);
            return Optional.of(response);

        } catch (AbstractSkanmotutgaaendeFunctionalException e) {
            log.warn("Skanmotutgaaende feilet funksjonelt med oppretting av journalpost fil={}, batch={}", filepair.getName(), batchnavn, e);
            return Optional.empty();
        } catch (AbstractSkanmotutgaaendeTechnicalException e) {
            log.warn("Skanmotutgaaende feilet teknisk med  oppretting av journalpost fil={}, batch={}", filepair.getName(), batchnavn, e);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Skanmotutgaaende feilet med ukjent feil ved oppretting av journalpost fil={}, batch={}", filepair.getName(), batchnavn, e);
            return Optional.empty();
        }
    }

}
