import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

// Abstract middle class for transactions that take items from stock (cash sale and utang).
// It keeps the product, quantity, unit price and total so the two subclasses do not repeat them.
// Demonstrates INHERITANCE (extends Transaction; CashSaleTransaction and UtangTransaction extend it)
// and ENCAPSULATION (the original values are final and can never change).
public abstract class SaleTransaction extends Transaction {
    private final Product product;
    private final int quantity;
    // The price at the moment of the sale. Undo uses these stored values, never the current price.
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;

    protected SaleTransaction(TransactionType type, LocalDate date, Product product, int quantity) {
        super(type, date);
        this.product = Objects.requireNonNull(product, "product");
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.totalAmount = Money.multiply(unitPrice, quantity);
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    // Shared save checks: valid quantity, enough stock, and a total within the limit.
    protected void validateSale() throws StoreException {
        Validator.checkQuantity(quantity);
        product.checkCanSell(quantity);
        if (totalAmount.compareTo(Money.MAX_TRANSACTION_TOTAL) > 0) {
            throw new ValidationException("Total of " + Money.format(totalAmount) + " is above the limit of "
                    + Money.format(Money.MAX_TRANSACTION_TOTAL) + " for one transaction.");
        }
    }

    // Shared save step: take the items out of stock and add them to total sold.
    protected void applySale() {
        product.recordSale(quantity);
    }

    // Shared undo check: stock and total sold must stay valid.
    protected void validateSaleReversal() throws UndoException {
        product.checkCanRestore(quantity);
    }

    // Shared undo step: put the items back into stock using the STORED quantity.
    protected void applySaleReversal() {
        product.restoreSale(quantity);
    }

    // Shared product lines for describe().
    protected String describeSale() {
        return ConsoleFormat.detailLine("Product", product.getCode() + " - " + product.getName())
                + ConsoleFormat.detailLine("Quantity", String.valueOf(quantity))
                + ConsoleFormat.detailLine("Unit price", Money.format(unitPrice))
                + ConsoleFormat.detailLine("Total", Money.format(totalAmount));
    }
}
