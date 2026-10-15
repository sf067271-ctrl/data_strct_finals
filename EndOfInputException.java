// Thrown when there is no more input to read (for example, the input file ended).
// Demonstrates INHERITANCE (extends StoreException).
public class EndOfInputException extends StoreException {
    private static final long serialVersionUID = 1L;

    public EndOfInputException() {
        super("No more input is available.");
    }
}
