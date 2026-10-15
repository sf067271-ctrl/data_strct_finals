import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Our own merge sort and binary search (no library sort or search is used).
// Demonstrates POLYMORPHISM through generics: mergeSort() works for any type with any Comparator.
public final class Algorithms {
    private Algorithms() {
        // Utility class: no objects needed.
    }

    // MERGE SORT: split the list in half, sort each half, then merge the two sorted halves.
    // Stable (equal items keep their original order). Time: O(n log n). Sorts the given list in place.
    public static <T> void mergeSort(List<T> list, Comparator<T> comparator) {
        // Step 1: a list with 0 or 1 items is already sorted. This stops the recursion.
        if (list.size() <= 1) {
            return;
        }
        // Step 2: split the list into a left half and a right half (copies).
        int middle = list.size() / 2;
        List<T> left = new ArrayList<>(list.subList(0, middle));
        List<T> right = new ArrayList<>(list.subList(middle, list.size()));
        // Step 3: sort each half the same way (recursion).
        mergeSort(left, comparator);
        mergeSort(right, comparator);
        // Step 4: merge the two sorted halves back into the original list.
        merge(left, right, list, comparator);
    }

    // Returns a sorted COPY, leaving the original list unchanged (used for reports and displays).
    public static <T> List<T> sortedCopy(List<T> original, Comparator<T> comparator) {
        List<T> copy = new ArrayList<>(original);
        mergeSort(copy, comparator);
        return copy;
    }

    // BINARY SEARCH: look at the middle item and throw away the half that cannot contain the target.
    // Works ONLY on a list sorted by name key with Entity.compareNameKeys(). Time: O(log n).
    // Returns the index of the matching item, or -1 if it is not in the list.
    public static <T extends Entity> int binarySearch(List<T> sortedList, String targetNameKey) {
        // low and high mark the part of the list that may still contain the target.
        int low = 0;
        int high = sortedList.size() - 1;
        while (low <= high) {
            // mid is the item in the middle of that part. (low + high) / 2 could overflow,
            // so we write it as low + (high - low) / 2.
            int mid = low + (high - low) / 2;
            int comparison = Entity.compareNameKeys(sortedList.get(mid).getNameKey(), targetNameKey);
            if (comparison == 0) {
                return mid;           // Found it.
            }
            if (comparison < 0) {
                low = mid + 1;        // Middle name comes BEFORE the target: search the right half.
            } else {
                high = mid - 1;       // Middle name comes AFTER the target: search the left half.
            }
        }
        // The range became empty, so the target is not in the list. This shortcut is only
        // correct because the list is sorted; on an unsorted list it could miss real matches.
        return -1;
    }

    // MERGE STEP: repeatedly take the smaller front item of the two sorted halves.
    private static <T> void merge(List<T> left, List<T> right, List<T> destination,
            Comparator<T> comparator) {
        int leftIndex = 0;
        int rightIndex = 0;
        int destinationIndex = 0;
        // Step 1: while both halves still have items, copy the smaller front item.
        while (leftIndex < left.size() && rightIndex < right.size()) {
            T leftItem = left.get(leftIndex);
            T rightItem = right.get(rightIndex);
            // "<= 0" takes the LEFT item when both are equal. This is what makes the sort stable.
            if (comparator.compare(leftItem, rightItem) <= 0) {
                destination.set(destinationIndex, leftItem);
                leftIndex++;
            } else {
                destination.set(destinationIndex, rightItem);
                rightIndex++;
            }
            destinationIndex++;
        }
        // Step 2: one half is used up; copy whatever is left in the other half.
        while (leftIndex < left.size()) {
            destination.set(destinationIndex, left.get(leftIndex));
            leftIndex++;
            destinationIndex++;
        }
        while (rightIndex < right.size()) {
            destination.set(destinationIndex, right.get(rightIndex));
            rightIndex++;
            destinationIndex++;
        }
    }
}
