// Thrown when the operator types bad input: blank, wrong format, or out of range.
// Demonstrates INHERITANCE (extends StoreException).
public class ValidationException extends StoreException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
