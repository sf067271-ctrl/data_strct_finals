package sarisari.datastructures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import sarisari.algorithms.Algorithms;
import sarisari.exceptions.DuplicateRecordException;
import sarisari.exceptions.StoreException;
import sarisari.models.Product;
import sarisari.validators.TextRules;

/**
 * Stores and manages all products.
 *
 * The master list is ALWAYS kept in alphabetical order by name, because
 * binary search only works on a sorted list. Other orders (price, stock,
 * best sellers) are made on a COPY, so the master list never changes order.
 *
 * OOP - ENCAPSULATION: the ArrayList is private. Other classes get copies,
 * so nobody can add an unchecked product or break the alphabetical order.
 */
public class Inventory {

    /**
     * Permission to change a product name. Only Inventory can create it, so
     * callers must use editProductName() for uniqueness checks and sorting.
     */
    public static final class NameChangeAccess {
        private NameChangeAccess() {
        }
    }

    private static final NameChangeAccess NAME_CHANGE_ACCESS = new NameChangeAccess();

    // ----- Sort orders (Comparators) used with merge sort -----

    /** Lowest price first. */
    public static final Comparator<Product> BY_PRICE = new Comparator<Product>() {
        @Override
        public int compare(Product a, Product b) {
            return a.getPrice().compareTo(b.getPrice());
        }
    };

    /** Lowest stock first. */
    public static final Comparator<Product> BY_STOCK = new Comparator<Product>() {
        @Override
        public int compare(Product a, Product b) {
            return Integer.compare(a.getStock(), b.getStock());
        }
    };

    /** Highest total sold first (b is compared with a to reverse the order). */
    public static final Comparator<Product> BY_TOTAL_SOLD_DESC = new Comparator<Product>() {
        @Override
        public int compare(Product a, Product b) {
            return Integer.compare(b.getTotalSold(), a.getTotalSold());
        }
    };

    private final ArrayList<Product> products = new ArrayList<Product>();

    // =====================================================================
    //  Adding
    // =====================================================================

    /**
     * Adds a product, then uses MERGE SORT to restore alphabetical order.
     *
     * @throws DuplicateRecordException if the code or name is already used
     *         (ignoring upper/lower case and surrounding spaces)
     */
    public void addProduct(Product product) throws DuplicateRecordException {
        if (product == null) {
            throw new IllegalArgumentException("The product cannot be empty.");
        }
        Product sameCode = findByCode(product.getCode());
        if (sameCode != null) {
            throw new DuplicateRecordException("Product code " + product.getCode()
                    + " is already used by \"" + sameCode.getName() + "\".");
        }
        Product sameName = findByName(product.getName());
        if (sameName != null) {
            throw new DuplicateRecordException("A product named \"" + sameName.getName()
                    + "\" already exists (code " + sameName.getCode() + ").");
        }
        products.add(product);
        Algorithms.mergeSort(products, Algorithms.BY_NAME);
    }

    // =====================================================================
    //  Searching
    // =====================================================================

    /** Exact name lookup using BINARY SEARCH. Returns null if not found. */
    public Product findByName(String name) {
        if (TextRules.clean(name).isEmpty()) {
            return null;
        }
        int index = Algorithms.binarySearch(products, Algorithms.nameKey(name), Algorithms.BY_NAME);
        return index >= 0 ? products.get(index) : null;
    }

    /**
     * Exact code lookup using LINEAR SEARCH (the list is sorted by name, not
     * by code, so binary search cannot be used here). Returns null if not found.
     */
    public Product findByCode(String code) {
        final String wanted = TextRules.clean(code);
        if (wanted.isEmpty()) {
            return null;
        }
        int index = Algorithms.linearSearch(products, new Algorithms.Condition<Product>() {
            @Override
            public boolean matches(Product p) {
                return p.getCode().equalsIgnoreCase(wanted);
            }
        });
        return index >= 0 ? products.get(index) : null;
    }

    /** Partial name lookup using LINEAR SEARCH, e.g. "pan" finds "Lucky Me Pancit Canton". */
    public List<Product> searchPartial(String keyword) {
        final String wanted = TextRules.clean(keyword).toLowerCase();
        if (wanted.isEmpty()) {
            return new ArrayList<Product>();
        }
        return Algorithms.linearSearchAll(products, new Algorithms.Condition<Product>() {
            @Override
            public boolean matches(Product p) {
                return p.getName().toLowerCase().contains(wanted);
            }
        });
    }

    // =====================================================================
    //  Lists for display and reports (all are sorted COPIES)
    // =====================================================================

    /** All products in alphabetical order (a copy). */
    public List<Product> getAllProducts() {
        return new ArrayList<Product>(products);
    }

    /** A copy of the products sorted with merge sort in the given order. */
    public List<Product> getSortedCopy(Comparator<? super Product> order) {
        return Algorithms.sortedCopy(products, order);
    }

    /** Products with stock at or below the limit (zero included), lowest stock first. */
    public List<Product> getLowStockProducts(final int limit) {
        List<Product> low = Algorithms.linearSearchAll(products, new Algorithms.Condition<Product>() {
            @Override
            public boolean matches(Product p) {
                return p.isLowStock(limit);
            }
        });
        Algorithms.mergeSort(low, BY_STOCK);   // sorting the new list, not the master list
        return low;
    }

    /** Up to maxCount products that HAVE sales, most sold first. */
    public List<Product> getBestSellers(int maxCount) {
        List<Product> sorted = Algorithms.sortedCopy(products, BY_TOTAL_SOLD_DESC);
        List<Product> result = new ArrayList<Product>();
        for (int i = 0; i < sorted.size() && result.size() < maxCount; i++) {
            if (sorted.get(i).getTotalSold() > 0) {   // products with zero sales are excluded
                result.add(sorted.get(i));
            }
        }
        return result;
    }

    public int size() {
        return products.size();
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    // =====================================================================
    //  Edit and remove (available in code, NOT connected to the menu,
    //  to match the documented project scope)
    // =====================================================================

    /**
     * Removes a product by code.
     *
     * @return the removed product
     * @throws StoreException if no product has this code
     */
    public Product removeProduct(String code) throws StoreException {
        Product product = requireByCode(code);
        products.remove(product);   // removing keeps the remaining products in order
        return product;
    }

    /**
     * Renames a product. The new name must still be unique, and the master
     * list is re-sorted with merge sort so binary search keeps working.
     *
     * @throws StoreException if the code is unknown or the new name is already used
     */
    public void editProductName(String code, String newName) throws StoreException {
        Product product = requireByCode(code);
        Product sameName = findByName(newName);
        if (sameName != null && sameName != product) {
            throw new DuplicateRecordException("A product named \"" + sameName.getName() + "\" already exists.");
        }
        try {
            product.setName(newName, NAME_CHANGE_ACCESS);
        } catch (IllegalArgumentException e) {
            throw new StoreException(e.getMessage());
        }
        Algorithms.mergeSort(products, Algorithms.BY_NAME);
    }

    /** Changes a product's price. @throws StoreException if the code or price is invalid */
    public void editProductPrice(String code, BigDecimal newPrice) throws StoreException {
        Product product = requireByCode(code);
        try {
            product.setPrice(newPrice);
        } catch (IllegalArgumentException e) {
            throw new StoreException(e.getMessage());
        }
    }

    /** Changes a product's stock quantity. @throws StoreException if the code or quantity is invalid */
    public void editProductStock(String code, int newStock) throws StoreException {
        Product product = requireByCode(code);
        try {
            product.setStock(newStock);
        } catch (IllegalArgumentException e) {
            throw new StoreException(e.getMessage());
        }
    }

    private Product requireByCode(String code) throws StoreException {
        Product product = findByCode(code);
        if (product == null) {
            throw new StoreException("No product has the code \"" + TextRules.clean(code) + "\".");
        }
        return product;
    }
}
