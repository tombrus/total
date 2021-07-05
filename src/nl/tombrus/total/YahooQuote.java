package nl.tombrus.total;

import java.util.Map;
import java.util.Optional;

import org.modelingvalue.json.Json;

class YahooQuote extends WebGetter {
    public static final  String              RESULT_PATH        = "quoteResponse.result[0]";
    public static final  String              MARKET_STATE_FIELD = "marketState";
    public static final  String              FALLBACK_FIELD     = "regularMarketPrice";
    private static final Map<String, String> MARKETSTATE2FIELD  = Map.of(
            "PRE", "preMarketPrice",
            "REGULAR", FALLBACK_FIELD,
            "POST", "postMarketPrice",
            "POSTPOST", "postMarketPrice",
            "PREPRE", "postMarketPrice",
            "CLOSED", FALLBACK_FIELD
    );

    private final String symbol;

    public YahooQuote(String symbol) {
        this.symbol = symbol;
    }

    @Override
    protected String getUrl() {
        return "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + symbol;
    }

    @Override
    protected Optional<Double> extractDouble(String json) {
        Object jsonParsed  = Json.fromJson(json);
        String marketState = WebGetter.<String> getJsonPath(jsonParsed, RESULT_PATH + "." + MARKET_STATE_FIELD).orElseThrow();
        String field       = MARKETSTATE2FIELD.get(marketState);
        if (field == null) {
            Logger.log("unknown " + MARKET_STATE_FIELD + "=" + marketState);
            Logger.log("JSON=" + json);
            return Optional.of(0.0);
        } else {
            Optional<Double> opt = getJsonPath(jsonParsed, RESULT_PATH + "." + field);
            if (opt.isEmpty()) {
                opt = getJsonPath(jsonParsed, RESULT_PATH + "." + FALLBACK_FIELD);
            }
            return opt;
        }
    }
}
