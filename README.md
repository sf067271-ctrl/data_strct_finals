# Sari-Sari Store Debt and Stock Management System

A terminal-based Java application (Java 8 or newer, standard library only)
for recording a sari-sari store's products, cash sales, utang (credit) and
utang payments, with undo.

## Compile and run

Open a terminal in this folder:

```
javac *.java
java Main
```

**In VS Code:** choose File > Open Folder and select this folder. Then
open `Main.java` and click **Run** (or press F5 and pick
*Run Sari-Sari Store System*). Type your answers in the **TERMINAL** panel.

## Menu options

| No. | Option | What it does |
|----|----|----|
| 1 | Add product | Adds a product with a unique code and name, a positive price and an opening stock (0 allowed). |
| 2 | Search product | Exact name (binary search), then exact code (linear search), then partial names (linear search). |
| 3 | Record cash sale | Sells one product for cash and prints a receipt. |
| 4 | Record utang | Sells one product on credit. A new customer can be created on the spot. |
| 5 | Record utang payment | Lowers a customer's balance. Overpayments are rejected. |
| 6 | Search customer | Shows the name, balance and full history (ACTIVE or UNDONE entries). |
| 7 | Display products sorted | By name, price, stock, or best sellers, using merge sort. |
| 8 | Low-stock alert | Products with 5 or fewer pieces (including 0), lowest stock first. |
| 9 | Reports | Session cash sales, total outstanding utang, top 3 best sellers, customers with utang. |
| 10 | Undo last transaction | Reverses the most recent cash sale, utang, payment or restock (stack, LIFO). |
| 11 | Exit | Ends the program after a confirmation. |
| 12 | Restock product | Adds the quantity received to an existing product's stock after confirmation. |

**Restocking:** choose option `12`, select a product by code or name, and enter
the number of pieces received. For example, Milo with 4 pieces plus a delivery
of 20 becomes 24 pieces. Stock cannot exceed 1,000,000 pieces. Restocking does
not change sales or customer balances and can be reversed with option `10`.

**Cancelling:** type `X` at any prompt inside an operation to cancel it and
return to the main menu. Nothing is changed by a cancelled operation.

## Sample data

Loaded automatically at startup (labelled `[SAMPLE DATA]` on screen):

- 7 products, codes **P001 to P007**: Milo Sachet (PHP 9.00), Egg (PHP 10.00),
  Lucky Me Pancit Canton (low stock), Kopiko Coffee Candy, Bear Brand Sachet
  (low stock), Datu Puti Vinegar 200ml (zero stock), Coke Mismo.
- 3 customers with zero balances and empty histories: Aling Nena, Mang Tonyo, Ate Joy.

## In-memory limitation

All records are kept in memory (`ArrayList`). Nothing is saved to a file or
database, so every change disappears when the program exits. Each run starts
again from the sample data.

## Where each algorithm and data structure is implemented

| Requirement | Where |
|----|----|
| Binary search | `Algorithms.binarySearch()`, used by `Inventory.findByName()` and `CustomerRecords.findByName()` |
| Linear search | `Algorithms.linearSearch()` (`Inventory.findByCode()`) and `Algorithms.linearSearchAll()` (`searchPartial()`, low stock, customers with utang) |
| Merge sort (stable) | `Algorithms.mergeSort()` and `Algorithms.sortedCopy()`: restoring A-Z order after adding, every display order, low stock, reports |
| Stack (LIFO) | `UndoStack`, backed by `java.util.Stack<Transaction>`; used by option 10 |
| ArrayList | Master lists in `Inventory` and `CustomerRecords`, and each customer's history |

The master product and customer lists are always kept in alphabetical
order (required by binary search). Other orders are made on sorted copies.

## Object-oriented programming

| Principle | Where |
|----|----|
| Encapsulation | All fields are `private`. `Product`, `Customer`, `CashRegister` and `UndoStack` only change through methods that check the rules first (no negative stock or balance, no overselling). Lists are returned as copies. |
| Abstraction | `Transaction` is an abstract class; `Nameable` and `Algorithms.Condition` are interfaces that hide details from the search and sort code. |
| Inheritance | `Transaction` → `ProductTransaction` → `CashSaleTransaction` / `UtangTransaction`; `Transaction` → `PaymentTransaction`; `Exception` → `StoreException` → `DuplicateRecordException` / `InsufficientStockException`. |
| Polymorphism | `undo()`, `getReceipt()`, `getBalanceEffect()` and `getDetails()` behave differently for each transaction type; one `catch (StoreException e)` handles all its subclasses; `Product` and `Customer` are both searched as `Nameable`; `toString()` is overridden. |

## Validation and exception handling

- Blank, too long, or invalid names and codes are rejected. Names may contain
  spaces, apostrophes and hyphens. Duplicates are checked ignoring upper/lower
  case and extra spaces.
- Whole numbers (including menu selections): letters, decimals, negative values,
  leading zeros (e.g. `01`) and numbers too large for an `int` are rejected.
  A single `0` is accepted when the prompt's range allows it.
- Money uses `BigDecimal`, built from validated text. Negative amounts, NaN,
  Infinity, scientific notation (`1e3`), commas and more than two decimal
  places are rejected.
- Every transaction checks everything **before** changing any record, so a
  failed or cancelled operation never changes stock, balances, history,
  totals, or the undo stack.
- If the input ends (terminal closed), the program exits cleanly.

## Files

| File | Responsibility |
|----|----|
| `Main.java` | Startup, menus, workflows, sample data, printing |
| `InputHelper.java` | Reads and validates all input (uses the one Scanner from Main) |
| `Product.java`, `Customer.java` | Product and customer information |
| `Inventory.java`, `CustomerRecords.java` | Storage, searching and sorted lists; product edit/remove methods (not on the menu) |
| `Transaction.java`, `ProductTransaction.java`, `CashSaleTransaction.java`, `UtangTransaction.java`, `PaymentTransaction.java` | Transaction details, recording and undo |
| `TransactionType.java` | Enum: cash sale, utang, payment |
| `UndoStack.java` | The undo stack |
| `Algorithms.java` | Binary search, linear search, merge sort |
| `CashRegister.java` | Session cash-sales total |
| `Money.java`, `TextRules.java`, `Nameable.java` | Small helpers: money, name/code rules, the shared name interface |
| `StoreException.java`, `DuplicateRecordException.java`, `InsufficientStockException.java`, `InputCancelledException.java`, `EndOfInputException.java` | Exceptions |
