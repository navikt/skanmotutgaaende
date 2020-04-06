package no.nav.skanmotutgaaende.zip;

import lombok.Builder;
import lombok.Data;
import no.nav.skanmotutgaaende.entities.Skanningmetadata;

@Data
@Builder
public class MetadataPdfPair {

    private final Skanningmetadata skanningmetadata;
    private final byte[] pdf;
}
