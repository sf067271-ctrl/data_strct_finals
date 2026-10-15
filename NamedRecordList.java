import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// Abstract generic master list of products or customers, always kept sorted by name.
// Demonstrates ABSTRACTION (hides the list and the algorithms), INHERITANCE (Inventory and
// CustomerRecords extend it) and POLYMORPHISM through generics (works for any Entity subtype).
public abstract class NamedRecordList<T extends Entity> {
    private final List<T> records = new ArrayList<>();
    // Sorting by name uses the same rule as binary search: Entity.compareNameKeys().
    private final Comparator<T> nameOrder =
            (first, second) -> Entity.compareNameKeys(first.getNameKey(), second.getNameKey());
    private final String recordLabel;

    protected NamedRecordList(String recordLabel) {
        this.recordLabel = Objects.requireNonNull(recordLabel, "recordLabel");
    }

    // Finds a record by its exact name (ignoring case and extra spaces) using BINARY SEARCH.
    // Returns null if no record has that name.
    public T findByName(String name) {
        int index = Algorithms.binarySearch(records, Validator.toNameKey(name));
        return index >= 0 ? records.get(index) : null;
    }

    // LINEAR SEARCH: checks every record one by one for names containing the keyword. Time: O(n).
    // Used when the exact-name search fails. Returns an empty list (never null) if nothing matches.
    public List<T> searchPartial(String keyword) {
        // Step 1: compare in lowercase so "PAN" also finds "Pancit".
        String keywordKey = Validator.toNameKey(keyword);
        List<T> matches = new ArrayList<>();
        // Step 2: visit the records in master-list order (A-Z), so matches come out A-Z too.
        for (T record : records) {
            // Step 3: String.contains() checks the characters of ONE name; the search over
            // the list is our own loop.
            if (record.getNameKey().contains(keywordKey)) {
                matches.add(record);
            }
        }
        return matches;
    }

    // Throws if another record already uses this name (ignoring case and extra spaces).
    public void checkNameAvailable(String name) throws DuplicateRecordException {
        T existing = findByName(name);
        if (existing != null) {
            throw new DuplicateRecordException("A " + recordLabel + " named \"" + existing.getName()
                    + "\" already exists.");
        }
    }

    // Returns a read-only view of the master list, sorted A-Z. Callers cannot change it.
    public List<T> getAll() {
        return Collections.unmodifiableList(records);
    }

    // Builds the message shown when nothing matches the search text.
    public String noMatchMessage(String searchText) {
        return "No " + recordLabel + " matches \"" + searchText + "\".";
    }

    // Returns the number of records.
    public int size() {
        return records.size();
    }

    // Adds a record, then restores alphabetical order with merge sort so binary search keeps working.
    protected void addRecord(T record) {
        records.add(Objects.requireNonNull(record, "record"));
        restoreNameOrder();
    }

    // Removes a record by finding its position with binary search.
    protected void removeRecord(T record) {
        int index = Algorithms.binarySearch(records, record.getNameKey());
        if (index < 0 || records.get(index) != record) {
            throw new IllegalStateException("The " + recordLabel + " is not in the list.");
        }
        records.remove(index);
    }

    // Sorts the master list by name again (needed after a name changes).
    protected void restoreNameOrder() {
        Algorithms.mergeSort(records, nameOrder);
    }

    // Returns a sorted COPY of the master list. The master list keeps its A-Z order.
    protected List<T> sortedCopy(Comparator<T> comparator) {
        return Algorithms.sortedCopy(records, comparator);
    }

    protected Comparator<T> getNameOrder() {
        return nameOrder;
    }

}
