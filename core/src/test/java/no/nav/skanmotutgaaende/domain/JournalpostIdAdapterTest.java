package no.nav.skanmotutgaaende.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalpostIdAdapterTest {
    private final JournalpostIdAdapter journalpostIdAdapter = new JournalpostIdAdapter();

    @Test
    void shouldUnmarshal() throws Exception {
        final String marshal = journalpostIdAdapter.unmarshal("4000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldUnmarshalWhenZeroesAdded() throws Exception {
        final String marshal = journalpostIdAdapter.unmarshal("0000004000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldMarshal() throws Exception {
        final String marshal = journalpostIdAdapter.marshal("4000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldMarshalWhenZeroesAdded() throws Exception {
        final String marshal = journalpostIdAdapter.marshal("0000004000000");
        assertThat(marshal).isEqualTo("4000000");
    }
}