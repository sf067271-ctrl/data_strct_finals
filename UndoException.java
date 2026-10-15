// Thrown when there is nothing to undo, or when undoing would make the data inconsistent.
// Demonstrates INHERITANCE (extends StoreException).
public class UndoException extends StoreException {
    private static final long serialVersionUID = 1L;

    public UndoException(String message) {
        super(message);
    }
}
