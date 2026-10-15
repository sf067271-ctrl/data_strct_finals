import java.math.BigDecimal;
import java.util.Objects;

// Session money totals. Cash sales and collected utang payments are kept SEPARATE,
// because an utang payment is money for an old sale, not a new sale.
// Demonstrates ENCAPSULATION: totals change only through checked methods.
public class SalesLedger {
    private BigDecimal cashSalesTotal = Money.ZERO;
    private BigDecimal paymentsCollectedTotal = Money.ZERO;

    public BigDecimal getCashSalesTotal() {
        return cashSalesTotal;
    }

    public BigDecimal getPaymentsCollectedTotal() {
        return paymentsCollectedTotal;
    }

    // Throws if removing this cash sale would make the cash-sales total negative.
    public void checkCanRemoveCashSale(BigDecimal amount) throws UndoException {
        if (amount.compareTo(cashSalesTotal) > 0) {
            throw new UndoException("Cannot undo: cash sales total would become negative.");
        }
    }

    // Throws if removing this payment would make the collected-payments total negative.
    public void checkCanRemovePayment(BigDecimal amount) throws UndoException {
        if (amount.compareTo(paymentsCollectedTotal) > 0) {
            throw new UndoException("Cannot undo: collected payments total would become negative.");
        }
    }

    // Package-private: only CashSaleTransaction calls these, after its checks pass.
    void addCashSale(BigDecimal amount) {
        cashSalesTotal = cashSalesTotal.add(requirePositive(amount));
    }

    void removeCashSale(BigDecimal amount) {
        cashSalesTotal = subtractWithoutGoingNegative(cashSalesTotal, amount);
    }

    // Package-private: only PaymentTransaction calls these, after its checks pass.
    void addPayment(BigDecimal amount) {
        paymentsCollectedTotal = paymentsCollectedTotal.add(requirePositive(amount));
    }

    void removePayment(BigDecimal amount) {
        paymentsCollectedTotal = subtractWithoutGoingNegative(paymentsCollectedTotal, amount);
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (!Money.isPositive(Objects.requireNonNull(amount, "amount"))) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        return amount;
    }

    private static BigDecimal subtractWithoutGoingNegative(BigDecimal total, BigDecimal amount) {
        BigDecimal newTotal = total.subtract(requirePositive(amount));
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Total would become negative; the check was skipped.");
        }
        return newTotal;
    }
}
