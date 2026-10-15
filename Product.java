import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

// A product sold in the store: code, price, stock and total quantity sold.
// Demonstrates INHERITANCE (extends Entity), ENCAPSULATION (private fields, no public setters)
// and POLYMORPHISM (overrides getSummaryLine() and toString()).
public class Product extends Entity {
    private static final int CODE_WIDTH = 12;
    private static final int NAME_WIDTH = 26;
    private static final int MONEY_WIDTH = 14;
    private static final int NUMBER_WIDTH = 8;
    private static final String ROW_FORMAT = "%-" + CODE_WIDTH + "s %-" + NAME_WIDTH + "s %"
            + MONEY_WIDTH + "s %" + NUMBER_WIDTH + "s %" + NUMBER_WIDTH + "s";
    public static final String TABLE_HEADER =
            String.format(Locale.US, ROW_FORMAT, "Code", "Name", "Price", "Stock", "Sold");

    private final String code;
    private BigDecimal price;
    private int stock;
    private int totalSold;

    // Values must already be validated by Inventory.addProduct().
    public Product(String code, String name, BigDecimal price, int stock) {
        super(name);
        this.code = Objects.requireNonNull(code, "code");
        this.price = Objects.requireNonNull(price, "price");
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        this.stock = stock;
        this.totalSold = 0;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public int getTotalSold() {
        return totalSold;
    }

    // Returns true if there is no stock left.
    public boolean isOutOfStock() {
        return stock == 0;
    }

    // Returns what the stock would be after selling this quantity (used for previews only).
    public int getStockAfterSelling(int quantity) {
        return stock - quantity;
    }

    // Throws if the product cannot be sold at all.
    public void checkInStock() throws InsufficientStockException {
        if (isOutOfStock()) {
            throw new InsufficientStockException(getName() + " is OUT OF STOCK.");
        }
    }

    // Checks that this quantity can be sold. Changes nothing.
    public void checkCanSell(int quantity) throws InsufficientStockException, ValidationException {
        checkInStock();
        if (quantity > stock) {
            throw new InsufficientStockException("Not enough stock for " + getName() + ". Requested: "
                    + quantity + ", available: " + stock + ".");
        }
        if ((long) totalSold + quantity > Integer.MAX_VALUE) {
            throw new ValidationException("Total sold for " + getName() + " would become too large.");
        }
    }

    // Checks that a sale of this quantity can be reversed. Changes nothing.
    public void checkCanRestore(int quantity) throws UndoException {
        if (quantity > totalSold) {
            throw new UndoException("Cannot undo: total sold for " + getName() + " would become negative.");
        }
        if ((long) stock + quantity > Integer.MAX_VALUE) {
            throw new UndoException("Cannot undo: stock for " + getName() + " would become too large.");
        }
    }

    @Override
    public String getSummaryLine() {
        return String.format(Locale.US, ROW_FORMAT, code, ConsoleFormat.fit(getName(), NAME_WIDTH),
                Money.format(price), stockText(), String.valueOf(totalSold));
    }

    @Override
    public String toString() {
        return code + " - " + getName() + " (" + Money.format(price) + ", stock " + stock + ")";
    }

    // Package-private (no modifier): only transactions call this, and only after checkCanSell().
    // New values are computed first and then assigned together.
    void recordSale(int quantity) {
        if (quantity <= 0 || quantity > stock) {
            throw new IllegalStateException("recordSale() called without a successful checkCanSell().");
        }
        int newStock = stock - quantity;
        int newTotalSold = Math.addExact(totalSold, quantity);
        stock = newStock;
        totalSold = newTotalSold;
    }

    // Package-private: only transactions call this, and only after checkCanRestore().
    void restoreSale(int quantity) {
        if (quantity <= 0 || quantity > totalSold) {
            throw new IllegalStateException("restoreSale() called without a successful checkCanRestore().");
        }
        int newStock = Math.addExact(stock, quantity);
        int newTotalSold = totalSold - quantity;
        stock = newStock;
        totalSold = newTotalSold;
    }

    // Package-private on purpose: renaming a product also needs a uniqueness check and a re-sort
    // of the master list, otherwise binary search would break. Only Inventory.renameProduct()
    // does both, so it is the only caller.
    void setName(String newName) {
        changeName(newName);
    }

    // Package-private: only Inventory.editPrice() calls this, after validating the price.
    void setPrice(BigDecimal newPrice) {
        this.price = Objects.requireNonNull(newPrice, "newPrice");
    }

    // Package-private: only Inventory.editStock() calls this, after validating the stock.
    void setStock(int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        this.stock = newStock;
    }

    private String stockText() {
        return isOutOfStock() ? "0 (OUT)" : String.valueOf(stock);
    }
}
