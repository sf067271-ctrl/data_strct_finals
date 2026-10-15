import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// All customers, kept sorted A-Z, plus queries over their utang records.
// Demonstrates INHERITANCE (extends NamedRecordList<Customer>) and ABSTRACTION (Main never sees the list).
public class CustomerRecords extends NamedRecordList<Customer> {
    private static final Comparator<Customer> BY_BALANCE_HIGH_TO_LOW =
            (first, second) -> second.getBalance().compareTo(first.getBalance());

    public CustomerRecords() {
        super("customer");
    }

    // Validates the name and adds a new customer right away (used for sample data).
    public Customer addCustomer(String rawName) throws ValidationException, DuplicateRecordException {
        Customer customer = createCustomer(rawName);
        addRecord(customer);
        return customer;
    }

    // Validates the name and creates a customer WITHOUT adding it. The customer is added later,
    // by UtangTransaction, only when the first utang is saved.
    public Customer createCustomer(String rawName) throws ValidationException, DuplicateRecordException {
        String name = Validator.validateCustomerName(rawName);
        checkNameAvailable(name);
        return new Customer(name);
    }

    // Returns true if this exact customer object is already in the list.
    public boolean isRegistered(Customer customer) {
        return findByName(customer.getName()) == customer;
    }

    // Returns customers who still owe money, highest balance first (a sorted copy).
    public List<Customer> getCustomersWithBalance() {
        List<Customer> withBalance = new ArrayList<>();
        for (Customer customer : getAll()) {
            if (Money.isPositive(customer.getBalance())) {
                withBalance.add(customer);
            }
        }
        Algorithms.mergeSort(withBalance, BY_BALANCE_HIGH_TO_LOW);
        return withBalance;
    }

    // Adds up every customer's computed balance.
    public BigDecimal getTotalOutstanding() {
        BigDecimal total = Money.ZERO;
        for (Customer customer : getAll()) {
            total = total.add(customer.getBalance());
        }
        return total;
    }

    // Returns the utang records that match the filter, ordered by customer name A-Z, then date.
    public List<UtangTransaction> getUtangHistory(HistoryFilter filter) {
        List<UtangTransaction> history = new ArrayList<>();
        for (Customer customer : getAll()) {
            for (UtangTransaction record : customer.getUtangRecords()) {
                if (filter.matches(record)) {
                    history.add(record);
                }
            }
        }
        // Two merge sorts on this copy: first by date, then by customer name. Merge sort is STABLE,
        // so the second sort keeps the date order among records of the same customer.
        // Result: customer A-Z, and oldest date first within each customer.
        Algorithms.mergeSort(history, UtangTransaction.BY_DATE);
        Algorithms.mergeSort(history, UtangTransaction.BY_CUSTOMER_NAME);
        return history;
    }

    // Counts the utang records that match the filter.
    public int countUtangRecords(HistoryFilter filter) {
        return getUtangHistory(filter).size();
    }

    // Package-private: only UtangTransaction.applyExecution() calls this, after its checks pass.
    void register(Customer customer) {
        addRecord(customer);
    }
}
