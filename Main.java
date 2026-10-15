import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

// Starts the program, shows the menus, runs each workflow and catches errors at the top level.
// It holds no business rules: those live in Inventory, CustomerRecords, the transactions and Validator.
// Demonstrates ABSTRACTION (asks objects to do things without knowing how) and POLYMORPHISM
// (saves and undoes every transaction through the Transaction type).
public class Main {
    private static final int MENU_ADD_PRODUCT = 1;
    private static final int MENU_SEARCH_PRODUCT = 2;
    private static final int MENU_ADD_TRANSACTION = 3;
    private static final int MENU_UTANG_HISTORY = 4;
    private static final int MENU_RECORD_PAYMENT = 5;
    private static final int MENU_PRODUCTS_SORTED = 6;
    private static final int MENU_LOW_STOCK = 7;
    private static final int MENU_REPORTS = 8;
    private static final int MENU_UNDO = 9;
    private static final int MENU_EXIT = 10;

    private static final int SORT_BY_NAME = 1;
    private static final int SORT_BY_PRICE = 2;
    private static final int SORT_BY_STOCK = 3;
    private static final int SORT_BY_BEST_SELLERS = 4;
    private static final int SORT_MENU_BACK = 5;

    private static final int HISTORY_MENU_BACK = 4;
    private static final int TOP_SELLER_COUNT = 3;
    private static final int CREATE_NEW_CUSTOMER_CHOICE = 0;
    private static final String NO_RECORDS = "No records found.";

    private final ConsoleInput input;
    private final Inventory inventory = new Inventory();
    private final CustomerRecords customers = new CustomerRecords();
    private final SalesLedger ledger = new SalesLedger();
    private final UndoStack undoStack = new UndoStack();

    public Main(ConsoleInput input) {
        this.input = input;
    }

    // Program entry point. Creates the one and only Scanner and closes it when the program ends.
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            new Main(new ConsoleInput(scanner)).run();
        } finally {
            scanner.close();
        }
    }

    // Loads the sample data, then shows the main menu until the operator exits or input ends.
    public void run() {
        System.out.println("==============================================");
        System.out.println(" Sari-Sari Store Debt and Stock Management");
        System.out.println("==============================================");
        loadSampleData();
        boolean running = true;
        while (running) {
            printMainMenu();
            running = runOneMenuAction();
        }
    }

    // ---------------------------------------------------------------- menu loop

    // Reads one menu choice and runs it. Every error is caught here, so the program keeps running.
    // Returns false when the program should stop.
    private boolean runOneMenuAction() {
        try {
            int choice = input.readMenuChoice(MENU_EXIT);
            return handleMenuChoice(choice);
        } catch (InputCancelledException cancelled) {
            System.out.println(cancelled.getMessage());
        } catch (EndOfInputException endOfInput) {
            System.out.println("No more input. Goodbye! (Session data is not saved.)");
            return false;
        } catch (StoreException storeProblem) {
            ConsoleFormat.printError(storeProblem.getMessage());
        } catch (RuntimeException unexpected) {
            ConsoleFormat.printError("Unexpected error: " + unexpected.getClass().getSimpleName()
                    + (unexpected.getMessage() == null ? "" : " - " + unexpected.getMessage()));
        }
        return true;
    }

    // Runs the chosen menu option. Returns false only for Exit.
    private boolean handleMenuChoice(int choice) throws StoreException {
        switch (choice) {
            case MENU_ADD_PRODUCT:
                addProduct();
                break;
            case MENU_SEARCH_PRODUCT:
                searchProduct();
                break;
            case MENU_ADD_TRANSACTION:
                addTransaction();
                break;
            case MENU_UTANG_HISTORY:
                viewUtangHistory();
                break;
            case MENU_RECORD_PAYMENT:
                recordUtangPayment();
                break;
            case MENU_PRODUCTS_SORTED:
                displayProductsSorted();
                break;
            case MENU_LOW_STOCK:
                showLowStockAlert();
                break;
            case MENU_REPORTS:
                showReports();
                break;
            case MENU_UNDO:
                undoLastTransaction();
                break;
            case MENU_EXIT:
                System.out.println("Goodbye! All changes from this session are now discarded.");
                return false;
            default:
                throw new IllegalStateException("Menu choice was not validated: " + choice);
        }
        return true;
    }

    private void printMainMenu() {
        ConsoleFormat.printHeading("Main Menu");
        System.out.println(" 1. Add product");
        System.out.println(" 2. Search product");
        System.out.println(" 3. Add transaction");
        System.out.println(" 4. View utang history");
        System.out.println(" 5. Record utang payment");
        System.out.println(" 6. Display products sorted");
        System.out.println(" 7. Low-stock alert");
        System.out.println(" 8. Reports");
        System.out.println(" 9. Undo last transaction");
        System.out.println("10. Exit");
    }

    // ---------------------------------------------------------------- 1. add product

    private void addProduct() throws StoreException {
        ConsoleFormat.printHeading("Add Product");
        String code = readNewProductCode();
        String name = readNewProductName();
        BigDecimal price = input.readPrice("Enter price in PHP (example: 12.50)");
        int stock = input.readStock("Enter opening stock (" + Validator.MIN_STOCK + "-" + Validator.MAX_STOCK + ")");
        Product product = inventory.addProduct(code, name, price, stock);
        ConsoleFormat.printOk("Product added: " + product);
    }

    // Asks for a code until it is valid and not used yet.
    private String readNewProductCode() throws InputCancelledException, EndOfInputException {
        while (true) {
            String code = input.readProductCode("Enter product code (1-12 letters, digits or -)");
            try {
                inventory.checkCodeAvailable(code);
                return code;
            } catch (DuplicateRecordException duplicate) {
                ConsoleFormat.printError(duplicate.getMessage());
            }
        }
    }

    // Asks for a name until it is valid and not used yet.
    private String readNewProductName() throws InputCancelledException, EndOfInputException {
        while (true) {
            String name = input.readProductName("Enter product name");
            try {
                inventory.checkNameAvailable(name);
                return name;
            } catch (DuplicateRecordException duplicate) {
                ConsoleFormat.printError(duplicate.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- 2. search product

    // Shows which search found the product: code (linear), exact name (binary) or partial name (linear).
    private void searchProduct() throws StoreException {
        ConsoleFormat.printHeading("Search Product");
        String text = input.readSearchText("Enter product code or name");
        Product byCode = inventory.findByCode(text);
        if (byCode != null) {
            System.out.println("Found by product code (linear search):");
            printProductTable(List.of(byCode));
            return;
        }
        Product byName = inventory.findByName(text);
        if (byName != null) {
            System.out.println("Found by exact name (binary search):");
            printProductTable(List.of(byName));
            return;
        }
        List<Product> matches = inventory.searchPartial(text);
        if (matches.isEmpty()) {
            System.out.println(inventory.noMatchMessage(text) + " " + NO_RECORDS);
            return;
        }
        System.out.println(matches.size() + " product(s) with names containing \"" + text + "\" (linear search):");
        printProductTable(matches);
    }

    // ---------------------------------------------------------------- 3. add transaction

    // Builds a cash or utang sale step by step. Nothing changes until the operator confirms.
    private void addTransaction() throws StoreException {
        ConsoleFormat.printHeading("Add Transaction");
        Product product = selectProduct();
        System.out.println("Selected: " + product);
        product.checkInStock();
        int quantity = readQuantityFor(product);
        PaymentMethod method = readPaymentMethod();
        SaleTransaction sale = createSale(product, quantity, method);
        // Run every save check now, so problems (like a balance above the limit) show before confirming.
        sale.checkCanExecute();
        ConsoleFormat.printHeading("Transaction Details");
        System.out.print(sale.describe());
        System.out.print(ConsoleFormat.detailLine("Stock after sale",
                String.valueOf(product.getStockAfterSelling(quantity))));
        confirmAndSave(sale, "Save this transaction?");
    }

    // Asks for a quantity until it is valid and not more than the stock on hand.
    private int readQuantityFor(Product product) throws InputCancelledException, EndOfInputException {
        while (true) {
            int quantity = input.readQuantity("Enter quantity (1-" + Validator.MAX_QUANTITY
                    + ", available: " + product.getStock() + ")");
            try {
                product.checkCanSell(quantity);
                return quantity;
            } catch (InsufficientStockException | ValidationException cannotSell) {
                ConsoleFormat.printError(cannotSell.getMessage());
            }
        }
    }

    private PaymentMethod readPaymentMethod() throws InputCancelledException, EndOfInputException {
        System.out.println("Payment method:");
        for (PaymentMethod method : PaymentMethod.values()) {
            System.out.println("  " + method.getMenuNumber() + ". " + method.getLabel());
        }
        int number = input.readInt("Choose payment method", PaymentMethod.values().length);
        return PaymentMethod.fromMenuNumber(number);
    }

    // Cash: today's date, no customer. Utang: choose (or create) a customer, then the date.
    private SaleTransaction createSale(Product product, int quantity, PaymentMethod method) throws StoreException {
        LocalDate today = LocalDate.now();
        if (method == PaymentMethod.CASH) {
            return new CashSaleTransaction(today, product, quantity, ledger);
        }
        Customer customer = selectOrCreateCustomer();
        LocalDate date = input.readUtangDate(today);
        return new UtangTransaction(date, product, quantity, customer, customers);
    }

    // Shared by "Add transaction" and "Record utang payment". Uses only the Transaction type.
    private void confirmAndSave(Transaction transaction, String question) throws StoreException {
        if (!input.readYesNo(question)) {
            throw new InputCancelledException();
        }
        // POLYMORPHISM: execute() runs the checks and changes of the actual subclass.
        transaction.execute();
        undoStack.push(transaction);
        ConsoleFormat.printOk("Transaction #" + transaction.getNumber() + " saved.");
        ConsoleFormat.printHeading("Receipt");
        System.out.print(transaction.describe());
        System.out.print(transaction.describeCurrentState());
    }

    // ---------------------------------------------------------------- selecting products and customers

    // PRODUCT SELECTION: exact code -> exact name -> partial name. A product is never guessed.
    private Product selectProduct() throws StoreException {
        // Step 1: read what the operator typed.
        String text = input.readSearchText("Enter product code or name");
        // Step 2: exact code, by linear search (the list is sorted by name, not code).
        Product product = inventory.findByCode(text);
        if (product != null) {
            return product;
        }
        // Step 3: exact name, by binary search on the A-Z master list.
        product = inventory.findByName(text);
        if (product != null) {
            return product;
        }
        // Step 4: partial name, by linear search. If several match, the operator must pick one.
        List<Product> matches = inventory.searchPartial(text);
        if (matches.isEmpty()) {
            throw new RecordNotFoundException(inventory.noMatchMessage(text));
        }
        printNumberedList("Products with names containing \"" + text + "\":", matches);
        return matches.get(input.readInt("Enter the number of the product", matches.size()) - 1);
    }

    // CUSTOMER SELECTION for utang: exact name -> partial name -> offer to create a new customer.
    private Customer selectOrCreateCustomer() throws StoreException {
        while (true) {
            String text = input.readSearchText("Enter customer name");
            Customer customer = findCustomer(text, true);
            if (customer != null) {
                return customer;
            }
            // Nothing chosen: offer to create. "n" cancels the whole transaction.
            if (!input.readYesNo("Customer \"" + text + "\" was not found. Create this customer?")) {
                throw new InputCancelledException();
            }
            try {
                Customer newCustomer = customers.createCustomer(text);
                System.out.println("New customer \"" + newCustomer.getName()
                        + "\" will be added when this transaction is saved.");
                return newCustomer;
            } catch (ValidationException | DuplicateRecordException invalidName) {
                ConsoleFormat.printError(invalidName.getMessage() + " Please enter the name again.");
            }
        }
    }

    // CUSTOMER SELECTION for payments: exact name -> partial name. Never creates a customer.
    private Customer selectExistingCustomer() throws StoreException {
        String text = input.readSearchText("Enter customer name");
        Customer customer = findCustomer(text, false);
        if (customer == null) {
            throw new RecordNotFoundException(customers.noMatchMessage(text));
        }
        return customer;
    }

    // Returns the matching customer, or null if there is no match or the operator chose
    // "create a new customer" (that choice is offered only when allowCreate is true).
    private Customer findCustomer(String text, boolean allowCreate) throws StoreException {
        // Step 1: exact name, by binary search.
        Customer exact = customers.findByName(text);
        if (exact != null) {
            return exact;
        }
        // Step 2: partial name, by linear search.
        List<Customer> matches = customers.searchPartial(text);
        if (matches.isEmpty()) {
            return null;
        }
        // Step 3: the operator must pick exactly one (or 0 to create a new customer instead).
        printNumberedList("Customers with names containing \"" + text + "\":", matches);
        int lowestChoice = 1;
        if (allowCreate) {
            System.out.println("  " + CREATE_NEW_CUSTOMER_CHOICE + ". None of these (new customer \"" + text + "\")");
            lowestChoice = CREATE_NEW_CUSTOMER_CHOICE;
        }
        int choice = input.readInt("Enter the number of the customer", lowestChoice, matches.size());
        return choice == CREATE_NEW_CUSTOMER_CHOICE ? null : matches.get(choice - 1);
    }

    // Prints "1. item", "2. item", ... using each item's toString().
    private <T> void printNumberedList(String title, List<T> items) {
        System.out.println(title);
        for (int index = 0; index < items.size(); index++) {
            System.out.println("  " + (index + 1) + ". " + items.get(index));
        }
    }

    // ---------------------------------------------------------------- 4. view utang history

    private void viewUtangHistory() throws StoreException {
        while (true) {
            ConsoleFormat.printHeading("View Utang History");
            System.out.println("1. Show all");
            System.out.println("2. Show unpaid");
            System.out.println("3. Show paid");
            System.out.println("4. Back");
            int choice = input.readMenuChoice(HISTORY_MENU_BACK);
            if (choice == HISTORY_MENU_BACK) {
                return;
            }
            showUtangHistory(HistoryFilter.values()[choice - 1]);
        }
    }

    // Prints the records (customer A-Z, then oldest date first), their payments and a summary.
    private void showUtangHistory(HistoryFilter filter) {
        ConsoleFormat.printHeading(filter.getTitle());
        List<UtangTransaction> records = customers.getUtangHistory(filter);
        if (records.isEmpty()) {
            System.out.println(NO_RECORDS);
            return;
        }
        System.out.println(UtangTransaction.TABLE_HEADER);
        System.out.println(ConsoleFormat.underline(UtangTransaction.TABLE_HEADER));
        for (UtangTransaction record : records) {
            System.out.println(record.getHistoryRow());
            printPayments(record);
        }
        printHistorySummary(records);
    }

    private void printPayments(UtangTransaction record) {
        for (PaymentTransaction payment : record.getPayments()) {
            System.out.println(String.format(Locale.US, "       - Payment #%d on %s: %s%s", payment.getNumber(),
                    Validator.formatDate(payment.getDate()), Money.format(payment.getAmount()),
                    payment.isUndone() ? "  [UNDONE]" : ""));
        }
    }

    // Undone records are listed in "Show all" but never counted in the totals.
    private void printHistorySummary(List<UtangTransaction> records) {
        BigDecimal totalUtang = Money.ZERO;
        BigDecimal totalPaid = Money.ZERO;
        BigDecimal totalRemaining = Money.ZERO;
        int undoneCount = 0;
        for (UtangTransaction record : records) {
            if (record.isUndone()) {
                undoneCount++;
                continue;
            }
            totalUtang = totalUtang.add(record.getTotalAmount());
            totalPaid = totalPaid.add(record.getAmountPaid());
            totalRemaining = totalRemaining.add(record.getRemaining());
        }
        System.out.println();
        String undoneNote = undoneCount == 0 ? "" : " (" + undoneCount + " undone, not included in totals)";
        System.out.print(ConsoleFormat.detailLine("Records shown", records.size() + undoneNote));
        System.out.print(ConsoleFormat.detailLine("Total utang", Money.format(totalUtang)));
        System.out.print(ConsoleFormat.detailLine("Total paid", Money.format(totalPaid)));
        System.out.print(ConsoleFormat.detailLine("Total remaining", Money.format(totalRemaining)));
    }

    // ---------------------------------------------------------------- 5. record utang payment

    private void recordUtangPayment() throws StoreException {
        ConsoleFormat.printHeading("Record Utang Payment");
        Customer customer = selectExistingCustomer();
        System.out.println(customer.getName() + "'s current balance: " + Money.format(customer.getBalance()));
        List<UtangTransaction> unpaid = customer.requireUnpaidRecords();
        printUnpaidRecords(unpaid);
        UtangTransaction record = unpaid.get(input.readInt("Enter the number of the record to pay", unpaid.size()) - 1);
        BigDecimal amount = input.readPaymentAmount("Enter payment amount in PHP (up to "
                + Money.format(record.getRemaining()) + ")", record.getRemaining());
        PaymentTransaction payment = new PaymentTransaction(LocalDate.now(), record, amount, ledger);
        payment.checkCanExecute();
        ConsoleFormat.printHeading("Payment Details");
        System.out.print(payment.describe());
        System.out.print(ConsoleFormat.detailLine("Remaining now", Money.format(record.getRemaining())));
        System.out.print(ConsoleFormat.detailLine("Remaining after payment",
                Money.format(record.getRemaining().subtract(amount))));
        confirmAndSave(payment, "Save this payment?");
    }

    private void printUnpaidRecords(List<UtangTransaction> unpaid) {
        System.out.println("Unpaid utang records:");
        System.out.println("     " + UtangTransaction.TABLE_HEADER);
        for (int index = 0; index < unpaid.size(); index++) {
            System.out.println(String.format(Locale.US, "%3d. %s", index + 1, unpaid.get(index).getHistoryRow()));
        }
    }

    // ---------------------------------------------------------------- 6. display products sorted

    // Every option sorts a COPY with merge sort; the master list always stays A-Z.
    private void displayProductsSorted() throws StoreException {
        while (true) {
            ConsoleFormat.printHeading("Display Products Sorted");
            System.out.println("1. Name (A-Z)");
            System.out.println("2. Price (low to high)");
            System.out.println("3. Stock (low to high)");
            System.out.println("4. Best sellers (most sold first)");
            System.out.println("5. Back");
            int choice = input.readMenuChoice(SORT_MENU_BACK);
            switch (choice) {
                case SORT_BY_NAME:
                    printProductSection("Products by name (A-Z)", inventory.getSortedByName());
                    break;
                case SORT_BY_PRICE:
                    printProductSection("Products by price (low to high)", inventory.getSortedByPrice());
                    break;
                case SORT_BY_STOCK:
                    printProductSection("Products by stock (low to high)", inventory.getSortedByStock());
                    break;
                case SORT_BY_BEST_SELLERS:
                    printProductSection("Best sellers (most sold first)", inventory.getBestSellers());
                    break;
                case SORT_MENU_BACK:
                    return;
                default:
                    throw new IllegalStateException("Sort choice was not validated: " + choice);
            }
        }
    }

    // ---------------------------------------------------------------- 7. low-stock alert

    private void showLowStockAlert() {
        ConsoleFormat.printHeading("Low-Stock Alert (stock of " + Inventory.LOW_STOCK_LEVEL + " or less)");
        List<Product> lowStock = inventory.getLowStockProducts();
        if (lowStock.isEmpty()) {
            System.out.println("No low-stock products. Every product has more than "
                    + Inventory.LOW_STOCK_LEVEL + " in stock.");
            return;
        }
        System.out.println(Product.TABLE_HEADER + "  Alert");
        System.out.println(ConsoleFormat.underline(Product.TABLE_HEADER + "  Alert"));
        for (Product product : lowStock) {
            System.out.println(product.getSummaryLine() + "  " + (product.isOutOfStock() ? "OUT OF STOCK" : "LOW STOCK"));
        }
    }

    // ---------------------------------------------------------------- 8. reports

    private void showReports() {
        ConsoleFormat.printHeading("Reports");
        System.out.print(ConsoleFormat.detailLine("Cash sales (session)", Money.format(ledger.getCashSalesTotal())));
        System.out.print(ConsoleFormat.detailLine("Utang payments collected",
                Money.format(ledger.getPaymentsCollectedTotal())));
        System.out.print(ConsoleFormat.detailLine("Total outstanding utang", Money.format(customers.getTotalOutstanding())));
        System.out.print(ConsoleFormat.detailLine("Unpaid utang records",
                String.valueOf(customers.countUtangRecords(HistoryFilter.UNPAID))));
        System.out.print(ConsoleFormat.detailLine("Paid utang records",
                String.valueOf(customers.countUtangRecords(HistoryFilter.PAID))));
        printTopSellers();
        printCustomersWithBalance();
    }

    private void printTopSellers() {
        System.out.println();
        System.out.println("Top " + TOP_SELLER_COUNT + " best-selling products (by quantity sold):");
        List<Product> bestSellers = inventory.getBestSellers();
        if (bestSellers.isEmpty()) {
            System.out.println(NO_RECORDS);
            return;
        }
        int shown = Math.min(TOP_SELLER_COUNT, bestSellers.size());
        printProductTable(bestSellers.subList(0, shown));
        if (shown < TOP_SELLER_COUNT) {
            System.out.println(shown == 1 ? "Only 1 product has sales so far."
                    : "Only " + shown + " products have sales so far.");
        }
    }

    private void printCustomersWithBalance() {
        System.out.println();
        System.out.println("Customers with outstanding utang (highest balance first):");
        List<Customer> withBalance = customers.getCustomersWithBalance();
        if (withBalance.isEmpty()) {
            System.out.println(NO_RECORDS);
            return;
        }
        System.out.println(Customer.TABLE_HEADER);
        System.out.println(ConsoleFormat.underline(Customer.TABLE_HEADER));
        for (Customer customer : withBalance) {
            System.out.println(customer.getSummaryLine());
        }
    }

    // ---------------------------------------------------------------- 9. undo

    // UNDO WORKFLOW: peek -> show -> confirm -> reverse -> pop. The stack is LIFO, so the most
    // recently saved transaction is always the one undone, whatever its date.
    private void undoLastTransaction() throws StoreException {
        ConsoleFormat.printHeading("Undo Last Transaction");
        // Step 1: look at the top transaction WITHOUT removing it (throws if the stack is empty).
        Transaction last = undoStack.peek();
        System.out.println("Most recent transaction (" + undoStack.size() + " on the undo stack):");
        System.out.print(last.describe());
        // Step 2: ask first. "n" or /cancel leaves everything, including the stack, unchanged.
        if (!input.readYesNo("Undo this transaction?")) {
            throw new InputCancelledException();
        }
        // Step 3: POLYMORPHISM - reverse() runs the undo rules of the real subclass
        // (cash sale, utang or payment). There is no if/switch on the type here.
        // If a check fails it throws, and nothing (not even the stack) has changed.
        last.reverse();
        // Step 4: only after a successful reverse() is the transaction removed from the stack.
        undoStack.pop();
        ConsoleFormat.printOk("Transaction #" + last.getNumber() + " (" + last.getType().getLabel() + ") undone.");
        System.out.print(last.describeCurrentState());
    }

    // ---------------------------------------------------------------- shared printing

    private void printProductSection(String title, List<Product> products) {
        ConsoleFormat.printHeading(title);
        if (products.isEmpty()) {
            System.out.println(NO_RECORDS);
            return;
        }
        printProductTable(products);
    }

    private void printProductTable(List<Product> products) {
        System.out.println(Product.TABLE_HEADER);
        System.out.println(ConsoleFormat.underline(Product.TABLE_HEADER));
        for (Product product : products) {
            System.out.println(product.getSummaryLine());
        }
    }

    // ---------------------------------------------------------------- sample data

    // SAMPLE DATA: 7 products and 3 customers with no utang. Nothing is saved to a file,
    // so every change made during the session is lost when the program exits.
    private void loadSampleData() {
        try {
            inventory.addProduct("P001", "Milo Sachet", new BigDecimal("9.00"), 30);
            inventory.addProduct("P002", "Egg", new BigDecimal("10.00"), 4);
            inventory.addProduct("P003", "Lucky Me Pancit Canton", new BigDecimal("16.00"), 25);
            inventory.addProduct("P004", "Coke Sakto", new BigDecimal("15.00"), 12);
            inventory.addProduct("P005", "Datu Puti Vinegar", new BigDecimal("22.00"), 8);
            inventory.addProduct("P006", "Safeguard Shampoo Sachet", new BigDecimal("7.00"), 0);
            inventory.addProduct("P007", "Safeguard Soap", new BigDecimal("38.00"), 3);
            customers.addCustomer("Aling Nena");
            customers.addCustomer("Mang Tonyo");
            customers.addCustomer("Ate Lorna");
        } catch (StoreException badSampleData) {
            // The sample data is fixed in the code, so a failure here is a programmer mistake.
            throw new IllegalStateException("Sample data is invalid: " + badSampleData.getMessage(), badSampleData);
        }
        System.out.println("Sample data loaded: " + inventory.size() + " products and "
                + customers.size() + " customers.");
        System.out.println("All changes are kept in memory only and are lost when you exit.");
        System.out.println("Type " + Validator.CANCEL_COMMAND + " at any prompt inside an option to cancel it.");
    }
}
