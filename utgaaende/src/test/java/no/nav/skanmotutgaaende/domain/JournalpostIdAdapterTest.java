package no.nav.skanmotutgaaende.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalpostIdAdapterTest {
    private final JournalpostIdAdapter journalpostIdAdapter = new JournalpostIdAdapter();

    @Test
    void shouldUnmarshal() {
        final String marshal = journalpostIdAdapter.unmarshal("4000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldUnmarshalWhenZeroesAdded() {
        final String marshal = journalpostIdAdapter.unmarshal("0000004000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldMarshal() {
        final String marshal = journalpostIdAdapter.marshal("4000000");
        assertThat(marshal).isEqualTo("4000000");
    }

    @Test
    void shouldMarshalWhenZeroesAdded() {
        final String marshal = journalpostIdAdapter.marshal("0000004000000");
        assertThat(marshal).isEqualTo("4000000");
    }
}