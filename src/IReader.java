import java.io.IOException;

public interface IReader {
    /**
     * Reads one character at a time from the file.
     * @return The next character.
     * @throws IOException
     */
    char read() throws IOException;
}
