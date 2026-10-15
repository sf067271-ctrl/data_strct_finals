/**
 * A sale paid in cash.
 *
 * Record: stock DOWN, total sold UP, cash sales UP.
 * Undo:   stock UP,   total sold DOWN, cash sales DOWN (by the ORIGINAL total).
 *
 * OOP - INHERITANCE: extends ProductTransaction (which extends Transaction).
 * OOP - POLYMORPHISM: overrides applyChanges(), reverseChanges(), getReceipt()
 *                     and getUndoResult() with cash-sale behaviour.
 */
public class CashSaleTransaction extends ProductTransaction {

    private final CashRegister register;

    public CashSaleTransaction(Product product, int quantity, CashRegister register) throws StoreException {
        super(TransactionType.CASH_SALE, product, quantity);
        if (register == null) {
            throw new IllegalArgumentException("A cash register is required for a cash sale.");
        }
        this.register = register;
    }

    @Override
    protected void applyChanges() throws StoreException {
        getProduct().sell(getQuantity());   // checked by validate(), so this cannot fail halfway
        register.addSale(getTotal());
    }

    @Override
    protected void reverseChanges() throws StoreException {
        // 1. Check everything first...
        checkStockCanBeRestored();
        if (!register.canRemoveSale(getTotal())) {
            throw new StoreException("Cannot undo: cash sales are lower than this sale's total.");
        }
        // 2. ...then change the records.
        getProduct().reverseSale(getQuantity());
        register.removeSale(getTotal());
    }

    @Override
    public String getReceipt() {
        return "  ----------- CASH SALE RECEIPT -----------\n"
                + receiptLine("Transaction no.", "#" + getNumber())
                + receiptLine("Date and time", getRecordedAtText())
                + receiptLine("Product", getProduct().getName())
                + receiptLine("Quantity", String.valueOf(getQuantity()))
                + receiptLine("Unit price", Money.format(getUnitPrice()))
                + receiptLine("TOTAL", Money.format(getTotal()))
                + receiptLine("Stock left", String.valueOf(getProduct().getStock()))
                + "  -----------------------------------------";
    }

    @Override
    public String getUndoResult() {
        return getProduct().getName() + " stock is back to " + getProduct().getStock()
                + ". Cash sales are now " + Money.format(register.getTotal()) + ".";
    }
}
