package nl.tombrus.total;

import java.util.Arrays;
import java.util.Optional;

class ValutaConverter extends WebGetter {
    private final String from;
    private final String to;

    public ValutaConverter(String from, String to) {
        this.from = from;
        this.to = to;
        if (!to.equals("EUR")) {
            throw new Error("can only convert to EUR, not to " + to);
        }
    }

    @Override
    protected String getUrl() {
        return "https://www.ecb.europa.eu/rss/fxref-" + from.toLowerCase() + ".html";
    }

    protected Optional<Double> extractDouble(String text) {
        return Arrays.stream(text.split("\n"))
                .filter(s -> s.contains(";rate="))
                .map(s -> s.replaceAll(".*;rate=", "").replaceAll("\".*", ""))
                .map(Double::parseDouble)
                .map(d -> 1 / d)
                .findFirst();
    }
}
