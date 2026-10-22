import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import sarisari.algorithms.Algorithms;
import sarisari.datastructures.CustomerRecords;
import sarisari.datastructures.Inventory;
import sarisari.datastructures.UndoStack;
import sarisari.exceptions.DuplicateRecordException;
import sarisari.exceptions.EndOfInputException;
import sarisari.exceptions.InputCancelledException;
import sarisari.exceptions.InsufficientStockException;
import sarisari.exceptions.StoreException;
import sarisari.models.CashRegister;
import sarisari.models.Customer;
import sarisari.models.Product;
import sarisari.transactions.CashSaleTransaction;
import sarisari.transactions.PaymentTransaction;
import sarisari.transactions.RestockTransaction;
import sarisari.transactions.Transaction;
import sarisari.transactions.UtangTransaction;
import sarisari.utils.Money;
import sarisari.validators.InputHelper;

/**
 * SARI-SARI STORE DEBT AND STOCK MANAGEMENT SYSTEM
 *
 * Main starts the program, shows the menus, reads the operator's input
 * (through InputHelper) and coordinates each transaction.
 *
 * How every transaction is kept safe:
 *   1. Collect ALL input first (typing X cancels at any point).
 *   2. Show a summary and ask for confirmation.
 *   3. Only then record it. The transaction checks itself again before
 *      changing anything, so a failed or cancelled operation never leaves
 *      stock, balances, history, totals or the undo stack half-changed.
 *
 * All data is kept in memory (ArrayLists). Sample data is loaded at startup
 * and everything is discarded when the program exits.
 *
 * Compile:  javac -d bin -sourcepath src src/Main.java   (from the project folder)
 * Run:      java -cp bin Main
 * Or double-click run.bat (Windows) / run "sh run.sh" (macOS, Linux).
 */
public class Main {

    /** A product is low on stock when it has this many pieces or fewer. */
    private static final int LOW_STOCK_LIMIT = 5;

    private static final int LINE_WIDTH = 78;

    private final InputHelper input;
    private final Inventory inventory = new Inventory();
    private final CustomerRecords customerRecords = new CustomerRecords();
    private final UndoStack undoStack = new UndoStack();
    private final CashRegister cashRegister = new CashRegister();

    public Main(Scanner scanner) {
        this.input = new InputHelper(scanner);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);   // the ONE Scanner used by the whole program
        Main app = new Main(scanner);
        app.run();
    }

    // =====================================================================
    //  Main menu loop
    // =====================================================================

    private void run() {
        loadSampleData();

        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = input.readMenuChoice("Enter your choice (1-12): ", 1, 12);
                running = handleMenuChoice(choice);
            } catch (EndOfInputException e) {
                System.out.println();
                System.out.println("No more input was received. Closing the program.");
                running = false;
            }
        }
        System.out.println();
        System.out.println("Thank you for using the system. Salamat po!");
        System.out.println("(This session's changes were kept in memory only and are now cleared.)");
    }

    /**
     * Runs the chosen menu option and handles every error in ONE place.
     *
     * Exception handling:
     *   InputCancelledException  - operator typed X: nothing was changed
     *   StoreException           - a business rule was broken (catches all its
     *                              subclasses too: polymorphism)
     *   IllegalArgumentException - invalid data reached a class's validation
     *   EndOfInputException      - passed up to run() to exit politely
     *   RuntimeException         - any unexpected bug: report it, keep running
     *
     * @return false if the program should end
     */
    private boolean handleMenuChoice(int choice) {
        boolean pauseAfter = true;
        try {
            switch (choice) {
                case 1:
                    addProduct();
                    break;
                case 2:
                    searchProduct();
                    break;
                case 3:
                    recordCashSale();
                    break;
                case 4:
                    recordUtang();
                    break;
                case 5:
                    recordUtangPayment();
                    break;
                case 6:
                    searchCustomer();
                    break;
                case 7:
                    displayProductsSorted();   // has its own sub-menu loop
                    pauseAfter = false;
                    break;
                case 8:
                    showLowStockAlert();
                    break;
                case 9:
                    showReports();
                    break;
                case 10:
                    undoLastTransaction();
                    break;
                case 11:
                    if (confirmExit()) {
                        return false;
                    }
                    pauseAfter = false;
                    break;
                case 12:
                    restockProduct();
                    break;
                default:
                    printError("Unknown menu option.");
                    break;
            }
        } catch (InputCancelledException e) {
            printInfo("Cancelled. No records were changed.");
        } catch (InsufficientStockException e) {
            printError(e.getMessage());
        } catch (StoreException e) {
            printError(e.getMessage());
        } catch (IllegalArgumentException e) {
            printError(e.getMessage());
        } catch (EndOfInputException e) {
            throw e;   // let run() end the program
        } catch (RuntimeException e) {
            printError("Something unexpected happened: " + e + ". No further changes were made.");
        }

        if (pauseAfter) {
            input.pause();
        }
        return true;
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(repeat('=', LINE_WIDTH));
        System.out.println(center("SARI-SARI STORE"));
        System.out.println(center("Debt (Utang) and Stock Management System"));
        System.out.println(repeat('=', LINE_WIDTH));
        System.out.println("   1. Add product                     7. Display products sorted");
        System.out.println("   2. Search product                  8. Low-stock alert");
        System.out.println("   3. Record cash sale                9. Reports");
        System.out.println("   4. Record utang                   10. Undo last transaction");
        System.out.println("   5. Record utang payment           11. Exit");
        System.out.println("   6. Search customer               12. Restock product");
        System.out.println(repeat('-', LINE_WIDTH));
        System.out.println("   Products: " + inventory.size()
                + "   |   Customers: " + customerRecords.size()
                + "   |   Cash sales: " + Money.format(cashRegister.getTotal())
                + "   |   Undo available: " + undoStack.size());
        System.out.println(repeat('=', LINE_WIDTH));
    }

    // =====================================================================
    //  1. Add product
    // =====================================================================

    private void addProduct() throws StoreException {
        printHeading("ADD PRODUCT");
        printCancelHint();

        // Ask again until the code is not used yet
        String code;
        while (true) {
            code = input.readCode("Product code (e.g. P008): ");
            Product sameCode = inventory.findByCode(code);   // linear search
            if (sameCode == null) {
                break;
            }
            printError("Code " + code + " is already used by \"" + sameCode.getName() + "\". Enter a different code.");
        }

        // Ask again until the name is not used yet
        String name;
        while (true) {
            name = input.readName("Product name: ");
            Product sameName = inventory.findByName(name);   // binary search
            if (sameName == null) {
                break;
            }
            printError("\"" + sameName.getName() + "\" already exists (code " + sameName.getCode() + "). Enter a different name.");
        }

        BigDecimal price = input.readMoney("Price per piece in PHP (e.g. 12.50): ", Money.ONE_CENTAVO, Money.MAX_AMOUNT);
        int stock = input.readWholeNumber("Opening stock (0 to " + Product.MAX_STOCK + "): ", 0, Product.MAX_STOCK);

        Product product = new Product(code, name, price, stock);

        System.out.println();
        System.out.println("New product: " + product);
        if (!input.confirm("Save this product?")) {
            printInfo("Cancelled. The product was not added.");
            return;
        }
        inventory.addProduct(product);   // checks duplicates again and re-sorts with merge sort
        printSuccess("\"" + product.getName() + "\" was added to the inventory.");
    }

    // =====================================================================
    //  2. Search product
    // =====================================================================

    private void searchProduct() {
        printHeading("SEARCH PRODUCT");
        if (inventory.isEmpty()) {
            printInfo("No records found. There are no products yet.");
            return;
        }
        printCancelHint();
        String keyword = input.readName("Enter a product name or code: ");

        // 1. Exact name: BINARY SEARCH on the alphabetical list
        Product exact = inventory.findByName(keyword);
        if (exact != null) {
            printInfo("Exact name match found using binary search.");
            printProductTable(singleItemList(exact));
            return;
        }

        // 2. Exact code: LINEAR SEARCH (the list is not sorted by code)
        Product byCode = inventory.findByCode(keyword);
        if (byCode != null) {
            printInfo("Product code match found using linear search.");
            printProductTable(singleItemList(byCode));
            return;
        }

        // 3. Partial name: LINEAR SEARCH
        List<Product> partial = inventory.searchPartial(keyword);
        if (partial.isEmpty()) {
            printInfo("No records found for \"" + keyword + "\".");
        } else {
            printInfo("No exact match. Products whose names contain \"" + keyword
                    + "\" (linear search): " + partial.size());
            printProductTable(partial);
        }
    }

    // =====================================================================
    //  3. Record cash sale
    // =====================================================================

    private void recordCashSale() throws StoreException {
        printHeading("RECORD CASH SALE");
        printCancelHint();

        Product product = selectProduct();
        if (product.getStock() == 0) {
            throw new InsufficientStockException(product.getName(), 1, 0);
        }
        System.out.println("Selected: " + product);

        // Ask for the quantity until the transaction is valid (enough stock)
        CashSaleTransaction sale = null;
        while (sale == null) {
            int quantity = input.readWholeNumber("Quantity sold: ", 1, Product.MAX_STOCK);
            try {
                sale = new CashSaleTransaction(product, quantity, cashRegister);
            } catch (InsufficientStockException e) {
                printError(e.getMessage() + " Enter a smaller quantity, or X to cancel.");
            }
        }

        System.out.println();
        System.out.println("Cash sale: " + sale.getDetails() + " = " + Money.format(sale.getTotal()));
        if (!input.confirm("Record this cash sale?")) {
            printInfo("Cancelled. No records were changed.");
            return;
        }

        sale.execute();          // checks again, then changes stock, total sold and cash sales
        undoStack.push(sale);    // PUSH: the newest transaction goes on top of the stack
        printSuccess("Cash sale recorded.");
        System.out.println(sale.getReceipt());
        printLowStockWarning(product);
    }

    // =====================================================================
    //  4. Record utang
    // =====================================================================

    private void recordUtang() throws StoreException {
        printHeading("RECORD UTANG");
        if (inventory.isEmpty()) {
            printInfo("There are no products to sell yet. Add a product first.");
            return;
        }
        printCancelHint();

        Customer customer = selectOrCreateCustomer();
        boolean isNewCustomer = customerRecords.findByName(customer.getName()) == null;
        if (isNewCustomer) {
            System.out.println("Customer: " + customer.getName() + " (NEW - will be saved when the utang is recorded)");
        } else {
            System.out.println("Customer: " + customer.getName() + ", current balance " + Money.format(customer.getBalance()));
        }

        Product product = selectProduct();
        if (product.getStock() == 0) {
            throw new InsufficientStockException(product.getName(), 1, 0);
        }
        System.out.println("Selected: " + product);

        UtangTransaction utang = null;
        while (utang == null) {
            int quantity = input.readWholeNumber("Quantity: ", 1, Product.MAX_STOCK);
            try {
                utang = new UtangTransaction(customer, product, quantity);
            } catch (InsufficientStockException e) {
                printError(e.getMessage() + " Enter a smaller quantity, or X to cancel.");
            }
        }

        BigDecimal newBalance = customer.getBalance().add(utang.getTotal());
        System.out.println();
        System.out.println("Utang: " + utang.getDetails() + " = " + Money.format(utang.getTotal()));
        System.out.println("The balance will become " + Money.format(newBalance) + ".");
        if (!input.confirm("Record this utang?")) {
            printInfo("Cancelled. No records were changed.");
            return;
        }

        // A new customer is saved only now, after confirmation.
        if (isNewCustomer) {
            customerRecords.addCustomer(customer);
        }
        try {
            utang.execute();
        } catch (StoreException e) {
            // ROLLBACK: the utang failed, so do not keep the customer created for it.
            if (isNewCustomer) {
                customerRecords.removeCustomer(customer);
            }
            throw e;
        }
        undoStack.push(utang);
        printSuccess("Utang recorded" + (isNewCustomer ? " and new customer saved." : "."));
        System.out.println(utang.getReceipt());
        printLowStockWarning(product);
    }

    // =====================================================================
    //  5. Record utang payment
    // =====================================================================

    private void recordUtangPayment() throws StoreException {
        printHeading("RECORD UTANG PAYMENT");
        if (customerRecords.getCustomersWithUtang().isEmpty()) {
            printInfo("No customer has an outstanding utang right now.");
            return;
        }
        printCancelHint();

        Customer customer = selectExistingCustomer();
        System.out.println("Customer: " + customer.getName() + ", outstanding balance " + Money.format(customer.getBalance()));
        if (!customer.hasOutstandingUtang()) {
            throw new StoreException(customer.getName() + " has no outstanding utang. Payment rejected.");
        }

        // Ask for the amount until it is a valid payment (not more than the balance)
        PaymentTransaction payment = null;
        while (payment == null) {
            BigDecimal amount = input.readMoney("Amount paid in PHP: ", Money.ONE_CENTAVO, Money.MAX_AMOUNT);
            try {
                payment = new PaymentTransaction(customer, amount);
            } catch (StoreException e) {
                printError(e.getMessage() + " Enter a smaller amount, or X to cancel.");
            }
        }

        BigDecimal newBalance = customer.getBalance().subtract(payment.getAmount());
        System.out.println();
        System.out.println("Payment of " + Money.format(payment.getAmount()) + " from " + customer.getName()
                + ". The balance will become " + Money.format(newBalance) + ".");
        if (!input.confirm("Record this payment?")) {
            printInfo("Cancelled. No records were changed.");
            return;
        }

        payment.execute();
        undoStack.push(payment);
        printSuccess(customer.hasOutstandingUtang()
                ? "Payment recorded."
                : "Payment recorded. " + customer.getName() + "'s utang is now FULLY PAID.");
        System.out.println(payment.getReceipt());
    }

    // =====================================================================
    //  6. Search customer
    // =====================================================================

    private void searchCustomer() {
        printHeading("SEARCH CUSTOMER");
        if (customerRecords.isEmpty()) {
            printInfo("No records found. There are no customers yet.");
            return;
        }
        printCancelHint();
        String keyword = input.readName("Enter a customer name: ");

        // 1. Exact name: BINARY SEARCH
        Customer exact = customerRecords.findByName(keyword);
        if (exact != null) {
            printInfo("Exact match found using binary search.");
            printCustomerDetails(exact);
            return;
        }

        // 2. Partial name: LINEAR SEARCH
        List<Customer> partial = customerRecords.searchPartial(keyword);
        if (partial.isEmpty()) {
            printInfo("No records found for \"" + keyword + "\".");
            return;
        }
        printInfo("No exact match. Customers whose names contain \"" + keyword
                + "\" (linear search): " + partial.size());
        for (Customer c : partial) {
            printCustomerDetails(c);
        }
    }

    // =====================================================================
    //  7. Display products sorted (sub-menu)
    // =====================================================================

    private void displayProductsSorted() {
        while (true) {
            printHeading("DISPLAY PRODUCTS SORTED (merge sort)");
            System.out.println("   1. By name (A to Z)");
            System.out.println("   2. By price (lowest first)");
            System.out.println("   3. By stock quantity (lowest first)");
            System.out.println("   4. Best sellers (most sold first)");
            System.out.println("   5. Back to main menu");
            int choice = input.readMenuChoice("Choose an order (1-5): ", 1, 5);
            if (choice == 5) {
                return;
            }

            List<Product> sorted;
            String title;
            switch (choice) {
                case 1:
                    sorted = inventory.getSortedCopy(Algorithms.BY_NAME);
                    title = "Products by name (A to Z)";
                    break;
                case 2:
                    sorted = inventory.getSortedCopy(Inventory.BY_PRICE);
                    title = "Products by price (lowest first; equal prices keep A-Z order)";
                    break;
                case 3:
                    sorted = inventory.getSortedCopy(Inventory.BY_STOCK);
                    title = "Products by stock quantity (lowest first)";
                    break;
                default:
                    sorted = inventory.getSortedCopy(Inventory.BY_TOTAL_SOLD_DESC);
                    title = "Best sellers (most sold first)";
                    break;
            }

            System.out.println();
            System.out.println(title + ":");
            printProductTable(sorted);
            input.pause();
        }
    }

    // =====================================================================
    //  8. Low-stock alert
    // =====================================================================

    private void restockProduct() throws StoreException {
        printHeading("RESTOCK PRODUCT");
        if (inventory.isEmpty()) {
            printInfo("There are no products yet. Add a product first.");
            return;
        }
        printCancelHint();
        Product product = selectProduct();
        System.out.println("Selected: " + product);
        int capacity = Product.MAX_STOCK - product.getStock();
        if (capacity == 0) {
            printInfo("This product is already at the stock limit of " + Product.MAX_STOCK + ".");
            return;
        }
        int quantity = input.readWholeNumber("Quantity received (1-" + capacity + "): ", 1, capacity);
        RestockTransaction restock = new RestockTransaction(product, quantity);
        System.out.println("Stock: " + product.getStock() + " + " + quantity
                + " = " + (product.getStock() + quantity));
        if (!input.confirm("Record this restock?")) {
            printInfo("Cancelled. No records were changed.");
            return;
        }
        restock.execute();
        undoStack.push(restock);
        printSuccess("Restock recorded. " + product.getName() + " stock is now " + product.getStock() + ".");
        printLowStockWarning(product);
    }

    private void showLowStockAlert() {
        printHeading("LOW-STOCK ALERT (" + LOW_STOCK_LIMIT + " pieces or fewer)");
        List<Product> low = inventory.getLowStockProducts(LOW_STOCK_LIMIT);
        if (low.isEmpty()) {
            printInfo("No records found. Every product has more than " + LOW_STOCK_LIMIT + " pieces in stock.");
            return;
        }
        System.out.println(low.size() + " product(s) need restocking, lowest stock first:");
        printProductTable(low);
    }

    // =====================================================================
    //  9. Reports
    // =====================================================================

    private void showReports() {
        printHeading("REPORTS");
        System.out.println("  Total cash sales (this session) : " + Money.format(cashRegister.getTotal()));
        System.out.println("  Total outstanding utang         : " + Money.format(customerRecords.getTotalOutstanding()));

        // Top 3 best sellers (merge sort by total sold, products with zero sales excluded)
        System.out.println();
        System.out.println("  TOP 3 BEST-SELLING PRODUCTS");
        List<Product> best = inventory.getBestSellers(3);
        if (best.isEmpty()) {
            System.out.println("    No records found. No products have been sold yet.");
        } else {
            System.out.printf("    %-5s %-30s %10s%n", "Rank", "Product", "Qty sold");
            for (int i = 0; i < best.size(); i++) {
                System.out.printf("    %-5d %-30s %10d%n", i + 1, fit(best.get(i).getName(), 30), best.get(i).getTotalSold());
            }
            if (best.size() < 3) {
                System.out.println("    (Only " + best.size() + " product(s) have sales so far.)");
            }
        }

        // Customers with utang (merge sort by balance, highest first)
        System.out.println();
        System.out.println("  CUSTOMERS WITH OUTSTANDING UTANG (highest balance first)");
        List<Customer> owing = customerRecords.getCustomersWithUtang();
        if (owing.isEmpty()) {
            System.out.println("    No records found. No customer has an outstanding utang.");
        } else {
            System.out.printf("    %-5s %-30s %16s%n", "No.", "Customer", "Balance");
            for (int i = 0; i < owing.size(); i++) {
                System.out.printf("    %-5d %-30s %16s%n", i + 1, fit(owing.get(i).getName(), 30),
                        Money.format(owing.get(i).getBalance()));
            }
        }
    }

    // =====================================================================
    //  10. Undo last transaction (STACK)
    // =====================================================================

    /**
     * Undo uses the stack's LIFO order:
     *   1. PEEK at the top transaction (the most recent one) and show it.
     *   2. Ask for confirmation.
     *   3. Reverse it. Thanks to polymorphism, undo() runs the correct
     *      reversal for a cash sale, an utang, or a payment.
     *   4. POP it from the stack ONLY after the reversal succeeded.
     * Cancelling, or a failed reversal, leaves everything unchanged.
     */
    private void undoLastTransaction() throws StoreException {
        printHeading("UNDO LAST TRANSACTION");
        Transaction last = undoStack.peek();
        if (last == null) {
            printInfo("There is nothing to undo. The undo stack is empty.");
            return;
        }

        System.out.println("Most recent transaction (top of the stack):");
        System.out.println("  " + last.getSummary());
        if (!input.confirm("Undo this transaction?")) {
            printInfo("Undo cancelled. Nothing was changed.");
            return;
        }

        last.undo();        // throws StoreException if it cannot be reversed; the stack is untouched
        undoStack.pop();    // removed only after a successful reversal
        printSuccess("Transaction #" + last.getNumber() + " (" + last.getType().getLabel() + ") was undone.");
        printInfo(last.getUndoResult());
        printInfo(undoStack.isEmpty()
                ? "The undo stack is now empty."
                : undoStack.size() + " transaction(s) can still be undone.");
    }

    // =====================================================================
    //  11. Exit
    // =====================================================================

    private boolean confirmExit() {
        System.out.println();
        try {
            return input.confirm("Exit the program? All changes from this session will be lost.");
        } catch (InputCancelledException e) {
            return false;
        }
    }

    // =====================================================================
    //  Selecting a product or customer
    // =====================================================================

    /**
     * Asks for a product code or exact name until ONE product is identified.
     *   - code        -> linear search
     *   - exact name  -> binary search
     *   - otherwise   -> partial matches (linear search); the operator must
     *                    pick one by number before continuing
     * Typing X cancels (InputCancelledException).
     */
    private Product selectProduct() {
        while (true) {
            String text = input.readName("Product code or exact name: ");

            Product product = inventory.findByCode(text);
            if (product == null) {
                product = inventory.findByName(text);
            }
            if (product != null) {
                return product;
            }

            List<Product> matches = inventory.searchPartial(text);
            if (matches.isEmpty()) {
                printError("No records found for \"" + text + "\". Try again, or type X to cancel.");
                continue;
            }

            System.out.println("No exact match. Did you mean one of these?");
            for (int i = 0; i < matches.size(); i++) {
                System.out.printf("   %d. %s%n", i + 1, matches.get(i));
            }
            int pick = input.readWholeNumber("Choose a product (1-" + matches.size() + "): ", 1, matches.size());
            return matches.get(pick - 1);
        }
    }

    /**
     * For RECORD UTANG: finds an existing customer, or creates a new one.
     * If the name does not exist, the operator is asked whether to create it.
     * The new customer is NOT saved here; it is saved only when the utang is
     * confirmed (see recordUtang).
     */
    private Customer selectOrCreateCustomer() {
        while (true) {
            String name = input.readName("Customer name: ");

            Customer existing = customerRecords.findByName(name);   // binary search, ignores case and spaces
            if (existing != null) {
                return existing;
            }

            List<Customer> matches = customerRecords.searchPartial(name);
            if (!matches.isEmpty()) {
                System.out.println("No exact match. Customers whose names contain \"" + name + "\":");
                for (int i = 0; i < matches.size(); i++) {
                    System.out.printf("   %d. %s%n", i + 1, matches.get(i));
                }
                int createOption = matches.size() + 1;
                System.out.printf("   %d. Create a NEW customer named \"%s\"%n", createOption, name);
                int pick = input.readWholeNumber("Choose (1-" + createOption + "): ", 1, createOption);
                if (pick < createOption) {
                    return matches.get(pick - 1);
                }
                return new Customer(name);
            }

            if (input.confirm("Customer \"" + name + "\" does not exist yet. Create this customer?")) {
                return new Customer(name);
            }
            printInfo("Customer not created. Enter another name, or type X to cancel.");
        }
    }

    /** For PAYMENT: finds an existing customer (exact or partial). Typing X cancels. */
    private Customer selectExistingCustomer() {
        while (true) {
            String name = input.readName("Customer name: ");

            Customer existing = customerRecords.findByName(name);
            if (existing != null) {
                return existing;
            }

            List<Customer> matches = customerRecords.searchPartial(name);
            if (matches.isEmpty()) {
                printError("No records found for \"" + name + "\". Try again, or type X to cancel.");
                continue;
            }
            System.out.println("No exact match. Did you mean one of these?");
            for (int i = 0; i < matches.size(); i++) {
                System.out.printf("   %d. %s%n", i + 1, matches.get(i));
            }
            int pick = input.readWholeNumber("Choose a customer (1-" + matches.size() + "): ", 1, matches.size());
            return matches.get(pick - 1);
        }
    }

    // =====================================================================
    //  Sample data (DEMO RECORDS ONLY - cleared when the program exits)
    // =====================================================================

    private void loadSampleData() {
        try {
            //                          code    name                        price    stock
            inventory.addProduct(sample("P001", "Milo Sachet",              "9.00",  40));
            inventory.addProduct(sample("P002", "Egg",                      "10.00", 30));
            inventory.addProduct(sample("P003", "Lucky Me Pancit Canton",   "16.00",  4));   // low
            inventory.addProduct(sample("P004", "Kopiko Coffee Candy",      "1.00", 120));
            inventory.addProduct(sample("P005", "Bear Brand Sachet",        "12.00",  2));   // low
            inventory.addProduct(sample("P006", "Datu Puti Vinegar 200ml",  "12.00",  0));   // zero
            inventory.addProduct(sample("P007", "Coke Mismo",               "20.00", 24));

            customerRecords.addCustomer(new Customer("Aling Nena"));
            customerRecords.addCustomer(new Customer("Mang Tonyo"));
            customerRecords.addCustomer(new Customer("Ate Joy"));
        } catch (DuplicateRecordException e) {
            printError("Sample data problem: " + e.getMessage());
        }
        System.out.println();
        System.out.println("[SAMPLE DATA] Loaded " + inventory.size() + " demo products (codes P001-P007) and "
                + customerRecords.size() + " demo customers with zero balances.");
        System.out.println("[SAMPLE DATA] Records are kept in memory only and are cleared when you exit.");
    }

    private static Product sample(String code, String name, String price, int stock) {
        return new Product(code, name, Money.of(price), stock);
    }

    // =====================================================================
    //  Printing helpers
    // =====================================================================

    private void printProductTable(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("  No records found.");
            return;
        }
        System.out.printf("  %-4s %-8s %-28s %14s %7s %6s  %s%n", "No.", "Code", "Product name", "Price", "Stock", "Sold", "Status");
        System.out.println("  " + repeat('-', LINE_WIDTH - 2));
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            System.out.printf("  %-4d %-8s %-28s %14s %7d %6d  %s%n", i + 1, p.getCode(), fit(p.getName(), 28),
                    Money.format(p.getPrice()), p.getStock(), p.getTotalSold(), stockStatus(p));
        }
    }

    private String stockStatus(Product p) {
        if (p.getStock() == 0) {
            return "OUT OF STOCK";
        }
        if (p.isLowStock(LOW_STOCK_LIMIT)) {
            return "LOW";
        }
        return "OK";
    }

    /**
     * Prints a customer's name, balance and full history. Each history row
     * calls getBalanceEffect() and getHistoryDetails(): POLYMORPHISM picks the
     * utang or payment version automatically.
     */
    private void printCustomerDetails(Customer customer) {
        System.out.println();
        System.out.println("  Customer : " + customer.getName());
        System.out.println("  Balance  : " + Money.format(customer.getBalance())
                + (customer.hasOutstandingUtang() ? "" : "  (no outstanding utang)"));
        System.out.println("  History  :");

        List<Transaction> history = customer.getHistory();
        if (history.isEmpty()) {
            System.out.println("    No records found.");
            return;
        }
        System.out.printf("    %-5s %-17s %-8s %-28s %14s  %s%n", "No.", "Date and time", "Type", "Details", "Amount", "Status");
        System.out.println("    " + repeat('-', LINE_WIDTH + 6));
        for (Transaction t : history) {
            BigDecimal effect = t.getBalanceEffect();
            String sign = effect.signum() >= 0 ? "+" : "-";
            System.out.printf("    %-5s %-17s %-8s %-28s %14s  %s%n", "#" + t.getNumber(), t.getRecordedAtText(),
                    t.getType().getLabel(), fit(t.getHistoryDetails(), 28), sign + Money.format(effect.abs()), t.getStatus());
        }
    }

    private void printLowStockWarning(Product product) {
        if (product.getStock() == 0) {
            printInfo("WARNING: " + product.getName() + " is now OUT OF STOCK.");
        } else if (product.isLowStock(LOW_STOCK_LIMIT)) {
            printInfo("WARNING: " + product.getName() + " is low on stock (" + product.getStock() + " left).");
        }
    }

    private void printHeading(String title) {
        System.out.println();
        System.out.println(repeat('-', LINE_WIDTH));
        System.out.println("  " + title);
        System.out.println(repeat('-', LINE_WIDTH));
    }

    private void printCancelHint() {
        System.out.println("(Type " + InputHelper.CANCEL_WORD + " at any prompt to cancel and return to the main menu.)");
    }

    private void printSuccess(String message) {
        System.out.println("  [OK] " + message);
    }

    private void printInfo(String message) {
        System.out.println("  [i] " + message);
    }

    private void printError(String message) {
        System.out.println("  [ERROR] " + message);
    }

    private static List<Product> singleItemList(Product p) {
        List<Product> list = new java.util.ArrayList<Product>();
        list.add(p);
        return list;
    }

    /** Shortens text that is too long for a table column. */
    private static String fit(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }

    /** Java 8 has no String.repeat(), so it is written here. */
    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String center(String text) {
        int left = Math.max(0, (LINE_WIDTH - text.length()) / 2);
        return repeat(' ', left) + text;
    }
}
