package sarisari.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

import sarisari.datastructures.Inventory;
import sarisari.exceptions.InsufficientStockException;
import sarisari.exceptions.StoreException;
import sarisari.utils.Money;
import sarisari.validators.TextRules;

/**
 * A product sold in the sari-sari store.
 *
 * OOP - ENCAPSULATION:
 *   All fields are PRIVATE. Other classes can READ them through getters, but
 *   can only CHANGE them through methods that check the rules first:
 *     - the price must be positive with at most two decimal places,
 *     - the stock can never go below zero,
 *     - you cannot sell more than what is on the shelf.
 *   This way a Product object can never be in an invalid state.
 *
 * OOP - ABSTRACTION / POLYMORPHISM:
 *   Implements the Nameable interface, so the same binary search and merge
 *   sort used for customers also work for products.
 */
public class Product implements Nameable {

    /** Highest stock quantity accepted for one product. */
    public static final int MAX_STOCK = 1000000;

    private final String code;   // the code never changes after creation
    private String name;
    private BigDecimal price;
    private int stock;
    private int totalSold;

    /**
     * Creates a product with zero sales.
     *
     * @throws IllegalArgumentException if any value breaks the rules
     */
    public Product(String code, String name, BigDecimal price, int stock) {
        this.code = requireValidCode(code);
        this.name = requireValidName(name);
        this.price = requireValidPrice(price);
        this.stock = requireValidStock(stock);
        this.totalSold = 0;
    }

    // =====================================================================
    //  Getters (read-only access)
    // =====================================================================

    public String getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
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

    /** True when the stock is at or below the limit (zero stock included). */
    public boolean isLowStock(int limit) {
        return stock <= limit;
    }

    // =====================================================================
    //  Stock changes used by transactions
    // =====================================================================

    /**
     * Takes items off the shelf for a cash sale or utang:
     * stock goes DOWN and total sold goes UP.
     *
     * Everything is checked first, and both fields are changed only after
     * all checks pass, so a failed sale changes nothing.
     *
     * @throws InsufficientStockException if there are not enough pieces
     * @throws StoreException             if the quantity is invalid or the total would overflow
     */
    public void sell(int quantity) throws StoreException {
        if (quantity <= 0) {
            throw new StoreException("The quantity must be at least 1.");
        }
        if (quantity > stock) {
            throw new InsufficientStockException(name, quantity, stock);
        }
        int newTotalSold;
        try {
            newTotalSold = Math.addExact(totalSold, quantity);   // detects integer overflow
        } catch (ArithmeticException e) {
            throw new StoreException("The total sold for " + name + " is too large to record.");
        }
        stock = stock - quantity;
        totalSold = newTotalSold;
    }

    /** Adds delivered pieces without changing the total sold. */
    public void addStock(int quantity) throws StoreException {
        if (quantity <= 0 || quantity > MAX_STOCK - stock) {
            throw new StoreException("Quantity received must be from 1 to " + (MAX_STOCK - stock) + ".");
        }
        stock += quantity;
    }

    /** Removes a previously recorded delivery when it is undone. */
    public void reverseRestock(int quantity) throws StoreException {
        if (quantity <= 0 || quantity > stock) {
            throw new StoreException("Cannot undo restock: there are not enough pieces in stock.");
        }
        stock -= quantity;
    }

    /** True if a sale of this quantity can be reversed (used by undo). */
    public boolean canReverseSale(int quantity) {
        return quantity > 0 && quantity <= totalSold && quantity <= MAX_STOCK - stock;
    }

    /**
     * Puts items back on the shelf when a sale or utang is UNDONE:
     * stock goes UP and total sold goes DOWN.
     *
     * @throws StoreException if the reversal would make the numbers invalid
     */
    public void reverseSale(int quantity) throws StoreException {
        if (!canReverseSale(quantity)) {
            throw new StoreException("Cannot return " + quantity + " piece(s) of " + name
                    + " to stock: the recorded sales do not allow it.");
        }
        stock = stock + quantity;
        totalSold = totalSold - quantity;
    }

    // =====================================================================
    //  Edits (used only by Inventory's edit methods)
    // =====================================================================

    /**
     * Changes the name with permission held only by Inventory. Use
     * Inventory.editProductName() so uniqueness and sorting stay consistent.
     *
     * @throws IllegalArgumentException if permission is missing or the name is invalid
     */
    public final void setName(String newName, Inventory.NameChangeAccess access) {
        if (access == null) {
            throw new IllegalArgumentException("Use Inventory.editProductName() to change a product name.");
        }
        this.name = requireValidName(newName);
    }

    /** Changes the price after checking it. Called by Inventory.editProductPrice(). */
    public void setPrice(BigDecimal newPrice) {
        this.price = requireValidPrice(newPrice);
    }

    /** Changes the stock quantity after checking it. Called by Inventory.editProductStock(). */
    public void setStock(int newStock) {
        this.stock = requireValidStock(newStock);
    }

    // =====================================================================
    //  Validation helpers (private: they are internal details)
    // =====================================================================

    private static String requireValidCode(String code) {
        String problem = TextRules.checkCode(code);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return TextRules.clean(code).toUpperCase();
    }

    private static String requireValidName(String name) {
        String problem = TextRules.checkName(name);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return TextRules.clean(name);
    }

    private static BigDecimal requireValidPrice(BigDecimal price) {
        if (!Money.isPositive(price)) {
            throw new IllegalArgumentException("The price must be greater than zero.");
        }
        if (!Money.hasAtMostTwoDecimals(price)) {
            throw new IllegalArgumentException("The price can have at most two decimal places.");
        }
        if (price.compareTo(Money.MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("The price cannot be more than " + Money.format(Money.MAX_AMOUNT) + ".");
        }
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static int requireValidStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("The stock cannot be negative.");
        }
        if (stock > MAX_STOCK) {
            throw new IllegalArgumentException("The stock cannot be more than " + MAX_STOCK + ".");
        }
        return stock;
    }

    /** OOP - POLYMORPHISM: overrides Object.toString() with a readable description. */
    @Override
    public String toString() {
        return code + " - " + name + " (" + Money.format(price) + ", stock " + stock + ")";
    }
}
