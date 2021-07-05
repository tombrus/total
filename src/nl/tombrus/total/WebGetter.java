package nl.tombrus.total;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.tombrus.total.Logger.log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import org.modelingvalue.json.Json;

abstract class WebGetter implements Callable<Double> {
    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15";
    private             double last       = 0;

    @Override
    public Double call() throws Exception {
        long t0 = System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(getUrl()).openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept-Encoding", "gzip");

            try (InputStream in = makeCorrectInputStream(conn)) {
                String           json = new String(in.readAllBytes(), UTF_8);
                Optional<Double> opt  = extractDouble(json);
                if (opt.isEmpty()) {
                    log("ERROR: " + getClass().getSimpleName() + ": no identifyable value in: " + json);
                }
                last = opt.orElse(last);
            } catch (Throwable e) {
                log("ERROR: " + getClass().getSimpleName() + ": " + e.getMessage());
                log(e);
            }
        } catch (Throwable e) {
            log("ERROR: " + getClass().getSimpleName() + ": " + e.getMessage());
            log(e);
        } finally {
            log(String.format("# %-14s took %5d ms", getClass().getSimpleName(), System.currentTimeMillis() - t0));
        }
        return last;
    }

    private InputStream makeCorrectInputStream(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getInputStream();
        if ("gzip".equals(conn.getContentEncoding())) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    protected abstract String getUrl();

    protected abstract Optional<Double> extractDouble(String text);

    public static <T> Optional<T> getJsonPath(String json, String path) {
        return getJsonPath(Json.fromJson(json), path);
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    public static <T> Optional<T> getJsonPath(Object o, String path) {
        try {
            String[] parts = path.split("([.\\[\\]])+");
            for (String part : parts) {
                if (part.matches("[0-9]+")) {
                    o = ((List<Object>) o).get(Integer.parseInt(part));
                } else {
                    o = ((Map<String, Object>) o).get(part);
                }
            }
            //noinspection unchecked
            return o == null ? Optional.empty() : Optional.of((T) o);
        } catch (ClassCastException | NumberFormatException e) {
            log("problem while parsing json path '" + path + "'");
            log(e);
            return Optional.empty();
        }
    }
}
