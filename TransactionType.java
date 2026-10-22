/**
 * The kinds of transaction that can be recorded and undone.
 */
public enum TransactionType {

    CASH_SALE("Cash sale"),
    UTANG("Utang"),
    PAYMENT("Payment"),
    RESTOCK("Restock");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    /** A readable name for tables and receipts, e.g. "Cash sale". */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
