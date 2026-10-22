package sarisari.datastructures;

import java.util.Stack;

import sarisari.transactions.Transaction;

/**
 * The undo stack.
 *
 * STACK = LIFO (Last In, First Out): like a pile of plates, the last
 * transaction put on top is the first one taken off. That is exactly how
 * "undo" works: it always reverses the MOST RECENT transaction first.
 *
 *     push(sale)     ->  [sale]
 *     push(utang)    ->  [sale, utang]
 *     push(payment)  ->  [sale, utang, payment]   <- top
 *     pop()          ->  payment is undone first, then utang, then sale
 *
 * OOP - ENCAPSULATION: the java.util.Stack is private. Other classes can
 * only use push/pop/peek/isEmpty/size, so nobody can remove a transaction
 * from the middle of the stack.
 */
public class UndoStack {

    private final Stack<Transaction> stack = new Stack<Transaction>();

    /**
     * Puts a transaction on top. Only recorded, active transactions are allowed.
     *
     * @throws IllegalArgumentException for null, unrecorded or undone transactions
     */
    public void push(Transaction transaction) {
        if (transaction == null || !transaction.isActive()) {
            throw new IllegalArgumentException("Only successfully recorded transactions can be added to the undo stack.");
        }
        stack.push(transaction);
    }

    /** Removes and returns the top transaction, or null if the stack is empty. */
    public Transaction pop() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    /** Returns the top transaction WITHOUT removing it, or null if the stack is empty. */
    public Transaction peek() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }
}
