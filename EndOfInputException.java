/**
 * Thrown by InputHelper when there is no more input to read (for example,
 * the terminal was closed or Ctrl+Z / Ctrl+D was pressed).
 *
 * Main catches it and ends the program politely instead of crashing with a
 * NoSuchElementException.
 */
public class EndOfInputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EndOfInputException() {
        super("No more input is available.");
    }
}
