package nl.tombrus.total;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.json.FromJson;

public class Settings {
    private final Path      file;
    private       Data      data;
    private       Throwable problem;

    public Settings(Path file) {
        this.file = file;
        new FileWatcherThread(file, () -> data = readData(), this::problem);
    }

    @SuppressWarnings("unchecked")
    private Data readData() {
        try {
            String              s = String.join("\n", Files.readAllLines(file));
            Map<String, Object> o = (Map<String, Object>) FromJson.fromJson(s);
            return new Data((String) o.get("valuta"), (HashMap<String, Double>) o.get("stock"), (HashMap<String, Double>) o.get("crypto"));
        } catch (Throwable e) {
            problem(e);
            return null;
        }
    }

    public Path getFile() {
        return file;
    }

    public Data getData() {
        return data;
    }

    public Throwable getProblem() {
        return problem;
    }

    private void problem(Throwable e) {
        problem = e;
        Logger.log("problem with settings: " + e.getMessage());
    }

    public static class Data {
        public final String                  valuta;
        public final HashMap<String, Double> stock;
        public final HashMap<String, Double> crypto;

        public Data(String valuta, HashMap<String, Double> stock, HashMap<String, Double> crypto) {
            this.valuta = valuta;
            this.stock = stock;
            this.crypto = crypto;
        }
    }
}
