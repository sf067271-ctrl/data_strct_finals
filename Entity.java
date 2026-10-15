import java.util.Objects;

// Abstract parent class of Product and Customer. Holds the display name and the name key.
// Demonstrates ABSTRACTION (abstract getSummaryLine()) and INHERITANCE (Product and Customer extend it).
public abstract class Entity {
    private String name;
    private String nameKey;

    protected Entity(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.nameKey = Validator.toNameKey(name);
    }

    // THE one comparison rule for names. Merge sort and binary search both use it,
    // so the order the list is sorted in is exactly the order binary search expects.
    public static int compareNameKeys(String firstKey, String secondKey) {
        return firstKey.compareTo(secondKey);
    }

    public String getName() {
        return name;
    }

    // The cleaned, lowercase name used for comparing and searching.
    public String getNameKey() {
        return nameKey;
    }

    // Returns this record as one formatted table row. Each subclass decides its own columns.
    public abstract String getSummaryLine();

    // Lets a subclass change the name while keeping the name key in step with it.
    protected final void changeName(String newName) {
        this.name = Objects.requireNonNull(newName, "newName");
        this.nameKey = Validator.toNameKey(newName);
    }
}
