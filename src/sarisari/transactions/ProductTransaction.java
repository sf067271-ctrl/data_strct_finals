package sarisari.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;

import sarisari.exceptions.InsufficientStockException;
import sarisari.exceptions.StoreException;
import sarisari.models.Product;
import sarisari.utils.Money;

/**
 * A transaction that sells ONE product in some quantity: the parent of
 * CashSaleTransaction and UtangTransaction.
 *
 * OOP - INHERITANCE:
 *   Cash sales and utang both need a product, a quantity, the unit price and
 *   the total. Instead of writing these fields twice, both classes inherit
 *   them from here. This class is still ABSTRACT because it does not know
 *   what to do with the money (cash register or customer balance).
 *
 * The unit price and total are SAVED when the transaction is created, so an
 * undo uses the original amount even if the product's price changes later.
 */
public abstract class ProductTransaction extends Transaction {

    private final Product product;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal total;

    /**
     * @throws StoreException             if the quantity is not positive
     * @throws InsufficientStockException if there are not enough pieces now
     */
    protected ProductTransaction(TransactionType type, Product product, int quantity) throws StoreException {
        super(type);   // calls the Transaction constructor (inheritance)
        if (product == null) {
            throw new IllegalArgumentException("A product must be selected.");
        }
        if (quantity <= 0) {
            throw new StoreException("The quantity must be at least 1.");
        }
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        // total amount = unit price x quantity
        this.total = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
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

    public BigDecimal getTotal() {
        return total;
    }

    @Override
    public BigDecimal getAmount() {
        return total;
    }

    /** Stock may have changed since the transaction was prepared, so check again. */
    @Override
    protected void validate() throws StoreException {
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
    }

    /** Used by subclasses before undoing: makes sure the stock can be restored. */
    protected void checkStockCanBeRestored() throws StoreException {
        if (!product.canReverseSale(quantity)) {
            throw new StoreException("Cannot undo: the stock and sales of " + product.getName()
                    + " no longer match this transaction.");
        }
    }

    @Override
    public String getDetails() {
        return quantity + " x " + product.getName() + " @ " + Money.format(unitPrice);
    }
}
