import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// The store's products. Adds, finds, edits and lists products; the master list stays sorted A-Z.
// Demonstrates INHERITANCE (extends NamedRecordList<Product>) and ABSTRACTION (Main never sees the list
// or the algorithms, it only asks for things like "find this product").
public class Inventory extends NamedRecordList<Product> {
    public static final int LOW_STOCK_LEVEL = 5;

    private static final Comparator<Product> BY_PRICE_LOW_TO_HIGH =
            (first, second) -> first.getPrice().compareTo(second.getPrice());
    private static final Comparator<Product> BY_STOCK_LOW_TO_HIGH =
            (first, second) -> Integer.compare(first.getStock(), second.getStock());
    private static final Comparator<Product> BY_SOLD_HIGH_TO_LOW =
            (first, second) -> Integer.compare(second.getTotalSold(), first.getTotalSold());

    public Inventory() {
        super("product");
    }

    // Validates everything, then adds a new product. Rejects duplicate codes and names.
    public Product addProduct(String rawCode, String rawName, BigDecimal price, int stock)
            throws ValidationException, DuplicateRecordException {
        String code = Validator.validateProductCode(rawCode);
        String name = Validator.validateProductName(rawName);
        Money.checkPrice(price);
        Validator.checkStock(stock);
        checkCodeAvailable(code);
        checkNameAvailable(name);
        Product product = new Product(code, name, price, stock);
        addRecord(product);
        return product;
    }

    // LINEAR SEARCH by product code (ignoring case). The list is sorted by NAME, not by code,
    // so binary search cannot be used here; we check each product one by one. Time: O(n).
    // Returns null if no product has that code.
    public Product findByCode(String code) {
        String wantedCode = code.trim().toUpperCase(Locale.ROOT);
        for (Product product : getAll()) {
            if (product.getCode().equals(wantedCode)) {
                return product;
            }
        }
        return null;
    }

    // Throws if another product already uses this code.
    public void checkCodeAvailable(String code) throws DuplicateRecordException {
        Product existing = findByCode(code);
        if (existing != null) {
            throw new DuplicateRecordException("Product code " + existing.getCode()
                    + " is already used by " + existing.getName() + ".");
        }
    }

    // NOTE: renameProduct(), editPrice(), editStock() and removeProduct() are not in the menu
    // (outside the documented project scope). If they are ever added to the menu, removeProduct()
    // must first check that no transaction on the undo stack refers to the product.

    // Renames a product, keeping names unique and the master list sorted.
    public void renameProduct(String code, String newName)
            throws RecordNotFoundException, ValidationException, DuplicateRecordException {
        Product product = requireByCode(code);
        String cleanName = Validator.validateProductName(newName);
        // A different product with the same name is a duplicate. The same product is fine
        // (for example, changing "egg" to "Egg").
        Product sameName = findByName(cleanName);
        if (sameName != null && sameName != product) {
            throw new DuplicateRecordException("A product named \"" + sameName.getName() + "\" already exists.");
        }
        product.setName(cleanName);
        // The new name may belong somewhere else in A-Z order. Binary search only works on a
        // sorted list, so we must re-sort the master list now.
        restoreNameOrder();
    }

    // Changes a product's price. Past transactions keep the price they were recorded with.
    public void editPrice(String code, BigDecimal newPrice) throws RecordNotFoundException, ValidationException {
        Product product = requireByCode(code);
        Money.checkPrice(newPrice);
        product.setPrice(newPrice);
    }

    // Changes a product's stock (0 to 1000000).
    public void editStock(String code, int newStock) throws RecordNotFoundException, ValidationException {
        Product product = requireByCode(code);
        Validator.checkStock(newStock);
        product.setStock(newStock);
    }

    // Removes a product from the master list.
    public void removeProduct(String code) throws RecordNotFoundException {
        removeRecord(requireByCode(code));
    }

    // Returns a copy sorted by name A-Z.
    public List<Product> getSortedByName() {
        return sortedCopy(getNameOrder());
    }

    // Returns a copy sorted by price, lowest first. Equal prices keep A-Z order (stable sort).
    public List<Product> getSortedByPrice() {
        return sortedCopy(BY_PRICE_LOW_TO_HIGH);
    }

    // Returns a copy sorted by stock, lowest first. Equal stock keeps A-Z order (stable sort).
    public List<Product> getSortedByStock() {
        return sortedCopy(BY_STOCK_LOW_TO_HIGH);
    }

    // Returns products that sold at least one item, highest total sold first.
    public List<Product> getBestSellers() {
        List<Product> sold = new ArrayList<>();
        for (Product product : getAll()) {
            if (product.getTotalSold() > 0) {
                sold.add(product);
            }
        }
        Algorithms.mergeSort(sold, BY_SOLD_HIGH_TO_LOW);
        return sold;
    }

    // Returns products with stock of LOW_STOCK_LEVEL or less (including zero), lowest stock first.
    public List<Product> getLowStockProducts() {
        List<Product> lowStock = new ArrayList<>();
        for (Product product : getAll()) {
            if (product.getStock() <= LOW_STOCK_LEVEL) {
                lowStock.add(product);
            }
        }
        Algorithms.mergeSort(lowStock, BY_STOCK_LOW_TO_HIGH);
        return lowStock;
    }

    private Product requireByCode(String code) throws RecordNotFoundException {
        Product product = findByCode(code);
        if (product == null) {
            throw new RecordNotFoundException("No product has the code " + code.trim().toUpperCase(Locale.ROOT) + ".");
        }
        return product;
    }
}
