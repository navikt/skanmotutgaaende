package no.nav.skanmotutgaaende.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetadataPdfPair {

    private final Skanningmetadata skanningmetadata;
    private final byte[] pdf;
}
