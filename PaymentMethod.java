// How the customer pays in "Add transaction": 1 = Cash, 2 = Utang (pay later).
// Demonstrates ENCAPSULATION: the menu number and label live with each constant.
public enum PaymentMethod {
    CASH(1, "Cash"),
    UTANG(2, "Utang");

    private final int menuNumber;
    private final String label;

    PaymentMethod(int menuNumber, String label) {
        this.menuNumber = menuNumber;
        this.label = label;
    }

    public int getMenuNumber() {
        return menuNumber;
    }

    public String getLabel() {
        return label;
    }

    // Returns the method with this menu number. The caller must pass a valid number.
    public static PaymentMethod fromMenuNumber(int menuNumber) {
        for (PaymentMethod method : values()) {
            if (method.menuNumber == menuNumber) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method number: " + menuNumber);
    }
}
