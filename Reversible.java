// Contract for anything that can be carried out and later undone.
// Demonstrates ABSTRACTION: callers only know these three methods, not how each one works.
public interface Reversible {
    // Validates, then applies the change. Throws and changes nothing if any check fails.
    void execute() throws StoreException;

    // Validates, then undoes the change. Throws and changes nothing if any check fails.
    void reverse() throws StoreException;

    // Returns true once the change has been undone.
    boolean isUndone();
}
