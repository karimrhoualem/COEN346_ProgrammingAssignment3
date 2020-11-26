import java.io.*;
import java.util.logging.Logger;

/**
 * Used to read characters from the file.
 */
public class Reader implements IReader {
    private BufferedReader bfReader;

    /**
     * Reads one character at a time from the file.
     * @return The next character.
     * @throws IOException
     */
    @Override
    public char read() throws IOException {
        int nextChar = bfReader.read();
        return isEOFchar(nextChar) ? '\0' : (char) nextChar;
    }

    /**
     * Constructor used to create a Reader object.
     * @param fileName: The file to be read.
     */
    public Reader(File fileName) {
        try {
            this.bfReader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            Logger.getLogger("").severe("File not found: " + fileName.getAbsolutePath());
        }
    }

    /**
     * Close the reader
     * @throws IOException: In case an error occurs
     */
    public void closeReader () throws IOException {
        bfReader.close();
    }

    /**
     * Verifies if the next char is an EOF character
     * @param nextChar: The next character
     * @return: true if EOF, false if not
     */
    private boolean isEOFchar(int nextChar){
        return (nextChar == -1);
    }
}
