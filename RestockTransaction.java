import java.math.BigDecimal;

/** A stock delivery; undo removes the delivered quantity. */
public class RestockTransaction extends Transaction {
    private final Product product;
    private final int quantity;

    public RestockTransaction(Product product, int quantity) throws StoreException {
        super(TransactionType.RESTOCK);
        if (product == null) {
            throw new IllegalArgumentException("A product must be selected.");
        }
        this.product = product;
        this.quantity = quantity;
        validate();
    }

    @Override
    protected void validate() throws StoreException {
        if (quantity <= 0 || quantity > Product.MAX_STOCK - product.getStock()) {
            throw new StoreException("The quantity must be positive and keep stock at or below " + Product.MAX_STOCK + ".");
        }
    }

    @Override
    protected void applyChanges() throws StoreException {
        product.addStock(quantity);
    }

    @Override
    protected void reverseChanges() throws StoreException {
        product.reverseRestock(quantity);
    }

    @Override
    public BigDecimal getAmount() {
        return Money.ZERO;
    }

    @Override
    public String getDetails() {
        return "Received " + quantity + " piece(s) of " + product.getName();
    }

    @Override
    public String getReceipt() {
        return "  RESTOCK RECORD\n"
                + receiptLine("Transaction no.", "#" + getNumber())
                + receiptLine("Product", product.getName())
                + receiptLine("Quantity received", String.valueOf(quantity))
                + receiptLine("Stock", String.valueOf(product.getStock()));
    }

    @Override
    public String getUndoResult() {
        return product.getName() + " stock is back to " + product.getStock() + ".";
    }
}
