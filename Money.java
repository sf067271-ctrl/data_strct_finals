import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

// Utility class for parsing, checking and formatting money. Every amount is a BigDecimal
// with exactly 2 decimal places, because double and float cannot store centavos exactly.
// Demonstrates ENCAPSULATION (one place for money rules) and POLYMORPHISM (overloaded format()).
public final class Money {
    public static final int DECIMAL_PLACES = 2;
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal MAX_PRICE = new BigDecimal("100000.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("10000000.00");
    // A single transaction may not be larger than the biggest balance a customer may have.
    public static final BigDecimal MAX_TRANSACTION_TOTAL = MAX_BALANCE;

    // Digits, then optionally a point followed by 1 or 2 digits: 10, 10.5, 10.50, 0.25.
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]{1,2})?");
    // Same shape but with 3 or more decimals, so we can give a more helpful message.
    private static final Pattern TOO_MANY_DECIMALS = Pattern.compile("[0-9]+\\.[0-9]{3,}");

    private Money() {
        // Utility class: no objects needed.
    }

    // Turns validated text into a BigDecimal with 2 decimal places.
    public static BigDecimal parse(String text, String fieldName) throws ValidationException {
        // Step 1: a value is required.
        if (text.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        // Step 2: give a clear message for amounts like 10.999.
        if (TOO_MANY_DECIMALS.matcher(text).matches()) {
            throw new ValidationException(fieldName + " can have at most 2 decimal places, for example 10.50.");
        }
        // Step 3: the strict pattern rejects -5, +5, NaN, Infinity, 1e3, 1,000, PHP 5, 10., .5 and spaces.
        if (!MONEY_PATTERN.matcher(text).matches()) {
            throw new ValidationException(fieldName
                    + " must be a number like 10, 10.5 or 10.50 (digits only, no signs, commas or symbols).");
        }
        // Step 4: build the BigDecimal from the checked text (never from a double).
        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
        } catch (NumberFormatException badNumber) {
            throw new ValidationException(fieldName + " is not a valid number.");
        }
        // Step 5: force exactly 2 decimals. UNNECESSARY throws if rounding would be needed,
        // so hidden extra precision can never slip through.
        try {
            return amount.setScale(DECIMAL_PLACES, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException needsRounding) {
            throw new ValidationException(fieldName + " can have at most 2 decimal places.");
        }
    }

    // Parses a product price and checks that it is above 0.00 and at most PHP 100000.00.
    public static BigDecimal parsePrice(String text) throws ValidationException {
        BigDecimal price = parse(text, "Price");
        checkPrice(price);
        return price;
    }

    // Checks an already-parsed price.
    public static void checkPrice(BigDecimal price) throws ValidationException {
        if (!hasAtMostTwoDecimals(price)) {
            throw new ValidationException("Price can have at most 2 decimal places.");
        }
        if (!isPositive(price)) {
            throw new ValidationException("Price must be greater than PHP 0.00.");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("Price cannot be more than " + format(MAX_PRICE) + ".");
        }
    }

    // Parses a payment amount and checks it against the amount still owed on the record.
    public static BigDecimal parsePayment(String text, BigDecimal remaining)
            throws ValidationException, InvalidPaymentException {
        BigDecimal amount = parse(text, "Payment amount");
        checkPayment(amount, remaining);
        return amount;
    }

    // A payment must be above 0.00, have at most 2 decimals, and not exceed the remaining amount.
    public static void checkPayment(BigDecimal amount, BigDecimal remaining) throws InvalidPaymentException {
        if (!hasAtMostTwoDecimals(amount)) {
            throw new InvalidPaymentException("Payment amount can have at most 2 decimal places.");
        }
        if (!isPositive(amount)) {
            throw new InvalidPaymentException("Payment amount must be greater than PHP 0.00.");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new InvalidPaymentException("Payment of " + format(amount)
                    + " is more than the remaining amount of " + format(remaining) + ".");
        }
    }

    // Returns price x quantity. A 2-decimal price times a whole number keeps 2 decimals.
    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Returns true if the amount is greater than 0.00 (compareTo ignores scale, equals does not).
    public static boolean isPositive(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // Returns true if the amount is exactly 0.00.
    public static boolean isZero(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // Formats an amount as "PHP 1234.50". Locale.US makes the decimal separator a period.
    public static String format(BigDecimal amount) {
        return String.format(Locale.US, "PHP %.2f", amount);
    }

    // OVERLOADING: same name, extra parameter. Right-aligns the amount in a table column.
    public static String format(BigDecimal amount, int columnWidth) {
        return String.format(Locale.US, "%" + columnWidth + "s", format(amount));
    }

    private static boolean hasAtMostTwoDecimals(BigDecimal amount) {
        try {
            amount.setScale(DECIMAL_PLACES, RoundingMode.UNNECESSARY);
            return true;
        } catch (ArithmeticException needsRounding) {
            return false;
        }
    }
}
