import java.time.LocalDate;
import java.util.Objects;

// A sale paid in cash right away. Always dated today.
// Demonstrates INHERITANCE (extends SaleTransaction) and POLYMORPHISM (overrides the template steps).
public class CashSaleTransaction extends SaleTransaction {
    private final SalesLedger ledger;

    public CashSaleTransaction(LocalDate today, Product product, int quantity, SalesLedger ledger) {
        super(TransactionType.CASH_SALE, today, product, quantity);
        this.ledger = Objects.requireNonNull(ledger, "ledger");
    }

    @Override
    public String describe() {
        return describeHeader()
                + describeSale()
                + ConsoleFormat.detailLine("Payment method", PaymentMethod.CASH.getLabel());
    }

    @Override
    public String describeCurrentState() {
        return ConsoleFormat.detailLine("Stock now", String.valueOf(getProduct().getStock()))
                + ConsoleFormat.detailLine("Cash sales total", Money.format(ledger.getCashSalesTotal()));
    }

    @Override
    protected void validateExecution() throws StoreException {
        validateSale();
    }

    @Override
    protected void applyExecution() {
        // Step 1: take the items out of stock and add them to total sold.
        applySale();
        // Step 2: the money was paid now, so it counts as a cash sale.
        ledger.addCashSale(getTotalAmount());
    }

    @Override
    protected void validateReversal() throws StoreException {
        validateSaleReversal();
        ledger.checkCanRemoveCashSale(getTotalAmount());
    }

    @Override
    protected void applyReversal() {
        // Step 1: put the STORED quantity back into stock.
        applySaleReversal();
        // Step 2: subtract the STORED total (not today's price x quantity) from cash sales.
        ledger.removeCashSale(getTotalAmount());
    }
}
