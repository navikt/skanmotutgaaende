package no.nav.skanmotutgaaende.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {
    @Test
    public void shouldRemoveLeadingZeros() {
        String s1 = Utils.removeLeadingZeros("0002301");
        String s2 = Utils.removeLeadingZeros("1230");
        String s3 = Utils.removeLeadingZeros("0");
        assertEquals("2301", s1);
        assertEquals("1230", s2);
        assertEquals("0", s3);
    }
}
