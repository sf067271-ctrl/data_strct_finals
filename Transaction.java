import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A completed (or about-to-be-completed) store transaction.
 *
 * OOP - ABSTRACTION (abstract class):
 *   "Transaction" by itself is an idea, not something you can record. You can
 *   only create a CashSaleTransaction, UtangTransaction or PaymentTransaction.
 *   This class defines WHAT every transaction can do (execute, undo, describe
 *   itself) and leaves HOW to each subclass through abstract methods.
 *
 * OOP - INHERITANCE:
 *
 *       Transaction (abstract)
 *         ├── ProductTransaction (abstract)   product, quantity, unit price, total
 *         │     ├── CashSaleTransaction
 *         │     └── UtangTransaction
 *         └── PaymentTransaction
 *
 *   Shared fields and logic (number, type, date, undo status) are written
 *   once here and inherited by all subclasses.
 *
 * OOP - POLYMORPHISM:
 *   Main and UndoStack only work with the type "Transaction". When Main calls
 *   transaction.undo(), Java automatically runs the correct reverseChanges()
 *   of the actual object: restoring stock for a sale, lowering a balance for
 *   an utang, or raising a balance for a payment. No if/else on the type is
 *   needed.
 *
 * TEMPLATE METHOD:
 *   execute() and undo() are "final": every transaction follows the same safe
 *   steps (check first, then change), while the subclass fills in the details.
 */
public abstract class Transaction {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static int nextNumber = 1;   // shared counter for transaction numbers

    private final TransactionType type;
    private int number;                  // assigned when the transaction is recorded
    private LocalDateTime recordedAt;
    private boolean executed;
    private boolean undone;

    protected Transaction(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("A transaction type is required.");
        }
        this.type = type;
    }

    // =====================================================================
    //  Template methods: the same safe steps for every transaction
    // =====================================================================

    /**
     * Records the transaction.
     *   1. validate()     - checks EVERYTHING first; throws if something is wrong
     *   2. applyChanges() - only then changes stock, balances and totals
     * If step 1 fails, nothing has been changed.
     *
     * @throws StoreException if the transaction is not allowed
     */
    public final void execute() throws StoreException {
        if (executed) {
            throw new StoreException("This transaction has already been recorded.");
        }
        validate();
        applyChanges();
        executed = true;
        number = nextNumber;
        nextNumber++;
        recordedAt = LocalDateTime.now();
    }

    /**
     * Reverses the transaction. reverseChanges() also checks first and only
     * then changes anything, so a failed undo leaves everything as it was.
     * The transaction stays in the customer's history, marked UNDONE.
     *
     * @throws StoreException if it was never recorded, was already undone,
     *                        or cannot be reversed safely
     */
    public final void undo() throws StoreException {
        if (!executed) {
            throw new StoreException("This transaction was never recorded, so it cannot be undone.");
        }
        if (undone) {
            throw new StoreException("Transaction #" + number + " has already been undone.");
        }
        reverseChanges();
        undone = true;
    }

    // =====================================================================
    //  Abstract methods: each subclass decides HOW
    // =====================================================================

    /** Checks that the transaction can be recorded right now. */
    protected abstract void validate() throws StoreException;

    /** Changes the records. Called only after validate() succeeds. */
    protected abstract void applyChanges() throws StoreException;

    /** Undoes applyChanges(), using the values stored in this transaction. */
    protected abstract void reverseChanges() throws StoreException;

    /** The money involved: sale total, utang total, or payment amount. */
    public abstract BigDecimal getAmount();

    /** Short description, e.g. "3 x Milo Sachet @ PHP 9.00". */
    public abstract String getDetails();

    /**
     * Description used in a customer's history table, where the customer's
     * name is already shown above. By default the same as getDetails();
     * subclasses may OVERRIDE it with a shorter text.
     */
    public String getHistoryDetails() {
        return getDetails();
    }

    /** Lines of a readable receipt, shown after recording. */
    public abstract String getReceipt();

    /** What changed after an undo, e.g. "Milo Sachet stock is back to 40." */
    public abstract String getUndoResult();

    /**
     * How this transaction changes a customer's balance:
     * positive for utang, negative for payment, zero otherwise.
     * Subclasses OVERRIDE this (polymorphism); the default is zero.
     */
    public BigDecimal getBalanceEffect() {
        return Money.ZERO;
    }

    // =====================================================================
    //  Common getters
    // =====================================================================

    public TransactionType getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public boolean isUndone() {
        return undone;
    }

    /** Active = recorded and not undone. Only active transactions go on the undo stack. */
    public boolean isActive() {
        return executed && !undone;
    }

    public String getStatus() {
        if (!executed) {
            return "NOT RECORDED";
        }
        return undone ? "UNDONE" : "ACTIVE";
    }

    public String getRecordedAtText() {
        return recordedAt == null ? "-" : recordedAt.format(DATE_FORMAT);
    }

    /** One-line summary, e.g. "#3 Payment | Payment from Aling Nena | PHP 10.00 | 2026-10-15 13:05". */
    public String getSummary() {
        return "#" + number + " " + type.getLabel() + " | " + getDetails() + " | "
                + Money.format(getAmount()) + " | " + getRecordedAtText();
    }

    /** Formats one receipt line, e.g. "  Quantity        : 3". Used by subclasses. */
    protected static String receiptLine(String label, String value) {
        return String.format("  %-16s: %s%n", label, value);
    }

    /** OOP - POLYMORPHISM: overrides Object.toString(). */
    @Override
    public String toString() {
        return getSummary() + " [" + getStatus() + "]";
    }
}
