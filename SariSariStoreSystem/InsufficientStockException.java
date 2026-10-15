/**
 * Thrown when a sale or utang asks for more pieces than are on the shelf.
 * (INHERITANCE: a more specific kind of StoreException.)
 */
public class InsufficientStockException extends StoreException {

    private static final long serialVersionUID = 1L;

    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, int requested, int available) {
        super(buildMessage(productName, requested, available));
        this.requested = requested;
        this.available = available;
    }

    private static String buildMessage(String productName, int requested, int available) {
        if (available == 0) {
            return productName + " is out of stock.";
        }
        return "Not enough stock for " + productName + ": you asked for " + requested
                + ", but only " + available + " " + (available == 1 ? "piece is" : "pieces are")
                + " available.";
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
