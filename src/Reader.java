import java.io.*;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Used to read processes from a file.
 */
public class Reader {
    /**
     * Used to read file.
     */
    private BufferedReader _reader;

    /**
     * Holds lines in the file.
     */
    private String _line;

    /**
     * Stores the processes in the order in which they're read from the file.
     */
    private Queue<double[]> _processQueue;

    private double _processNumber = 1;

    /**
     * Constructor used to create a Reader object, read processes from file, and insert them into a Queue.
     * @param fileName: The file to be read.
     */
    public Reader(File fileName) throws IOException {
        try {
            this._reader = new BufferedReader(new FileReader(fileName));

            _processQueue = readLines();
        } catch (FileNotFoundException e) {
            Logger.getLogger("").severe("File not found: " + fileName.getAbsolutePath());
        }
    }

    /**
     * Gets the loaded process Queue.
     * @return The Queue of processes.
     */
    public Queue<double[]> get_processQueue() {
        return _processQueue;
    }

    /**
     * Close the reader
     * @throws IOException: In case an error occurs
     */
    public void closeReader() throws IOException {
        _reader.close();
    }

    /**
     * Read lines from file and store processes in a Queue.
     * @return The Queue of processes
     * @throws IOException
     */
    private Queue<double[]> readLines() throws IOException {
        Queue<double[]> processQueue = new LinkedBlockingQueue<>();

        try {
            while ((_line = _reader.readLine()) != null) {
                String[] s = _line.split(" ");
                processQueue.add(new double[] {Integer.parseInt(s[0]), Integer.parseInt(s[1]), _processNumber++});
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        closeReader();

        return processQueue;
    }
}
