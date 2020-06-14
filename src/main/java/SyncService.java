import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncService {

    private final Path pathFrom;
    private final Path pathTo;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public SyncService(String pathFrom, String pathTo) {
        this.pathFrom = Paths.get(pathFrom);
        this.pathTo = Paths.get(pathTo);
    }


    public void execute() throws IOException, InterruptedException {
        // delete pathTo directory
        FileUtils.deleteDirectory(pathTo.toFile());
        // initial copy of all files from pathFrom to pathTo
        for (final File fileEntry : pathFrom.toFile().listFiles()) {
            map.put(fileEntry.getAbsolutePath(), "ENTRY_CREATE");
            // call sync
            submit(new SyncroTask(this,  "ENTRY_CREATE", Paths.get(fileEntry.toURI()), pathTo));
        }

        // TODO: wait initial copy task to complete

        // infinite loop for watcher
        WatchService watchService = FileSystems.getDefault().newWatchService();

        pathFrom.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path path = pathFrom.resolve(ev.context());
                if (!map.containsKey(path.toFile().getAbsolutePath())) {
                    System.out.println("Event kind:" + ev.kind() + ". File affected: " + path.toFile().getAbsolutePath());
                    map.put(path.toFile().getAbsolutePath(), ev.kind().name());
                    // call sync
                    submit(new SyncroTask(this, ev.kind().name(), path, pathTo));
                }
            }
            key.reset();
        }

    }

    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }

    public void submit(SyncroTask syncroTask) {
        executorService.submit(syncroTask);
    }

}
