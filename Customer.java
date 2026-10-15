import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer who may buy on credit (utang).
 *
 * OOP - ENCAPSULATION:
 *   The balance and history are private. The balance can only change through
 *   addDebt() and reduceDebt(), which check the amount first, so it can never
 *   become negative. getHistory() returns a COPY, so no other class can
 *   delete or rearrange the history.
 *
 * OOP - ABSTRACTION / POLYMORPHISM:
 *   Implements Nameable, so customers are searched and sorted with the same
 *   algorithms as products.
 */
public class Customer implements Nameable {

    private final String name;
    private BigDecimal balance;
    private final ArrayList<Transaction> history;

    /**
     * Creates a customer with a zero balance and an empty history.
     *
     * @throws IllegalArgumentException if the name breaks the name rules
     */
    public Customer(String name) {
        String problem = TextRules.checkName(name);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        this.name = TextRules.clean(name);
        this.balance = Money.ZERO;
        this.history = new ArrayList<Transaction>();
    }

    @Override
    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /** True if the customer still owes money. */
    public boolean hasOutstandingUtang() {
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Returns a COPY of the history (oldest first), so it cannot be changed from outside. */
    public List<Transaction> getHistory() {
        return new ArrayList<Transaction>(history);
    }

    // =====================================================================
    //  Changes made by transactions (package-private: not for the menus)
    // =====================================================================

    /** Increases the balance (utang recorded, or a payment undone). */
    void addDebt(BigDecimal amount) {
        if (!Money.isPositive(amount)) {
            throw new IllegalArgumentException("The amount added to a balance must be greater than zero.");
        }
        balance = balance.add(amount);
    }

    /**
     * Decreases the balance (payment recorded, or an utang undone).
     *
     * @throws StoreException if the amount is bigger than the balance
     */
    void reduceDebt(BigDecimal amount) throws StoreException {
        if (!Money.isPositive(amount)) {
            throw new StoreException("The amount must be greater than zero.");
        }
        if (amount.compareTo(balance) > 0) {
            throw new StoreException("The amount " + Money.format(amount) + " is more than "
                    + name + "'s balance of " + Money.format(balance) + ".");
        }
        balance = balance.subtract(amount);
    }

    /** Adds an utang or payment to the end of the history. */
    void addHistory(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("A history entry cannot be empty.");
        }
        history.add(transaction);
    }

    /** OOP - POLYMORPHISM: overrides Object.toString(). */
    @Override
    public String toString() {
        return name + " (balance " + Money.format(balance) + ")";
    }
}
