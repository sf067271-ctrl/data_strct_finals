package sarisari.transactions;

import java.math.BigDecimal;

import sarisari.exceptions.StoreException;
import sarisari.models.Customer;
import sarisari.utils.Money;

/**
 * A customer paying part or all of their utang.
 *
 * Record: customer balance DOWN, entry added to history.
 *         (Cash sales do NOT change: payments are kept separate.)
 * Undo:   customer balance UP by the original payment; the history entry
 *         stays but is shown as UNDONE.
 *
 * OOP - INHERITANCE: extends Transaction directly (no product involved).
 * OOP - POLYMORPHISM: overrides getBalanceEffect() to return -amount.
 */
public class PaymentTransaction extends Transaction {

    private final Customer customer;
    private final BigDecimal amount;

    /**
     * @throws StoreException if the amount is not allowed for this customer
     */
    public PaymentTransaction(Customer customer, BigDecimal amount) throws StoreException {
        super(TransactionType.PAYMENT);
        if (customer == null) {
            throw new IllegalArgumentException("A customer must be selected.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("A payment amount is required.");
        }
        this.customer = customer;
        this.amount = amount;
        checkPaymentRules();   // reject a bad payment as early as possible
    }

    /** Checked again just before recording, in case the balance changed. */
    @Override
    protected void validate() throws StoreException {
        checkPaymentRules();
    }

    /**
     * The payment rules:
     *   - the customer must have an outstanding utang,
     *   - the amount must be greater than zero,
     *   - it can have at most two decimal places,
     *   - it cannot exceed the outstanding balance.
     * (private, so it cannot be overridden and is safe to call from the constructor)
     */
    private void checkPaymentRules() throws StoreException {
        if (!customer.hasOutstandingUtang()) {
            throw new StoreException(customer.getName() + " has no outstanding utang. Payment rejected.");
        }
        if (!Money.isPositive(amount)) {
            throw new StoreException("The payment must be greater than zero.");
        }
        if (!Money.hasAtMostTwoDecimals(amount)) {
            throw new StoreException("The payment can have at most two decimal places.");
        }
        if (amount.compareTo(customer.getBalance()) > 0) {
            throw new StoreException("Overpayment rejected: " + Money.format(amount) + " is more than "
                    + customer.getName() + "'s balance of " + Money.format(customer.getBalance()) + ".");
        }
    }

    @Override
    protected void applyChanges() throws StoreException {
        customer.reduceDebt(amount);
        customer.addHistory(this);
    }

    @Override
    protected void reverseChanges() {
        customer.addDebt(amount);   // adding a positive amount cannot fail
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public BigDecimal getBalanceEffect() {
        return amount.negate();   // a payment decreases the balance
    }

    @Override
    public String getDetails() {
        return "Payment from " + customer.getName();
    }

    @Override
    public String getHistoryDetails() {
        return "Payment received";
    }

    @Override
    public String getReceipt() {
        return "  ------------ PAYMENT RECEIPT ------------\n"
                + receiptLine("Transaction no.", "#" + getNumber())
                + receiptLine("Date and time", getRecordedAtText())
                + receiptLine("Customer", customer.getName())
                + receiptLine("Amount paid", Money.format(amount))
                + receiptLine("Balance left", Money.format(customer.getBalance()))
                + "  -----------------------------------------";
    }

    @Override
    public String getUndoResult() {
        return customer.getName() + "'s balance is back to " + Money.format(customer.getBalance()) + ".";
    }
}
