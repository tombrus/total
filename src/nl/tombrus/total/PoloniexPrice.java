package nl.tombrus.total;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

class PoloniexPrice extends WebGetter {
    private final String from;
    private final String to;

    public PoloniexPrice(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    protected String getUrl() {
        long t = LocalDateTime.now(ZoneId.of("UTC")).toEpochSecond(ZoneOffset.ofHours(0));
        return "https://poloniex.com/public?command=returnTradeHistory&currencyPair=" + from + "T_" + to + "&start=" + (t - 60);
    }

    protected Optional<Double> extractDouble(String json) {
        return Arrays.stream(json.split("[,{}]"))
                .filter(s -> s.contains("\"rate\""))
                .map(s -> s.replaceAll(".*: *\"", "").replaceAll("[^0-9.]", ""))
                .map(Double::parseDouble)
                .findFirst();
    }
}
