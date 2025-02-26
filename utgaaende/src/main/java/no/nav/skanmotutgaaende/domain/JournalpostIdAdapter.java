package no.nav.skanmotutgaaende.domain;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

class JournalpostIdAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String s) {
        return removeLeadingZeros(s);
    }

    @Override
    public String marshal(String s) {
        return removeLeadingZeros(s);
    }

    private String removeLeadingZeros(String s) {
        return s.replaceFirst("^0+(?!$)", "");
    }
}
