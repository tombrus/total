package nl.tombrus.total;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

public class FileWatcherThread extends Thread {
    private final Path                file;
    private final Runnable            changed;
    private final Consumer<Throwable> problem;
    private final WatchService        watchService;

    public FileWatcherThread(Path file, Runnable changed, Consumer<Throwable> problem) {
        setDaemon(true);
        this.file = file;
        this.changed = changed;
        this.problem = problem;
        watchService = getWatchService();
        start();
        changed.run();
    }

    private WatchService getWatchService() {
        try {
            return FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            problem.accept(e);
            return null;
        }
    }

    @Override
    public void run() {
        try {
            WatchKey watchKey;
            file.getParent().register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
            do {
                watchKey = watchService.take();
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind != OVERFLOW && event.context().equals(file)) {
                        changed.run();
                    }
                }
            } while (watchKey.reset());
        } catch (IOException | InterruptedException e) {
            problem.accept(e);
        }
    }
}
