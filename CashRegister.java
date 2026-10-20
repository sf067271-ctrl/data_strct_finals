import java.math.BigDecimal;

/**
 * Keeps the total of CASH SALES for the current session.
 *
 * Utang sales and utang payments are NOT added here: utang is money not yet
 * received, and payments are tracked on each customer's balance.
 *
 * OOP - ENCAPSULATION: the total is private and can only change through
 * addSale() and removeSale(), which check the amount first.
 */
public class CashRegister {

    private BigDecimal cashSales = Money.ZERO;

    public BigDecimal getTotal() {
        return cashSales;
    }

    /** Adds the amount of a recorded cash sale. */
    void addSale(BigDecimal amount) {
        if (!Money.isPositive(amount)) {
            throw new IllegalArgumentException("A cash sale amount must be greater than zero.");
        }
        cashSales = cashSales.add(amount);
    }

    /** True if this amount can be subtracted without going below zero. */
    boolean canRemoveSale(BigDecimal amount) {
        return Money.isPositive(amount) && amount.compareTo(cashSales) <= 0;
    }

    /**
     * Subtracts the amount of an undone cash sale.
     *
     * @throws StoreException if the total would become negative
     */
    void removeSale(BigDecimal amount) throws StoreException {
        if (!canRemoveSale(amount)) {
            throw new StoreException("Cannot subtract " + Money.format(amount)
                    + " from cash sales of " + Money.format(cashSales) + ".");
        }
        cashSales = cashSales.subtract(amount);
    }
}
