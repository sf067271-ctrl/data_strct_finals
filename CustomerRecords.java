import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stores and manages all utang customers.
 *
 * Like Inventory, the master list is kept in ALPHABETICAL order by name so
 * exact names can be found with binary search.
 *
 * OOP - ENCAPSULATION: the ArrayList is private; other classes get copies.
 */
public class CustomerRecords {

    /** Highest balance first. */
    public static final Comparator<Customer> BY_BALANCE_DESC = new Comparator<Customer>() {
        @Override
        public int compare(Customer a, Customer b) {
            return b.getBalance().compareTo(a.getBalance());
        }
    };

    private final ArrayList<Customer> customers = new ArrayList<Customer>();

    /**
     * Adds a customer, then uses MERGE SORT to restore alphabetical order.
     *
     * @throws DuplicateRecordException if a customer with the same name exists
     *         (ignoring upper/lower case and surrounding spaces)
     */
    public void addCustomer(Customer customer) throws DuplicateRecordException {
        if (customer == null) {
            throw new IllegalArgumentException("The customer cannot be empty.");
        }
        Customer existing = findByName(customer.getName());
        if (existing != null) {
            throw new DuplicateRecordException("A customer named \"" + existing.getName() + "\" already exists.");
        }
        customers.add(customer);
        Algorithms.mergeSort(customers, Algorithms.BY_NAME);
    }

    /**
     * Removes a customer. Used to roll back a NEW customer if their first
     * utang could not be recorded.
     *
     * @return true if the customer was removed
     */
    public boolean removeCustomer(Customer customer) {
        return customers.remove(customer);
    }

    /** Exact name lookup using BINARY SEARCH. Returns null if not found. */
    public Customer findByName(String name) {
        if (TextRules.clean(name).isEmpty()) {
            return null;
        }
        int index = Algorithms.binarySearch(customers, Algorithms.nameKey(name), Algorithms.BY_NAME);
        return index >= 0 ? customers.get(index) : null;
    }

    /** Partial name lookup using LINEAR SEARCH, e.g. "nena" finds "Aling Nena". */
    public List<Customer> searchPartial(String keyword) {
        final String wanted = TextRules.clean(keyword).toLowerCase();
        if (wanted.isEmpty()) {
            return new ArrayList<Customer>();
        }
        return Algorithms.linearSearchAll(customers, new Algorithms.Condition<Customer>() {
            @Override
            public boolean matches(Customer c) {
                return c.getName().toLowerCase().contains(wanted);
            }
        });
    }

    /** Customers who still owe money, highest balance first (a sorted copy). */
    public List<Customer> getCustomersWithUtang() {
        List<Customer> owing = Algorithms.linearSearchAll(customers, new Algorithms.Condition<Customer>() {
            @Override
            public boolean matches(Customer c) {
                return c.hasOutstandingUtang();
            }
        });
        Algorithms.mergeSort(owing, BY_BALANCE_DESC);
        return owing;
    }

    /** The sum of all customers' balances. */
    public BigDecimal getTotalOutstanding() {
        BigDecimal total = Money.ZERO;
        for (Customer c : customers) {
            total = total.add(c.getBalance());
        }
        return total;
    }

    /** All customers in alphabetical order (a copy). */
    public List<Customer> getAllCustomers() {
        return new ArrayList<Customer>(customers);
    }

    public int size() {
        return customers.size();
    }

    public boolean isEmpty() {
        return customers.isEmpty();
    }
}
