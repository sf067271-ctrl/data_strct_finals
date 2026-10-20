import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The searching and sorting algorithms of the system, written by hand.
 * (No Collections.sort(), List.sort(), Arrays.sort() or library searches.)
 *
 *   binarySearch()    - exact name lookups            O(log n)
 *   linearSearch()    - product code lookup           O(n)
 *   linearSearchAll() - partial names, low stock      O(n)
 *   mergeSort()       - every sorted list             O(n log n), stable
 *
 * The methods are GENERIC (<T>): the same code works for products and
 * customers. A Comparator tells the method HOW to compare two elements
 * (by name, by price, by stock, ...), so one merge sort supports every
 * sort order in the program.
 */
public final class Algorithms {

    /**
     * Compares two products or customers by name, ignoring upper/lower case
     * and extra spaces. The SAME rule is used for sorting and for binary
     * search, which binary search requires.
     */
    public static final Comparator<Nameable> BY_NAME = new Comparator<Nameable>() {
        @Override
        public int compare(Nameable a, Nameable b) {
            return compareNames(a.getName(), b.getName());
        }
    };

    private Algorithms() {
        // Utility class: no objects needed.
    }

    /** Case-insensitive name comparison, ignoring surrounding and repeated spaces. */
    public static int compareNames(String a, String b) {
        return TextRules.clean(a).compareToIgnoreCase(TextRules.clean(b));
    }

    /**
     * Wraps a typed name in a small Nameable object so it can be compared
     * with products or customers using BY_NAME during binary search.
     * (An anonymous class implementing the Nameable interface.)
     */
    public static Nameable nameKey(String name) {
        final String cleaned = TextRules.clean(name);
        return new Nameable() {
            @Override
            public String getName() {
                return cleaned;
            }
        };
    }

    // =====================================================================
    //  BINARY SEARCH
    // =====================================================================

    /**
     * BINARY SEARCH: finds the key in a list that is SORTED by the same comparator.
     *
     * How it works:
     *   1. Look at the MIDDLE element.
     *   2. If it equals the key, we are done.
     *   3. If the key is smaller, it can only be in the LEFT half: last = middle - 1.
     *      If the key is bigger, it can only be in the RIGHT half: first = middle + 1.
     *   4. Repeat until found, or until first > last (not in the list).
     * Each step throws away half of the remaining list, so even 1,000 items
     * need at most about 10 comparisons.
     *
     * @return the index of the key, or -1 if it is not in the list
     */
    public static <T> int binarySearch(List<? extends T> sortedList, T key, Comparator<? super T> comparator) {
        int first = 0;
        int last = sortedList.size() - 1;

        while (first <= last) {
            int middle = first + (last - first) / 2;   // same as (first + last) / 2, but cannot overflow
            int result = comparator.compare(sortedList.get(middle), key);

            if (result == 0) {
                return middle;            // found
            } else if (result < 0) {
                first = middle + 1;       // middle is smaller than the key: search the right half
            } else {
                last = middle - 1;        // middle is bigger than the key: search the left half
            }
        }
        return -1;                        // not found
    }

    // =====================================================================
    //  LINEAR SEARCH
    // =====================================================================

    /**
     * LINEAR (sequential) SEARCH: checks the elements one by one, from the
     * first to the last, and returns the index of the FIRST one that matches.
     * It works even when the list is NOT sorted by what we are looking for
     * (for example, the product list is sorted by name, not by code).
     *
     * @return the index of the first match, or -1 if nothing matches
     */
    public static <T> int linearSearch(List<T> list, Condition<? super T> condition) {
        for (int i = 0; i < list.size(); i++) {
            if (condition.matches(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * LINEAR SEARCH for ALL matches: visits every element once and collects
     * each one that matches. Used for partial names ("pan" finds
     * "Lucky Me Pancit Canton") and low-stock items. Binary search cannot do
     * this, because a partial match can be anywhere in the list.
     *
     * @return a new list with all matches, in their original order (may be empty)
     */
    public static <T> List<T> linearSearchAll(List<T> list, Condition<? super T> condition) {
        List<T> matches = new ArrayList<T>();
        for (int i = 0; i < list.size(); i++) {
            if (condition.matches(list.get(i))) {
                matches.add(list.get(i));
            }
        }
        return matches;
    }

    // =====================================================================
    //  MERGE SORT
    // =====================================================================

    /**
     * Sorts the list IN PLACE with merge sort.
     *
     * MERGE SORT (divide and conquer):
     *   1. A list of 0 or 1 element is already sorted.
     *   2. DIVIDE the list into a left half and a right half.
     *   3. Sort each half (by calling merge sort again: recursion).
     *   4. MERGE the two sorted halves into one sorted list.
     *
     * It is STABLE: when two elements are equal (e.g. two products with the
     * same price), they keep their original order.
     */
    public static <T> void mergeSort(List<T> list, Comparator<? super T> comparator) {
        if (list.size() > 1) {
            mergeSort(list, 0, list.size() - 1, comparator);
        }
    }

    /**
     * Returns a SORTED COPY and leaves the original list untouched. Used for
     * reports and other display orders, so the master lists stay alphabetical.
     */
    public static <T> List<T> sortedCopy(List<T> list, Comparator<? super T> comparator) {
        List<T> copy = new ArrayList<T>(list);
        mergeSort(copy, comparator);
        return copy;
    }

    /** Sorts the part of the list from index left to index right. */
    private static <T> void mergeSort(List<T> list, int left, int right, Comparator<? super T> comparator) {
        if (left >= right) {
            return;                                     // 0 or 1 element: already sorted
        }
        int middle = left + (right - left) / 2;
        mergeSort(list, left, middle, comparator);      // sort the left half
        mergeSort(list, middle + 1, right, comparator); // sort the right half
        merge(list, left, middle, right, comparator);   // combine both halves
    }

    /**
     * Merges two neighbouring sorted parts: list[left..middle] and list[middle+1..right].
     *
     * Copies both halves into temporary lists, then repeatedly takes the
     * smaller front element of the two halves and writes it back.
     */
    private static <T> void merge(List<T> list, int left, int middle, int right, Comparator<? super T> comparator) {
        // Temporary copies of the two halves
        List<T> leftHalf = new ArrayList<T>();
        List<T> rightHalf = new ArrayList<T>();
        for (int i = left; i <= middle; i++) {
            leftHalf.add(list.get(i));
        }
        for (int j = middle + 1; j <= right; j++) {
            rightHalf.add(list.get(j));
        }

        int i = 0;      // position in leftHalf
        int j = 0;      // position in rightHalf
        int k = left;   // position in the original list

        while (i < leftHalf.size() && j < rightHalf.size()) {
            // "<= 0" takes from the LEFT half when both are equal.
            // This is what makes merge sort STABLE.
            if (comparator.compare(leftHalf.get(i), rightHalf.get(j)) <= 0) {
                list.set(k, leftHalf.get(i));
                i++;
            } else {
                list.set(k, rightHalf.get(j));
                j++;
            }
            k++;
        }

        // Copy whatever is left over (only one of these loops will run)
        while (i < leftHalf.size()) {
            list.set(k, leftHalf.get(i));
            i++;
            k++;
        }
        while (j < rightHalf.size()) {
            list.set(k, rightHalf.get(j));
            j++;
            k++;
        }
    }

    // =====================================================================
    //  Condition: a simple "yes or no" test used by linear search
    // =====================================================================

    /**
     * A test that says whether an element matches, e.g. "is the stock 5 or
     * fewer?" (ABSTRACTION: linear search does not need to know what is being
     * tested, only that it can ask "does this element match?").
     */
    public interface Condition<T> {
        boolean matches(T element);
    }
}
