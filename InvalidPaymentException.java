// Thrown when a payment is zero, has too many decimals, or is above the amount still owed.
// Demonstrates INHERITANCE (extends StoreException).
public class InvalidPaymentException extends StoreException {
    private static final long serialVersionUID = 1L;

    public InvalidPaymentException(String message) {
        super(message);
    }
}
