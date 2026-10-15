// Thrown when the requested quantity is more than the stock on hand.
// Demonstrates INHERITANCE (extends StoreException).
public class InsufficientStockException extends StoreException {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super(message);
    }
}
