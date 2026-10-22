package sarisari.transactions;

import java.math.BigDecimal;

import sarisari.exceptions.StoreException;
import sarisari.models.Customer;
import sarisari.models.Product;
import sarisari.utils.Money;

/**
 * A sale on credit (utang): the customer takes the item now and pays later.
 *
 * Record: stock DOWN, total sold UP, customer balance UP, entry added to history.
 *         (Cash sales do NOT change, because no cash was received.)
 * Undo:   stock UP, total sold DOWN, customer balance DOWN; the history entry
 *         stays but is shown as UNDONE.
 *
 * OOP - INHERITANCE: extends ProductTransaction (which extends Transaction).
 * OOP - POLYMORPHISM: overrides getBalanceEffect() to return +total.
 */
public class UtangTransaction extends ProductTransaction {

    private final Customer customer;

    public UtangTransaction(Customer customer, Product product, int quantity) throws StoreException {
        super(TransactionType.UTANG, product, quantity);
        if (customer == null) {
            throw new IllegalArgumentException("A customer must be selected.");
        }
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    protected void applyChanges() throws StoreException {
        getProduct().sell(getQuantity());
        customer.addDebt(getTotal());
        customer.addHistory(this);
    }

    @Override
    protected void reverseChanges() throws StoreException {
        // 1. Check everything first...
        checkStockCanBeRestored();
        if (getTotal().compareTo(customer.getBalance()) > 0) {
            throw new StoreException("Cannot undo: " + customer.getName()
                    + "'s balance is lower than this utang.");
        }
        // 2. ...then change the records.
        getProduct().reverseSale(getQuantity());
        customer.reduceDebt(getTotal());
    }

    @Override
    public BigDecimal getBalanceEffect() {
        return getTotal();   // utang increases the balance
    }

    @Override
    public String getDetails() {
        return customer.getName() + ": " + super.getDetails();   // reuses the parent's description
    }

    /** In the customer's own history, the name is not repeated. */
    @Override
    public String getHistoryDetails() {
        return super.getDetails();   // e.g. "2 x Egg @ PHP 10.00"
    }

    @Override
    public String getReceipt() {
        return "  ------------- UTANG RECORD --------------\n"
                + receiptLine("Transaction no.", "#" + getNumber())
                + receiptLine("Date and time", getRecordedAtText())
                + receiptLine("Customer", customer.getName())
                + receiptLine("Product", getProduct().getName())
                + receiptLine("Quantity", String.valueOf(getQuantity()))
                + receiptLine("Unit price", Money.format(getUnitPrice()))
                + receiptLine("TOTAL UTANG", Money.format(getTotal()))
                + receiptLine("New balance", Money.format(customer.getBalance()))
                + receiptLine("Stock left", String.valueOf(getProduct().getStock()))
                + "  -----------------------------------------";
    }

    @Override
    public String getUndoResult() {
        return customer.getName() + "'s balance is now " + Money.format(customer.getBalance())
                + ". " + getProduct().getName() + " stock is back to " + getProduct().getStock() + ".";
    }
}
