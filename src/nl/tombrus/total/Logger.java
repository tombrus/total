package nl.tombrus.total;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final Path LOG_FILE = Paths.get(System.getProperty("user.home"), ".total.log");

    private static String getDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    static void log(String msg) {
        try {
            Files.writeString(LOG_FILE, String.format("%s: %s\n", getDate(), msg), WRITE, APPEND, CREATE);
        } catch (IOException e) {
            System.err.println("IOException while writing log " + LOG_FILE + ": " + msg);
            e.printStackTrace();
        }
    }

    static void log(Throwable e) {
        try (OutputStream out = Files.newOutputStream(LOG_FILE, WRITE, APPEND, CREATE)) {
            e.printStackTrace(new PrintStream(out));
        } catch (IOException io) {
            log("intern probleem: " + io + " while logging " + e);
        }
    }
}
