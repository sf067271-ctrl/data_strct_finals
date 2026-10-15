import java.time.LocalDate;
import java.util.Objects;

// Abstract parent of every transaction. Holds the shared data (number, date, type, undone flag)
// and the TEMPLATE for execute() and reverse(): always validate first, then apply.
// Demonstrates ABSTRACTION (abstract steps), INHERITANCE (three subclasses) and
// POLYMORPHISM (Main calls execute() and reverse() without knowing the subclass).
public abstract class Transaction implements Reversible {
    // The number the next saved transaction will get. It only moves forward after a successful
    // execute(), so a cancelled transaction does not leave a gap in the numbering.
    private static int nextNumber = 1;

    private final int number;
    private final LocalDate date;
    private final TransactionType type;
    private boolean executed;
    private boolean undone;

    protected Transaction(TransactionType type, LocalDate date) {
        this.type = Objects.requireNonNull(type, "type");
        this.date = Objects.requireNonNull(date, "date");
        this.number = nextNumber;
    }

    // TEMPLATE METHOD for saving. It is final so no subclass can skip a step.
    @Override
    public final void execute() throws StoreException {
        // Step 1: a transaction can be saved only once, and only the newest one may be saved.
        if (executed) {
            throw new IllegalStateException("Transaction #" + number + " was already saved.");
        }
        if (number != nextNumber) {
            throw new IllegalStateException("Transaction #" + number + " is out of date.");
        }
        // Step 2: run every check. If one fails it throws, and NOTHING has been changed yet.
        validateExecution();
        // Step 3: all checks passed, so apply the change. applyExecution() never fails.
        applyExecution();
        // Step 4: remember that it is saved and reserve the next transaction number.
        executed = true;
        nextNumber++;
    }

    // TEMPLATE METHOD for undo. It is final so no subclass can skip a step.
    @Override
    public final void reverse() throws StoreException {
        // Step 1: only a saved transaction that is not yet undone can be undone.
        if (!executed) {
            throw new IllegalStateException("Transaction #" + number + " was never saved.");
        }
        if (undone) {
            throw new UndoException("Transaction #" + number + " is already undone.");
        }
        // Step 2: run every check first. If one fails, nothing has been changed.
        validateReversal();
        // Step 3: all checks passed, so undo the change. applyReversal() never fails.
        applyReversal();
        // Step 4: this is the ONLY place where the undone flag changes.
        undone = true;
    }

    // Runs the save checks without changing anything, so problems show before the operator confirms.
    public final void checkCanExecute() throws StoreException {
        validateExecution();
    }

    @Override
    public boolean isUndone() {
        return undone;
    }

    public boolean isExecuted() {
        return executed;
    }

    public int getNumber() {
        return number;
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }

    // Returns PENDING (not saved yet), SAVED or UNDONE.
    public String getStatusLabel() {
        if (undone) {
            return "UNDONE";
        }
        return executed ? "SAVED" : "PENDING (not saved yet)";
    }

    // Returns the full details of this transaction, one item per line.
    public abstract String describe();

    // Returns the live values this transaction affects (stock, balance, remaining amount).
    public abstract String describeCurrentState();

    // Checks that saving is allowed. Must throw a StoreException on any problem and change nothing.
    protected abstract void validateExecution() throws StoreException;

    // Applies the change. Called only after validateExecution() passed, so it must not fail.
    protected abstract void applyExecution();

    // Checks that undo is allowed. Must throw a StoreException on any problem and change nothing.
    protected abstract void validateReversal() throws StoreException;

    // Undoes the change. Called only after validateReversal() passed, so it must not fail.
    protected abstract void applyReversal();

    // Shared first lines of describe(), used by every subclass.
    protected String describeHeader() {
        return ConsoleFormat.detailLine("Transaction no.", String.valueOf(number))
                + ConsoleFormat.detailLine("Date", Validator.formatDate(date))
                + ConsoleFormat.detailLine("Type", type.getLabel())
                + ConsoleFormat.detailLine("Transaction status", getStatusLabel());
    }
}
