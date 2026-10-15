// Thrown when no product or customer matches what the operator typed.
// Demonstrates INHERITANCE (extends StoreException).
public class RecordNotFoundException extends StoreException {
    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }
}
