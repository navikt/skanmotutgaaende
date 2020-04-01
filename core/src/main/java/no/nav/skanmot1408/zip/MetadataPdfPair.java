package no.nav.skanmot1408.zip;

import lombok.Builder;
import lombok.Data;
import no.nav.skanmot1408.entities.Skanningmetadata;

@Data
@Builder
public class MetadataPdfPair {

    private final Skanningmetadata skanningmetadata;
    private final byte[] pdf;
}
