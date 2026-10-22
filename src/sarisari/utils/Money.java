package sarisari.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small helper for money.
 *
 * Why BigDecimal and not double?
 *   double cannot store some decimals exactly (0.1 + 0.2 = 0.30000000000000004),
 *   so balances could slowly become wrong. BigDecimal stores exact pesos and
 *   centavos, which is what a store's records need.
 */
public final class Money {

    /** PHP 0.00 */
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    /** The smallest amount allowed for a price or payment: PHP 0.01 */
    public static final BigDecimal ONE_CENTAVO = new BigDecimal("0.01");

    /** The largest amount accepted for a price or payment: PHP 1,000,000.00 */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    private Money() {
        // Utility class: no objects needed.
    }

    /** Formats an amount, e.g. 1250.5 -> "PHP 1,250.50". */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "PHP 0.00";
        }
        return "PHP " + String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Creates a money value from text that was ALREADY validated,
     * e.g. "9" -> 9.00 and "9.5" -> 9.50.
     *
     * @throws NumberFormatException if the text is not a plain number
     * @throws ArithmeticException   if it has more than two decimal places
     */
    public static BigDecimal of(String text) {
        return new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** True if the amount is greater than zero. */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** True if the amount has at most two decimal places (centavos). */
    public static boolean hasAtMostTwoDecimals(BigDecimal amount) {
        return amount != null && amount.stripTrailingZeros().scale() <= 2;
    }
}
