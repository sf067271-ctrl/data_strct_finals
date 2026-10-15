// Parent of every exception caused by the user or by a business rule.
// Demonstrates INHERITANCE (all custom exceptions extend this class) and ABSTRACTION
// (Main can catch any store problem through this one type).
public abstract class StoreException extends Exception {
    // Exceptions are Serializable, so each class declares a version number.
    private static final long serialVersionUID = 1L;

    protected StoreException(String message) {
        super(message);
    }
}
