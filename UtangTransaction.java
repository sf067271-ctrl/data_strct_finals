import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// A sale on credit (utang): the customer takes the items now and pays later.
// It also keeps its own payments and computes the amount paid, the remaining amount and PAID/UNPAID.
// Demonstrates INHERITANCE (extends SaleTransaction), POLYMORPHISM (overrides the template steps)
// and ENCAPSULATION (the payment list can only grow through a validated PaymentTransaction).
public class UtangTransaction extends SaleTransaction {
    private static final int NUMBER_WIDTH = 4;
    private static final int DATE_WIDTH = 10;
    private static final int CUSTOMER_WIDTH = 14;
    private static final int PRODUCT_WIDTH = 16;
    private static final int QUANTITY_WIDTH = 5;
    private static final int MONEY_WIDTH = 14;
    private static final String ROW_FORMAT = "%" + NUMBER_WIDTH + "s %-" + DATE_WIDTH + "s %-" + CUSTOMER_WIDTH
            + "s %-" + PRODUCT_WIDTH + "s %" + QUANTITY_WIDTH + "s %" + MONEY_WIDTH + "s %" + MONEY_WIDTH
            + "s %" + MONEY_WIDTH + "s  %s";
    public static final String TABLE_HEADER = String.format(Locale.US, ROW_FORMAT,
            "No.", "Date", "Customer", "Product", "Qty", "Total", "Paid", "Remaining", "Status");

    // Comparators used by the utang history (two stable merge sorts).
    public static final Comparator<UtangTransaction> BY_DATE =
            (first, second) -> first.getDate().compareTo(second.getDate());
    public static final Comparator<UtangTransaction> BY_CUSTOMER_NAME =
            (first, second) -> Entity.compareNameKeys(first.customer.getNameKey(), second.customer.getNameKey());

    private final Customer customer;
    private final CustomerRecords customerRecords;
    private final List<PaymentTransaction> payments = new ArrayList<>();

    // The customer may be new (not yet in customerRecords). A new customer is added only
    // when this transaction is saved, so cancelling leaves the customer list unchanged.
    public UtangTransaction(LocalDate date, Product product, int quantity, Customer customer,
            CustomerRecords customerRecords) {
        super(TransactionType.UTANG, date, product, quantity);
        this.customer = Objects.requireNonNull(customer, "customer");
        this.customerRecords = Objects.requireNonNull(customerRecords, "customerRecords");
    }

    public Customer getCustomer() {
        return customer;
    }

    // Read-only list of the payments made on this record (including undone ones).
    public List<PaymentTransaction> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    // Adds up the payments that were not undone.
    public BigDecimal getAmountPaid() {
        BigDecimal paid = Money.ZERO;
        for (PaymentTransaction payment : payments) {
            if (!payment.isUndone()) {
                paid = paid.add(payment.getAmount());
            }
        }
        return paid;
    }

    // Total minus the amount paid. Computed every time, so it always matches the payments.
    public BigDecimal getRemaining() {
        return getTotalAmount().subtract(getAmountPaid());
    }

    // PAID when nothing remains. Computed, not stored, so it can never be out of date.
    public boolean isPaid() {
        return Money.isZero(getRemaining());
    }

    // Returns true if at least one payment on this record is not undone.
    public boolean hasActivePayments() {
        for (PaymentTransaction payment : payments) {
            if (!payment.isUndone()) {
                return true;
            }
        }
        return false;
    }

    // Returns "PAID", "UNPAID" or "[UNDONE]".
    public String getPaymentStatus() {
        if (isUndone()) {
            return "[UNDONE]";
        }
        return isPaid() ? "PAID" : "UNPAID";
    }

    // One row of the utang history table.
    public String getHistoryRow() {
        return String.format(Locale.US, ROW_FORMAT, getNumber(), Validator.formatDate(getDate()),
                ConsoleFormat.fit(customer.getName(), CUSTOMER_WIDTH),
                ConsoleFormat.fit(getProduct().getName(), PRODUCT_WIDTH), getQuantity(),
                Money.format(getTotalAmount()), Money.format(getAmountPaid()),
                Money.format(getRemaining()), getPaymentStatus());
    }

    @Override
    public String describe() {
        return describeHeader()
                + describeSale()
                + ConsoleFormat.detailLine("Payment method", PaymentMethod.UTANG.getLabel())
                + ConsoleFormat.detailLine("Customer", customer.getName());
    }

    @Override
    public String describeCurrentState() {
        return ConsoleFormat.detailLine("Stock now", String.valueOf(getProduct().getStock()))
                + ConsoleFormat.detailLine("Status", getPaymentStatus())
                + ConsoleFormat.detailLine(customer.getName() + " balance", Money.format(customer.getBalance()));
    }

    @Override
    public String toString() {
        return "#" + getNumber() + " " + Validator.formatDate(getDate()) + " " + getProduct().getName()
                + " x" + getQuantity() + " (" + customer.getName() + ")";
    }

    @Override
    protected void validateExecution() throws StoreException {
        validateSale();
        Validator.checkUtangDate(getDate(), LocalDate.now());
        // A new customer must still have a unique name when it is finally added.
        if (!customerRecords.isRegistered(customer)) {
            customerRecords.checkNameAvailable(customer.getName());
        }
        BigDecimal newBalance = customer.getBalance().add(getTotalAmount());
        if (newBalance.compareTo(Money.MAX_BALANCE) > 0) {
            throw new ValidationException(customer.getName() + "'s balance would become " + Money.format(newBalance)
                    + ", above the limit of " + Money.format(Money.MAX_BALANCE) + ".");
        }
    }

    @Override
    protected void applyExecution() {
        // Step 1: take the items out of stock and add them to total sold.
        applySale();
        // Step 2: a new customer joins the customer list only now, when the sale is saved.
        if (!customerRecords.isRegistered(customer)) {
            customerRecords.register(customer);
        }
        // Step 3: add this record to the customer's history. Its status starts as UNPAID.
        // No cash is added: the customer has not paid yet.
        customer.addUtangRecord(this);
    }

    @Override
    protected void validateReversal() throws StoreException {
        // Payments belong to this record; undoing it first would leave them pointing at nothing.
        if (hasActivePayments()) {
            throw new UndoException("Cannot undo utang #" + getNumber()
                    + " because it still has payments. Undo those payments first.");
        }
        validateSaleReversal();
    }

    @Override
    protected void applyReversal() {
        // Step 1: put the STORED quantity back into stock.
        applySaleReversal();
        // Step 2: nothing else to change. Once reverse() marks this record undone, the customer's
        // computed balance skips it automatically. The record stays in history as [UNDONE].
    }

    // Package-private: only PaymentTransaction calls this, after its checks pass.
    void addPayment(PaymentTransaction payment) {
        payments.add(Objects.requireNonNull(payment, "payment"));
    }
}
