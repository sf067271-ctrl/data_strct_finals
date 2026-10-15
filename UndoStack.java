import java.util.Objects;
import java.util.Stack;

// The undo history, stored as a STACK (LIFO: Last In, First Out). The last transaction recorded
// is the first one undone, no matter what dates the transactions have.
// Demonstrates ENCAPSULATION and ABSTRACTION: the java.util.Stack is hidden behind five methods.
public class UndoStack {
    private final Stack<Transaction> stack = new Stack<>();

    // Puts a successfully saved transaction on top of the stack.
    public void push(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");
        if (!transaction.isExecuted() || transaction.isUndone()) {
            throw new IllegalArgumentException("Only saved, active transactions can be pushed.");
        }
        stack.push(transaction);
    }

    // Returns the top transaction without removing it.
    public Transaction peek() throws UndoException {
        requireNotEmpty();
        return stack.peek();
    }

    // Removes and returns the top transaction.
    public Transaction pop() throws UndoException {
        requireNotEmpty();
        return stack.pop();
    }

    // Returns true if there is nothing to undo.
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    // Returns how many transactions can still be undone.
    public int size() {
        return stack.size();
    }

    private void requireNotEmpty() throws UndoException {
        if (stack.isEmpty()) {
            throw new UndoException("Nothing to undo. There are no saved transactions left on the undo stack.");
        }
    }
}
