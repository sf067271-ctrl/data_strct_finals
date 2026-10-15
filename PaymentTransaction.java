import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

// A payment on ONE specific utang record. Always dated today. Partial payments are allowed.
// Demonstrates INHERITANCE (extends Transaction) and POLYMORPHISM (overrides the template steps).
public class PaymentTransaction extends Transaction {
    private final UtangTransaction utangRecord;
    private final BigDecimal amount;
    private final SalesLedger ledger;

    public PaymentTransaction(LocalDate today, UtangTransaction utangRecord, BigDecimal amount,
            SalesLedger ledger) {
        super(TransactionType.PAYMENT, today);
        this.utangRecord = Objects.requireNonNull(utangRecord, "utangRecord");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
    }

    public UtangTransaction getUtangRecord() {
        return utangRecord;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String describe() {
        return describeHeader()
                + ConsoleFormat.detailLine("Customer", utangRecord.getCustomer().getName())
                + ConsoleFormat.detailLine("Pays utang record", "#" + utangRecord.getNumber() + " ("
                        + Validator.formatDate(utangRecord.getDate()) + ", " + utangRecord.getProduct().getName()
                        + " x" + utangRecord.getQuantity() + ")")
                + ConsoleFormat.detailLine("Record total", Money.format(utangRecord.getTotalAmount()))
                + ConsoleFormat.detailLine("Payment amount", Money.format(amount));
    }

    @Override
    public String describeCurrentState() {
        Customer customer = utangRecord.getCustomer();
        return ConsoleFormat.detailLine("Record paid so far", Money.format(utangRecord.getAmountPaid()))
                + ConsoleFormat.detailLine("Record remaining", Money.format(utangRecord.getRemaining()))
                + ConsoleFormat.detailLine("Status", utangRecord.getPaymentStatus())
                + ConsoleFormat.detailLine(customer.getName() + " balance", Money.format(customer.getBalance()));
    }

    @Override
    protected void validateExecution() throws StoreException {
        if (!utangRecord.isExecuted() || utangRecord.isUndone()) {
            throw new InvalidPaymentException("Utang record #" + utangRecord.getNumber() + " cannot accept payments.");
        }
        if (utangRecord.isPaid()) {
            throw new InvalidPaymentException("Utang record #" + utangRecord.getNumber() + " is already PAID.");
        }
        Money.checkPayment(amount, utangRecord.getRemaining());
    }

    @Override
    protected void applyExecution() {
        // Step 1: link the payment to its utang record. The record's paid and remaining amounts
        // (and the customer's balance) are computed from this list, so they update by themselves.
        utangRecord.addPayment(this);
        // Step 2: count the money as a collected utang payment, NOT as a cash sale.
        ledger.addPayment(amount);
    }

    @Override
    protected void validateReversal() throws StoreException {
        ledger.checkCanRemovePayment(amount);
    }

    @Override
    protected void applyReversal() {
        // Step 1: subtract the STORED amount from collected payments.
        ledger.removePayment(amount);
        // Step 2: once reverse() marks this payment undone, the record's paid amount skips it,
        // so the remaining amount goes back up (a PAID record becomes UNPAID again).
    }
}
