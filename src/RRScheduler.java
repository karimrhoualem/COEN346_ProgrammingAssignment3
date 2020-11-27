import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Implements the Round Robin scheduling algorithm.
 */
public class RRScheduler implements Runnable {

    /**
     * The size of the time quantum for each process.
     */
    private final double _smallestTimeQuantum = 0.1;

    /**
     * The current thread number. The count will equal to the number of processes.
     */
    private int _threadNumber;

    /**
     * The File object that holds the process input data.
     */
    private static File _file;

    /**
     * Reader object used to read process data.
     */
    private static Reader _reader;

    /**
     * Holds the processes in the order in which they arrive to the system.
     */
    private static Queue<double[]> _processQueue;

    /**
     * Holds a duplicate of the original Queue for logging purposes.
     */
    private static Queue<double[]> _duplicateProcessQueue;

    /**
     * Size of the original Queue.
     */
    private static int _queueSize;

    /**
     * Dictionary of process waiting times. Calculated as Waiting Time = (Turnaround Time - Arrival Time - Burst Time)
     */
    private static HashMap<Integer, Double> _processWaitTimes;

    /**
     * Dictionary of process completion times.
     */
    private static HashMap<Integer, Double> _processCompletionTimes;

    /**
     * Keeps track of the current runtime.
     */
    private static double _time;

    /**
     * Semaphore used to synchronize processes/threads.
     */
    private static Semaphore _semaphore;

    /**
     * Static constructor used to initialize the static variables to be used across the class instances.
     */
    static {
        try {
            _file = new File("TestFile.txt");
            _reader = new Reader(_file);
            _processQueue = _reader.get_processQueue();
            _duplicateProcessQueue = new LinkedBlockingQueue<>();
            CopyQueue();
            _queueSize = _processQueue.size();
            _processWaitTimes = new HashMap<Integer, Double>();
            _processCompletionTimes = new HashMap<Integer, Double>();
            InitializeHashMaps();
            _time = _processQueue.peek()[0];
            _semaphore = new Semaphore(1);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a new RRScheduler object
     * @param tid The objects thread ID.
     */
    public RRScheduler(int tid) {
        _threadNumber = tid;
    }

    /**
     * Override Thread class run method to start a new thread.
     */
    @Override
    public void run() {
        try {
            StartProcess();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Core Round Robin Scheduler logic that executes processes.
     * @throws InterruptedException
     */
    private void StartProcess() throws InterruptedException {

        while (true) {
            _semaphore.acquire();

            if (!_processQueue.isEmpty()) {
                var process = _processQueue.remove();

                LogStartProcess(process);
                LogResumedProcess(process);

                var updatedProcess = UpdateRunTime(process);

                if (updatedProcess[1] == 0) {
                    _processCompletionTimes.put((int)process[2], _time);
                    LogCompletedProcess(updatedProcess);
                }
                else {
                    LogPausedProcess(updatedProcess);
                    _processQueue.add(updatedProcess);
                }

            }
            else {
                FindWaitTimes();
                LogWaitTimes();
                System.exit(1);
            }

            _semaphore.release();
        }
    }

    /**
     * Finds the wait times for each process upon program termination.
     */
    private void FindWaitTimes() {
        int i = 1;

        for (var process : _duplicateProcessQueue) {
            var completionTime = _processCompletionTimes.get(i);
            _processWaitTimes.put(i, completionTime - process[0] - process[1] );
            i++;
        }
    }

    /**
     * Logs the wait times to an output file.
     */
    private void LogWaitTimes() {
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Waiting Times:");

        int i = 1;

        for (var process : _duplicateProcessQueue) {
            var waitTime = _processWaitTimes.get(i);
            System.out.println("Process " + i + String.format(": %.6f", waitTime));
            i++;
        }
    }

    /**
     * Updates the runtime as processes execute.
     * @param process The current executing process.
     * @return The updated process object.
     */
    private double[] UpdateRunTime(double[] process) {
        var timeQuantum = 0.1*process[1];

        if (process[1] <= _smallestTimeQuantum) {
            _time += process[1];
            process[1] = 0;
        }
        else {
            process[1] -= timeQuantum;
            _time += timeQuantum;
        }

        return process;
    }

    /**
     * Initializes the dictionaries with default values.
     */
    private static void InitializeHashMaps() {
        for (int i = 1; i <= _queueSize; i++) {
            _processWaitTimes.put(i, 0.0);
            _processCompletionTimes.put(i, 0.0);
        }
    }

    /**
     * DEEP copy of the original Queue to retain original values for waiting time calculations.
     */
    private static void CopyQueue() {
        for (var process : _processQueue) {
            _duplicateProcessQueue.add(new double[] {process[0], process[1], process[2]});
        }
    }

    /**
     * Logs the process starting message to an output file.
     * @param process The current process that is starting.
     */
    private void LogStartProcess(double[] process) {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Started.");
    }

    /**
     * Logs the process resuming message to an output file.
     * @param process The current process that is resuming.
     */
    private void LogResumedProcess(double[] process) {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Resumed.");
    }

    /**
     * Logs the process pausing message to an output file.
     * @param updatedProcess The current process that is pausing.
     */
    private void LogPausedProcess(double[] updatedProcess) {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Paused.");
    }

    /**
     * Logs the process completion message to an output file.
     * @param updatedProcess The current process that is terminating.
     */
    private void LogCompletedProcess(double[] updatedProcess) {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Finished.");
    }
}
