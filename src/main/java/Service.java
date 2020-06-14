import java.io.IOException;

public class Service {


    public static void main(String[] args) throws IOException, InterruptedException {
        String pathFrom = args[0];
        String pathTo = args[1];
        SyncService syncService = new SyncService(pathFrom, pathTo);
        syncService.execute();
    }

}
