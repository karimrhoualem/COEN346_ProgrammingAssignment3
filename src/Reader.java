import java.io.*;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Used to read processes from a file.
 */
public class Reader {
//    //TODO: delete this after testing
//    public static void main(String[] args) throws IOException {
//        File file = new File("TestFile.txt");
//        Queue<int[]> processQueue;
//
//        Reader reader = new Reader(file);
//        processQueue = reader.get_processQueue();
//
//        for (var process: processQueue) {
//            System.out.println(process[0] + " " + process[1]);
//        }
//    }

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
    private Queue<int[]> _processQueue;

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
    public Queue<int[]> get_processQueue() {
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
    private Queue<int[]> readLines() throws IOException {
        Queue<int[]> processQueue = new LinkedBlockingQueue<>();

        try {
            while ((_line = _reader.readLine()) != null) {
                String[] s = _line.split(" ");
                processQueue.add(new int[] {Integer.parseInt(s[0]), Integer.parseInt(s[1])});
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
