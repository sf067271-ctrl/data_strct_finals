// Thrown when the operator types the cancel command (/cancel) at any prompt.
// Demonstrates INHERITANCE (extends StoreException).
public class InputCancelledException extends StoreException {
    private static final long serialVersionUID = 1L;

    public InputCancelledException() {
        super("Operation cancelled. No changes were made.");
    }
}
