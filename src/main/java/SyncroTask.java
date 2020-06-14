import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncroTask implements Runnable {

    private final SyncService syncService;
    private final String kind;
    private final Path filePath;
    private final Path directoryTo;

    public SyncroTask(SyncService syncService, String kind, Path filePath, Path directoryTo) {
        this.syncService = syncService;
        this.kind = kind;
        this.filePath = filePath;
        this.directoryTo = directoryTo;
    }

    private void sync() throws IOException {
        switch (kind) {
            case "ENTRY_CREATE":
            case "ENTRY_MODIFY":
                System.out.println("copying="+filePath.toFile()+" to "+directoryTo.toFile());
                if (Files.isSymbolicLink(filePath)) {
                    Files.createSymbolicLink(directoryTo.resolve(filePath.getFileName()), Files.readSymbolicLink(filePath));
                } else {
                    FileUtils.copyFileToDirectory(filePath.toFile(), directoryTo.toFile());
                }
                System.out.println("success");
                break;
            case "ENTRY_DELETE":
                File fileToDelete = directoryTo.resolve(filePath.getFileName()).toFile();
                System.out.println("deleting="+fileToDelete);
                FileUtils.deleteQuietly(fileToDelete);
                System.out.println("success");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

    @Override
    public void run() {
        try {
            sync();
            syncService.getMap().remove(filePath.toFile().getAbsolutePath());
        } catch (FileNotFoundException fe) {
            System.out.println("There was an error :" + fe.getMessage() + ", close quietly");
        } catch (IOException e) {
                System.out.println("There was an error :" + e.getMessage() + ", will try again");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                syncService.submit(this);
        }
    }
}
