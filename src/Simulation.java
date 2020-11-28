import java.io.File;
import java.io.IOException;
import java.util.Queue;

/**
 * Main simulation entry point to start the RR Scheduler.
 */
public class Simulation {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Set up the simulation by loading processes from input file
        File file = new File("TestFile.txt");
        Reader reader = new Reader(file);
        Queue<double[]> processQueue;
        processQueue = reader.get_processQueue();
        int queueSize = processQueue.size();
        int threadNumber = 0;

        // Run one new thread for each process in the Queue.
        for (int i = 0; i < queueSize; i++) {
            Runnable runnable = new RRScheduler(++threadNumber);
            Thread mainThread = new Thread(runnable);
            mainThread.start();
        }
    }
}
