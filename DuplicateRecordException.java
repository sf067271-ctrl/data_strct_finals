// Thrown when a product code, product name, or customer name already exists.
// Demonstrates INHERITANCE (extends StoreException).
public class DuplicateRecordException extends StoreException {
    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }
}
