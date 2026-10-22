package sarisari.exceptions;

/**
 * Thrown when a product code, product name, or customer name is already used.
 * (INHERITANCE: a more specific kind of StoreException.)
 */
public class DuplicateRecordException extends StoreException {

    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }
}
