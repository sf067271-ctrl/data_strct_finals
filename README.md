# Sari-Sari Store Debt and Stock Management System

A terminal-based Java program for a small neighborhood store (sari-sari store). It tracks products and stock,
cash sales, utang (items taken on credit), utang payments, and lets the operator undo the last transaction.
It is a Data Structures and Algorithms project that demonstrates **binary search, linear search, merge sort and
a stack**, plus the four pillars of OOP.

## Compile and run

Requires Java 17 or newer. There is no build tool and no external library.

```
javac -Xlint:all *.java
java Main
```

All data is kept **in memory only** (`ArrayList`s). Sample data loads at startup, and every change is lost
when the program exits. There is no login, database or file saving.

## Cancel command

Inside any option, type **`/cancel`** (any letter case) at a prompt to stop. The program prints
"Operation cancelled. No changes were made." and returns to the menu. Every prompt that can be cancelled
says `(or /cancel)`.

## Main menu

| No. | Option | What it does |
|---|---|---|
| 1 | Add product | Code, name, price, opening stock. Rejects duplicate codes/names (ignoring case and extra spaces). |
| 2 | Search product | Type a code or a name. Shows whether it was found by code (linear search), exact name (binary search) or partial name (linear search). |
| 3 | Add transaction | Sells one product, paid in cash or as utang (see below). |
| 4 | View utang history | Sub-menu: 1 Show all, 2 Show unpaid, 3 Show paid, 4 Back. |
| 5 | Record utang payment | Pays one specific unpaid utang record (partial payments allowed). |
| 6 | Display products sorted | Sub-menu: 1 Name A-Z, 2 Price low-high, 3 Stock low-high, 4 Best sellers, 5 Back. |
| 7 | Low-stock alert | Products with stock of 5 or less, lowest first; zero stock is marked OUT OF STOCK. |
| 8 | Reports | Cash sales, utang payments collected (kept separate), total outstanding utang, unpaid/paid counts, top 3 best sellers, customers with a balance (highest first). |
| 9 | Undo last transaction | Shows the newest transaction, asks for confirmation, then reverses it. |
| 10 | Exit | Ends the program. |

### Add transaction flow (option 3)

Nothing is changed until the final confirmation.

1. `Enter product code or name (or /cancel):` lookup order is exact code, then exact name, then partial
   name. Several partial matches give a numbered list; the operator must pick one by number.
2. Quantity (1 to 10000, and not more than the stock).
3. Payment method: `1. Cash` or `2. Utang`.
4. Utang only: customer name (exact name, then partial matches as a numbered list). If the customer does
   not exist, the program asks whether to create them. Then the date:
   `Date of this utang (yyyy-MM-dd). Press Enter to use today's date (...)`. Past dates are allowed;
   future dates, dates before 2000-01-01 and impossible dates (2026-02-30) are rejected.
   Cash sales and payments always use today's date.
5. Transaction details are shown (number, date, product, quantity, unit price, total, payment method,
   customer, stock after sale), then `Save this transaction? (y/n)`.
6. On save, a receipt shows the remaining stock (and, for utang, the customer's new balance).

### View utang history (option 4)

The table lists No., Date, Customer, Product, Qty, Total, Paid, Remaining and Status, with each record's
payments on indented lines. Records are ordered by customer name A-Z, then oldest date first. Undone
records (and undone payments) are marked `[UNDONE]`, appear only under *Show all*, and are left out of
every total. A summary at the end shows the number of records, total utang, total paid and total remaining.

## Sample data

| Code | Name | Price | Stock |
|---|---|---|---|
| P001 | Milo Sachet | PHP 9.00 | 30 |
| P002 | Egg | PHP 10.00 | 4 |
| P003 | Lucky Me Pancit Canton | PHP 16.00 | 25 |
| P004 | Coke Sakto | PHP 15.00 | 12 |
| P005 | Datu Puti Vinegar | PHP 22.00 | 8 |
| P006 | Safeguard Shampoo Sachet | PHP 7.00 | 0 |
| P007 | Safeguard Soap | PHP 38.00 | 3 |

Customers: Aling Nena, Mang Tonyo and Ate Lorna, all with no utang. Kuya Ben is *not* preloaded, so
creating a new customer can be tested. The data is loaded by `Main.loadSampleData()`.

## Where each item is implemented

### Data structures and algorithms

| Item | Where |
|---|---|
| Binary search | `Algorithms.binarySearch()`, used by `NamedRecordList.findByName()` (inherited by `Inventory` and `CustomerRecords`). The master lists stay sorted A-Z, and sorting and searching share one rule: `Entity.compareNameKeys()`. |
| Linear search | `NamedRecordList.searchPartial()` (partial names) and `Inventory.findByCode()` (codes, because the list is sorted by name, not code). |
| Merge sort (stable) | `Algorithms.mergeSort()` / `merge()`. Used by `NamedRecordList.addRecord()` / `restoreNameOrder()` (A-Z order after adding or renaming), `Inventory.getSortedByName/Price/Stock()`, `getBestSellers()`, `getLowStockProducts()`, `CustomerRecords.getCustomersWithBalance()`, `CustomerRecords.getUtangHistory()` (two sorts: date, then customer name), and `Customer.getUnpaidRecords()`. Displays always sort a copy. |
| Stack | `UndoStack` wraps `java.util.Stack<Transaction>` (`push`, `pop`, `peek`, `isEmpty`, `size`). Used by `Main.confirmAndSave()` (push) and `Main.undoLastTransaction()` (peek, then pop after a successful reverse). |

### OOP principles

| Principle | Where |
|---|---|
| Encapsulation | All fields are `private`. `getAll()`, `getUtangRecords()` and `getPayments()` return read-only lists. Stock and totals change only through checked methods (`Product.recordSale()`/`restoreSale()`, `SalesLedger.addCashSale()` etc. are package-private and called only after validation). `Product.setName()` is package-private and called only by `Inventory.renameProduct()`. The `undone` flag in `Transaction` changes only in `reverse()`. Quantity, unit price and total are `final` in `SaleTransaction`; the payment amount is `final` in `PaymentTransaction`. A customer's balance is computed in `Customer.getBalance()`, never stored. |
| Abstraction | Abstract class `Entity` (`getSummaryLine()`), abstract class `Transaction` (`validateExecution()`, `applyExecution()`, `validateReversal()`, `applyReversal()`, `describe()`, `describeCurrentState()`), interface `Reversible` (`execute()`, `reverse()`, `isUndone()`), abstract generic `NamedRecordList<T>`. `Inventory`, `CustomerRecords` and `UndoStack` hide their collections and algorithms from `Main`. |
| Inheritance | `Entity` <- `Product`, `Customer`; `NamedRecordList<T>` <- `Inventory`, `CustomerRecords`; `Transaction` <- `SaleTransaction` <- `CashSaleTransaction`, `UtangTransaction`, and `Transaction` <- `PaymentTransaction`; `StoreException` <- 8 exception classes. Constructors call `super(...)`; shared logic lives in the parent (`describeHeader()`, `validateSale()`, `applySale()`, ...). |
| Polymorphism | Overriding: every transaction subclass overrides `describe()`, `describeCurrentState()` and the four validate/apply methods; `Product` and `Customer` override `getSummaryLine()` and `toString()`. Runtime dispatch: `Main.confirmAndSave()` calls `transaction.execute()` and `Main.undoLastTransaction()` calls `transaction.reverse()` through the `Transaction` type, with no `if`/`switch` on the type. Generics: `Algorithms.mergeSort(List<T>, Comparator<T>)`, `NamedRecordList<T extends Entity>`. Overloading: `ConsoleInput.readInt(prompt, min, max)` / `readInt(prompt, max)` and `Money.format(amount)` / `format(amount, width)`. |

The template method is in `Transaction.execute()` and `Transaction.reverse()` (both `final`): validate
everything first, then apply. A failed check throws before anything changes, so stock, balances, history,
totals and the undo stack are never partly updated.

### Exceptions

```
StoreException (abstract, checked)
├── ValidationException          bad input: blank, wrong format, out of range
├── DuplicateRecordException     duplicate product code/name or customer name
├── RecordNotFoundException      no product/customer found
├── InsufficientStockException   quantity is more than the stock
├── InvalidPaymentException      payment zero, too many decimals, above the remaining amount, or nothing to pay
├── UndoException                nothing to undo, or undo would make data inconsistent
├── InputCancelledException      operator typed /cancel
└── EndOfInputException          no more input
```

* `ConsoleInput` catches `ValidationException` / `InvalidPaymentException` at each prompt, prints the
  message and asks again. `NumberFormatException`, `ArithmeticException` and `DateTimeParseException` are
  caught where text is parsed (`Validator`, `Money`) and turned into `ValidationException`s.
* `Main.runOneMenuAction()` wraps every menu action: `InputCancelledException` (cancelled message),
  `EndOfInputException` (goodbye and clean exit), `StoreException` (error message), and `RuntimeException`
  ("Unexpected error: ..."; the program keeps running). The single `Scanner` is closed in a `finally` block
  in `Main.main()`.

## Test inputs

The program is tested by feeding scripted input, for example `java Main < test-inputs/required-sequence.txt`.

| File | What it covers |
|---|---|
| `test-inputs/required-sequence.txt` | Cash sale, utang for Aling Nena, two payments (PAID), then undo of everything back to the starting state. |
| `test-inputs/product-and-menu-validation.txt` | Invalid menu choices, duplicate/invalid codes and names, bad prices and stock, searches, sorting, low stock. |
| `test-inputs/transaction-validation.txt` | Product selection by code/name/partial, quantities, payment methods, new and declined customers, dates, payments, history filters, `/cancel` at every prompt type, declined undo. |
| `test-inputs/end-of-input-mid-transaction.txt` | Input ends in the middle of a transaction; the program exits cleanly. |
