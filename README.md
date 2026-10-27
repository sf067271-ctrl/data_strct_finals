# Sari-Sari Store Debt and Stock Management System

A terminal-based Java application (Java 8 or newer, standard library only)
for recording a sari-sari store's products, cash sales, utang (credit),
utang payments and restocks, with undo.

## Compile and run

**Easiest:** on Windows, double-click `run.bat`. On macOS or Linux, open a
terminal in this folder and type `sh run.sh`.

**By hand:** open a terminal in this folder and type:

```
javac -d bin -sourcepath src src/Main.java
java -cp bin Main
```

The first line compiles everything into the `bin` folder (it is ignored by
Git); the second starts the program.

**In VS Code:** choose File > Open Folder and select this folder. Then
open `src/Main.java` and click **Run** (or press F5 and pick
*Run Sari-Sari Store System*). Type your answers in the **TERMINAL** panel.

## Project structure

All Java code is in `src`. `Main.java` is the starting point; everything
else is grouped by what it does, one folder (Java package) per kind of file.

```
src/
├── Main.java                         Start here: the menu, the 12 options, sample data, printing
└── sarisari/
    ├── models/                       The things the store keeps track of
    │   ├── Product.java              Code, name, price, stock, total sold
    │   ├── Customer.java             Name, utang balance, history
    │   ├── CashRegister.java         Cash sales total for this session
    │   └── Nameable.java             "Has a name" interface used by search and sort
    ├── datastructures/               Where the records are stored
    │   ├── Inventory.java            ArrayList of all products, kept A-Z
    │   ├── CustomerRecords.java      ArrayList of all customers, kept A-Z
    │   └── UndoStack.java            Stack (LIFO) of transactions that can be undone
    ├── algorithms/
    │   └── Algorithms.java           Binary search, linear search, merge sort
    ├── transactions/                 Everything that can be recorded and undone
    │   ├── Transaction.java          Base class: record, undo, number, time
    │   ├── TransactionType.java      Cash sale, Utang, Payment, Restock
    │   ├── ProductTransaction.java   Shared parent of cash sale and utang
    │   ├── CashSaleTransaction.java
    │   ├── UtangTransaction.java
    │   ├── PaymentTransaction.java
    │   └── RestockTransaction.java
    ├── validators/                   Checks for everything the user types
    │   ├── InputHelper.java          Reads input: numbers, money, yes/no, cancel with X
    │   └── TextRules.java            Rules for names and product codes
    ├── exceptions/                   The errors the program can show
    │   ├── StoreException.java       Base class for business-rule errors
    │   ├── DuplicateRecordException.java
    │   ├── InsufficientStockException.java
    │   ├── InputCancelledException.java
    │   └── EndOfInputException.java
    └── utils/
        └── Money.java                Peso amounts: format "PHP 1,250.50" and checks
```

## Where to look

| When something happens with... | Look in |
|----|----|
| A menu option (what it asks, what it prints) | `src/Main.java`: one method per option, e.g. `recordCashSale()`, `restockProduct()` |
| A typed number, amount, name or code being accepted or rejected | `validators/InputHelper.java` (numbers, money, Y/N, X) and `validators/TextRules.java` (names, codes) |
| Stock, total sold or price | `models/Product.java` |
| A customer's balance or history | `models/Customer.java`, plus `transactions/UtangTransaction.java` and `PaymentTransaction.java` |
| Cash sales total | `models/CashRegister.java` and `transactions/CashSaleTransaction.java` |
| Recording or undoing a sale, utang, payment or restock | `transactions/` (each type's `applyChanges()` and `reverseChanges()`) and `datastructures/UndoStack.java` |
| Search results, sort order, low stock, best sellers | `algorithms/Algorithms.java`, and `datastructures/Inventory.java` / `CustomerRecords.java` for which algorithm each list uses |
| An error message's wording | `exceptions/`, `validators/`, or the class that throws it |
| Money formatting | `utils/Money.java` |
| The sample products and customers | `Main.loadSampleData()` in `src/Main.java` |

## Menu options

| No. | Option | What it does |
|----|----|----|
| 1 | Add product | Adds a product with a unique code and name, a positive price and an opening stock (0 allowed). |
| 2 | Search product | Exact name (binary search), then exact code (linear search), then partial names (linear search). |
| 3 | Record cash sale | Sells one product for cash and prints a receipt. |
| 4 | Record utang | Sells one product on credit. A new customer can be created on the spot. |
| 5 | Record utang payment | Lowers a customer's balance. Overpayments are rejected. |
| 6 | View Customers | Sub-menu to view all customers alphabetically or search customers with outstanding debts by exact or partial name. Shows names, balances and full histories (ACTIVE or UNDONE entries). |
| 7 | Display products sorted | By name, price, stock, or best sellers, using merge sort. |
| 8 | Low-stock alert | Products with 5 or fewer pieces (including 0), lowest stock first. |
| 9 | Reports | Session cash sales, total outstanding utang, top 3 best sellers, customers with utang. |
| 10 | Undo last transaction | Reverses the most recent cash sale, utang, payment or restock (stack, LIFO). |
| 11 | Restock product | Adds the quantity received to an existing product's stock after confirmation. |
| 12 | Exit | Ends the program after a confirmation. |

**Restocking:** choose option `11`, select a product by code or name, and enter
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
| Binary search | `algorithms/Algorithms.java`: `binarySearch()`, used by `Inventory.findByName()` and `CustomerRecords.findByName()` |
| Linear search | `algorithms/Algorithms.java`: `linearSearch()` (`Inventory.findByCode()`) and `linearSearchAll()` (`searchPartial()`, low stock, customers with utang) |
| Merge sort (stable) | `algorithms/Algorithms.java`: `mergeSort()` and `sortedCopy()`: restoring A-Z order after adding, every display order, low stock, reports |
| Stack (LIFO) | `datastructures/UndoStack.java`, backed by `java.util.Stack<Transaction>`; used by option 10 |
| ArrayList | `datastructures/Inventory.java` and `datastructures/CustomerRecords.java` (master lists), and each customer's history in `models/Customer.java` |

The master product and customer lists are always kept in alphabetical
order (required by binary search). Other orders are made on sorted copies.

## Object-oriented programming

| Principle | Where |
|----|----|
| Encapsulation | All fields are `private`. `Product`, `Customer`, `CashRegister` and `UndoStack` only change through methods that check the rules first (no negative stock or balance, no overselling). Lists are returned as copies. |
| Abstraction | `Transaction` is an abstract class; `Nameable` and `Algorithms.Condition` are interfaces that hide details from the search and sort code. |
| Inheritance | `Transaction` → `ProductTransaction` → `CashSaleTransaction` / `UtangTransaction`; `Transaction` → `PaymentTransaction` / `RestockTransaction`; `Exception` → `StoreException` → `DuplicateRecordException` / `InsufficientStockException`. |
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
