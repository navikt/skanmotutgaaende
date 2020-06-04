package no.nav.skanmotutgaaende.lagrefildetaljer.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {

    public static final String ENDORSER_NR = "endorsernr";
    public static final String FYSISK_POSTBOKS = "fysiskPostboks";
    public static final String STREKKODE_POSTBOKS = "strekkodePostboks";
    public static final String ANTALL_SIDER = "antallSider";

    private final String nokkel;

    private final String verdi;
}
