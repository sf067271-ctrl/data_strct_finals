package sarisari.exceptions;

/**
 * Base class for every BUSINESS-RULE error in the store, for example
 * "not enough stock" or "payment is bigger than the balance".
 *
 * OOP - INHERITANCE:
 *   StoreException extends Exception (Java's built-in class), and the more
 *   specific errors below extend StoreException:
 *
 *       Exception
 *         └── StoreException
 *               ├── DuplicateRecordException
 *               └── InsufficientStockException
 *
 *   Because of this, one "catch (StoreException e)" in Main catches all of
 *   them (POLYMORPHISM: a subclass object can be used wherever its parent
 *   class is expected).
 *
 * It is a CHECKED exception, so the compiler forces every caller to handle it.
 */
public class StoreException extends Exception {

    private static final long serialVersionUID = 1L;

    public StoreException(String message) {
        super(message);
    }
}
