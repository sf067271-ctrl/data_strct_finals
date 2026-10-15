import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// A customer and their utang records.
// Demonstrates INHERITANCE (extends Entity), ENCAPSULATION (the record list is private and read-only
// outside) and POLYMORPHISM (overrides getSummaryLine() and toString()).
public class Customer extends Entity {
    private static final int NAME_WIDTH = 26;
    private static final int MONEY_WIDTH = 16;
    private static final int COUNT_WIDTH = 14;
    private static final String ROW_FORMAT = "%-" + NAME_WIDTH + "s %" + MONEY_WIDTH + "s %" + COUNT_WIDTH + "s";
    public static final String TABLE_HEADER =
            String.format(Locale.US, ROW_FORMAT, "Customer", "Balance", "Unpaid records");

    private final List<UtangTransaction> utangRecords = new ArrayList<>();

    // The name must already be validated by CustomerRecords.
    public Customer(String name) {
        super(name);
    }

    // The balance is COMPUTED from the records instead of being stored in a field. A stored
    // balance could drift away from the records (for example after an undo); this cannot.
    public BigDecimal getBalance() {
        BigDecimal outstandingBalance = Money.ZERO;
        for (UtangTransaction record : utangRecords) {
            if (!record.isUndone()) {
                outstandingBalance = outstandingBalance.add(record.getRemaining());
            }
        }
        return outstandingBalance;
    }

    // Read-only list of every utang record, including undone ones.
    public List<UtangTransaction> getUtangRecords() {
        return Collections.unmodifiableList(utangRecords);
    }

    // Returns the records that still have something to pay, oldest date first.
    public List<UtangTransaction> getUnpaidRecords() {
        List<UtangTransaction> unpaid = new ArrayList<>();
        for (UtangTransaction record : utangRecords) {
            if (HistoryFilter.UNPAID.matches(record)) {
                unpaid.add(record);
            }
        }
        Algorithms.mergeSort(unpaid, UtangTransaction.BY_DATE);
        return unpaid;
    }

    // Same as getUnpaidRecords(), but throws if there is nothing to pay.
    public List<UtangTransaction> requireUnpaidRecords() throws InvalidPaymentException {
        List<UtangTransaction> unpaid = getUnpaidRecords();
        if (unpaid.isEmpty()) {
            throw new InvalidPaymentException(getName() + " has no unpaid utang records. Nothing to pay.");
        }
        return unpaid;
    }

    @Override
    public String getSummaryLine() {
        return String.format(Locale.US, ROW_FORMAT, ConsoleFormat.fit(getName(), NAME_WIDTH),
                Money.format(getBalance()), String.valueOf(getUnpaidRecords().size()));
    }

    @Override
    public String toString() {
        return getName() + " (balance " + Money.format(getBalance()) + ")";
    }

    // Package-private: only UtangTransaction.applyExecution() calls this, after its checks pass.
    void addUtangRecord(UtangTransaction record) {
        utangRecords.add(Objects.requireNonNull(record, "record"));
    }
}
