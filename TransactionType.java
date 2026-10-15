// The three kinds of transactions, with a readable label for the screen.
// Demonstrates ENCAPSULATION: each constant carries its own label.
public enum TransactionType {
    CASH_SALE("Cash sale"),
    UTANG("Utang"),
    PAYMENT("Utang payment");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
