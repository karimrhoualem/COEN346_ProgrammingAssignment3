import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
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
    private static File _inputFile;

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
     * Dictionary of process start statuses.
     */
    private static HashMap<Integer, Boolean> _processesStarted;

    /**
     * Keeps track of the current runtime.
     */
    private static double _time;

    /**
     * Semaphore used to synchronize processes/threads.
     */
    private static Semaphore _semaphore;

    /**
     * File writer user to log data to an output file.
     */
    private static FileWriter _fileWriter;

    /**
     * Name of output file.
     */
    private static File _outputFile;

    /**
     * Used to synchronize access to the file writer when logging that a process started.
     */
    private static Object _startLock = new Object();

    /**
     * Used to synchronize access to the file writer when logging that a process resumed.
     */
    private static Object _resumeLock = new Object();

    /**
     * Used to synchronize access to the file writer when logging that a process paused.
     */
    private static Object _pauseLock = new Object();

    /**
     * Used to synchronize access to the file writer when logging that a process terminated.
     */
    private static Object _finishLock = new Object();

    /**
     * Used to synchronize access to the time variable when updating processes.
     */
    private static Object _updateLock = new Object();

    /**
     * Used to synchronize access to the file writer when logging process wait times.
     */
    private static Object _waitLock = new Object();

    /**
     * Static constructor used to initialize the static variables to be used across the class instances.
     */
    static {
        try {
            _inputFile = new File("input.txt");
            _outputFile = new File("output.txt");
            _fileWriter = new FileWriter(_outputFile);
            _reader = new Reader(_inputFile);
            _processQueue = _reader.get_processQueue();
            _duplicateProcessQueue = new LinkedBlockingQueue<>();
            CopyQueue();
            _queueSize = _processQueue.size();
            _processWaitTimes = new HashMap<Integer, Double>();
            _processCompletionTimes = new HashMap<Integer, Double>();
            _processesStarted = new HashMap<Integer, Boolean>();
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
     * @param tid The object's thread ID.
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
    private void StartProcess() throws InterruptedException, IOException {

        while (true) {
            _semaphore.acquire();

            if (!_processQueue.isEmpty()) {
                var process = FindProcessWithShortestRemainingTime();

                if (!_processesStarted.get((int)process[2])) {
                    synchronized (_startLock) {
                        LogStartProcess(process);
                    }
                    _processesStarted.put((int)process[2], true);
                }

                synchronized (_resumeLock) {
                    LogResumedProcess(process);
                }

                double[] updatedProcess;
                synchronized (_updateLock) {
                    updatedProcess = UpdateRunTime(process);
                }

                if (updatedProcess[1] == 0) {
                    _processCompletionTimes.put((int)process[2], _time);
                    synchronized (_finishLock) {
                        LogCompletedProcess(updatedProcess);
                    }
                }
                else {
                    synchronized (_pauseLock) {
                        LogPausedProcess(updatedProcess);
                    }
                    _processQueue.add(updatedProcess);
                }

            }
            else {
                FindWaitTimes();
                synchronized (_waitLock) {
                    LogWaitTimes();
                }
                _fileWriter.flush();
                _fileWriter.close();
                System.exit(1);
            }

            _semaphore.release();
        }
    }

    /**
     * Rearranges Queue and places the process with the shortest remaining time at the front of the Queue.
     * It is then removed to execute next.
     */
    private double[] FindProcessWithShortestRemainingTime() {
        var SRT = _processQueue.peek();

        for (var process : _processQueue) {
            if (_time >= process[0]) { // Only check if process has already arrived in the queue.
                if (process[1] <= SRT[1]) { // By checking for "<=", the scheduler will give priority to the older process if two have equal SRT.
                    SRT = process;
                }
            }
            else {
                break;
            }
        }

        while (SRT[2] != _processQueue.peek()[2]) { // We check for the process number in the 3rd index to give priority to older process.
            _processQueue.add(_processQueue.remove());
        }

        return _processQueue.remove();
    }

    /**
     * Finds the wait times for each process upon program termination.
     */
    private void FindWaitTimes() {
        int i = 1;

        for (var process : _duplicateProcessQueue) {
            var completionTime = _processCompletionTimes.get(i);
            _processWaitTimes.put(i, ((completionTime - process[0] - process[1]) < 0)? 0.0 : (completionTime - process[0] - process[1]));
            i++;
        }
    }

    /**
     * Logs the wait times to console and an output file.
     */
    private void LogWaitTimes() throws IOException {
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Waiting Times:");

        _fileWriter.write("---------------------------------------------------------------------\n");
        _fileWriter.write("Waiting Times:\n");

        for (int i = 1; i <= _processWaitTimes.size(); i++) {
            var waitTime = _processWaitTimes.get(i);
            System.out.println("Process " + i + String.format(": %.6f", waitTime));
            _fileWriter.write("Process " + i + String.format(": %.6f\n", waitTime));
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
            _processesStarted.put(i, false);
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
     * Logs the process starting message to console and an output file.
     * @param process The current process that is starting.
     */
    private void LogStartProcess(double[] process) throws IOException {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Started.");
        _fileWriter.write("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Started.\n");
    }

    /**
     * Logs the process resuming message to console and an output file.
     * @param process The current process that is resuming.
     */
    private void LogResumedProcess(double[] process) throws IOException {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Resumed.");
        _fileWriter.write("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)process[2] + ", Resumed.\n");
    }

    /**
     * Logs the process pausing message to console and an output file.
     * @param updatedProcess The current process that is pausing.
     */
    private void LogPausedProcess(double[] updatedProcess) throws IOException {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Paused.");
        _fileWriter.write("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Paused.\n");
    }

    /**
     * Logs the process completion message to console and an output file.
     * @param updatedProcess The current process that is terminating.
     */
    private void LogCompletedProcess(double[] updatedProcess) throws IOException {
        System.out.println("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Finished.");
        _fileWriter.write("[Thread " + _threadNumber + String.format("] Time: %.6f", _time) + ", Process " + (int)updatedProcess[2] + ", Finished.\n");
    }
}
