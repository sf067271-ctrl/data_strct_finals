package sarisari.validators;

/**
 * The rules for names and product codes, kept in ONE place so the input
 * screens and the Product/Customer classes always agree.
 */
public final class TextRules {

    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_CODE_LENGTH = 10;

    /**
     * Letters, digits, spaces, apostrophes ( ' and ’ ), hyphens, periods,
     * commas, ampersands, parentheses and slashes.
     * Examples: "Aling Nena", "Mang Jose-Ramos", "D'Angelo", "Coke 1.5L (Mismo)".
     */
    private static final String NAME_PATTERN = "[\\p{L}\\p{N} '’\\-.,&()/]+";

    /** Letters, digits and hyphens only, e.g. "P001" or "SKU-12". */
    private static final String CODE_PATTERN = "[A-Za-z0-9\\-]+";

    private TextRules() {
        // Utility class: no objects needed.
    }

    /**
     * Removes spaces at the start and end and turns repeated inner spaces
     * into one, so "  Aling   Nena " becomes "Aling Nena".
     * This is how "differs only by surrounding spaces" is ignored.
     */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * Checks a product or customer name.
     * @return null if the name is valid, otherwise a message explaining the problem
     */
    public static String checkName(String name) {
        String cleaned = clean(name);
        if (cleaned.isEmpty()) {
            return "The name cannot be blank.";
        }
        if (cleaned.length() > MAX_NAME_LENGTH) {
            return "The name can have at most " + MAX_NAME_LENGTH + " characters.";
        }
        if (!cleaned.matches(NAME_PATTERN)) {
            return "The name may only contain letters, numbers, spaces, apostrophes, hyphens, "
                    + "and . , & ( ) /";
        }
        if (!cleaned.matches(".*[\\p{L}\\p{N}].*")) {
            return "The name must contain at least one letter or number.";
        }
        if (cleaned.equalsIgnoreCase(InputHelper.CANCEL_WORD)) {
            return "\"" + InputHelper.CANCEL_WORD + "\" is reserved for cancelling and cannot be used as a name.";
        }
        return null;
    }

    /**
     * Checks a product code.
     * @return null if the code is valid, otherwise a message explaining the problem
     */
    public static String checkCode(String code) {
        String cleaned = clean(code);
        if (cleaned.isEmpty()) {
            return "The product code cannot be blank.";
        }
        if (cleaned.length() > MAX_CODE_LENGTH) {
            return "The product code can have at most " + MAX_CODE_LENGTH + " characters.";
        }
        if (!cleaned.matches(CODE_PATTERN)) {
            return "The product code may only contain letters, numbers and hyphens (no spaces).";
        }
        if (cleaned.equalsIgnoreCase(InputHelper.CANCEL_WORD)) {
            return "\"" + InputHelper.CANCEL_WORD + "\" is reserved for cancelling and cannot be used as a code.";
        }
        return null;
    }
}
