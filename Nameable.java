/**
 * Anything that has a name: a Product or a Customer.
 *
 * OOP - ABSTRACTION (interface):
 *   The searching and sorting code in Algorithms only needs to know that an
 *   object HAS a name. It does not care whether it is a product or a
 *   customer. This interface hides those details, so ONE binary search and
 *   ONE name comparator work for both classes.
 *
 * OOP - POLYMORPHISM:
 *   A List<Product> and a List<Customer> can both be searched with
 *   Algorithms.BY_NAME, because Product and Customer both implement this
 *   interface and each provides its own getName().
 */
public interface Nameable {

    /** Returns the name used for searching and alphabetical sorting. */
    String getName();
}
