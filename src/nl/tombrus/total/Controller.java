package nl.tombrus.total;

import static nl.tombrus.total.Logger.log;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import nl.tombrus.total.Settings.Data;

public class Controller extends Thread {
    private static final Path            SETTINGS_FILE               = Paths.get(System.getProperty("user.home")).resolve(".total.json");
    private static final ExecutorService EXECUTOR                    = Executors.newFixedThreadPool(10, r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });
    public static final  int             FOREGROUND_REFRESH_INTERVAL = 10 * 1000;
    public static final  int             BACKGROUND_REFRESH_INTERVAL = 60 * 1000;
    public static final  double          MAX_SINCE                   = 2 * 60 * 1000;
    public static final  double          PRE_PHASE                   = 10 * 1000;
    public static final  Color           ALARM_COLOR                 = Color.web("#ff5050");
    public static final  Color           OK_COLOR                    = Color.web("#a2d7eb");

    public        Label    total;
    public        Button   refresh;
    public        Tooltip  tooltip;
    //
    private final Settings settings = new Settings(SETTINGS_FILE);
    private       long     lastRefreshTick;
    private       Color    lastColor;

    public Controller() {
        super("total-controller");
        setDaemon(true);
        start();
    }

    @SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
    public void run() {
        try {
            log("started in " + Paths.get(".").toAbsolutePath().getParent());
            while (true) {
                Thread.sleep(getRefreshInterval() / 10);
                if (!windowNotShowing()) {
                    if (getRefreshInterval() < getSinceLastRefresh()) {
                        refresh(null);
                    }

                    Color c = interpolate(ALARM_COLOR, OK_COLOR, getSinceLastRefresh());
                    if (!c.equals(lastColor) && lastRefreshTick != 0) {
                        Platform.runLater(() -> refresh.setStyle(getBackgroundColorStyle(c)));
                        lastColor = c;
                    }
                }
            }
        } catch (InterruptedException e) {
            log("UNEXPECTED: " + e);
            log(e);
        }
    }

    private int getRefreshInterval() {
        return windowNotShowing() ? 50 : total.getScene().getWindow().isFocused() ? FOREGROUND_REFRESH_INTERVAL : BACKGROUND_REFRESH_INTERVAL;
    }

    private long getSinceLastRefresh() {
        return System.currentTimeMillis() - lastRefreshTick;
    }

    private boolean windowNotShowing() {
        return total == null || total.getScene() == null || total.getScene().getWindow() == null || !total.getScene().getWindow().isShowing();
    }

    private String getBackgroundColorStyle(Color hsb) {
        return "-fx-background-color: " + hsb.toString().replace("0x", "#");
    }

    public void refresh(@SuppressWarnings("unused") ActionEvent event) {
        Platform.runLater(() -> total.setTextFill(Color.web("#aaa")));
        List<String> l = calcValue();
        Platform.runLater(() -> total.setTextFill(Color.web("#000")));

        String mainText    = l.get(0);
        String tooltipText = l.get(1);

        log(String.join(", ", l).replace('\n', ' '));
        Platform.runLater(() -> {
            total.setText(mainText);
            tooltip.setText(tooltipText);
        });

        lastRefreshTick = System.currentTimeMillis();
    }

    private List<String> calcValue() {
        Data data = settings.getData();
        if (data == null) {
            return Arrays.asList("ERROR", "no data");
        }
        try {
            Future<Double> epd = EXECUTOR.submit(new YahooQuote("USDEUR%3dX"));
            Future<Double> dpb = EXECUTOR.submit(new YahooQuote("BTC-USD"));
            Future<Double> dpt = EXECUTOR.submit(new YahooQuote("TSLA"));

            log(String.format("##%12.2f;%12.2f;%12.2f;", dpb.get(), epd.get(), dpt.get()));

            double v1 = dpt.get() * epd.get() * data.stock.get("TSLA");
            double v2 = dpb.get() * epd.get() * data.crypto.get("BTC");
            double v  = v1 + v2;

            String mainText = String.format("%,10.0f", v);
            String tooltipText = String.format("""
                            %,9.0f (@%,6.0f x %4.2f)
                            %,9.0f (@%,6.0f x %4.2f)
                            """,

                    v1, dpt.get(), epd.get(),
                    v2, dpb.get(), epd.get());

            return Arrays.asList(mainText, tooltipText);
        } catch (InterruptedException e) {
            return Arrays.asList("ERROR", "INTERRUPT");
        } catch (ExecutionException e) {
            return Arrays.asList("ERROR", "EXEC");
        } catch (Throwable e) {
            return Arrays.asList("ERROR", "THROWABLE");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private Color interpolate(Color c1, Color c2, long sinceLastRefresh) {
        double fact1 = getFact(sinceLastRefresh);
        double fact2 = 1.0 - fact1;
        double r     = c1.getRed() * fact1 + c2.getRed() * fact2;
        double g     = c1.getGreen() * fact1 + c2.getGreen() * fact2;
        double b     = c1.getBlue() * fact1 + c2.getBlue() * fact2;
        return new Color(r, g, b, 1.0);
    }

    private double getFact(long sinceLastRefresh) {
        double raw = (sinceLastRefresh - PRE_PHASE) / (MAX_SINCE - PRE_PHASE);
        return Math.max(0.0, Math.min(raw, 1.0));
    }
}
