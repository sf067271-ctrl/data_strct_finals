import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

// Utility class with static checks for names, codes, whole numbers, and dates.
// Demonstrates ENCAPSULATION: every input rule lives in one place, so all classes apply the same rules.
public final class Validator {
    public static final String CANCEL_COMMAND = "/cancel";
    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_CODE_LENGTH = 12;
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 10_000;
    public static final int MIN_STOCK = 0;
    public static final int MAX_STOCK = 1_000_000;
    public static final LocalDate EARLIEST_UTANG_DATE = LocalDate.of(2000, 1, 1);

    private static final Pattern DIGITS_ONLY = Pattern.compile("[0-9]+");
    private static final Pattern PRODUCT_CODE = Pattern.compile("[A-Za-z0-9-]+");
    private static final Pattern DATE_SHAPE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Pattern REPEATED_SPACES = Pattern.compile(" {2,}");
    private static final String PRODUCT_NAME_SYMBOLS = "' - . , & ( ) %";
    private static final String CUSTOMER_NAME_SYMBOLS = "' - .";
    // "uuuu" + STRICT makes impossible dates such as 2026-02-30 fail instead of being adjusted.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private Validator() {
        // Utility class: no objects needed.
    }

    // Returns true if the text is the cancel command, ignoring letter case.
    public static boolean isCancelCommand(String text) {
        return text.trim().equalsIgnoreCase(CANCEL_COMMAND);
    }

    // Rejects control characters (tab, escape, etc.) and other non-printable characters.
    public static void checkPrintable(String text) throws ValidationException {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int type = Character.getType(character);
            if (Character.isISOControl(character) || type == Character.FORMAT
                    || type == Character.PRIVATE_USE || type == Character.SURROGATE
                    || type == Character.UNASSIGNED) {
                throw new ValidationException("Input contains a control or non-printable character.");
            }
        }
    }

    // Trims the ends and turns runs of spaces into one space: "  Lucky   Me " -> "Lucky Me".
    public static String collapseSpaces(String text) {
        return REPEATED_SPACES.matcher(text.trim()).replaceAll(" ");
    }

    // Builds the key used to compare names: cleaned and lowercase, so "EGG" and " egg " match.
    public static String toNameKey(String name) {
        return collapseSpaces(name).toLowerCase(Locale.ROOT);
    }

    // Cleans and checks a product name. Letters, digits, spaces and ' - . , & ( ) % are allowed.
    public static String validateProductName(String rawName) throws ValidationException {
        return validateName(rawName, "Product name", PRODUCT_NAME_SYMBOLS, true);
    }

    // Cleans and checks a customer name. Letters, spaces and ' - . are allowed (no digits).
    public static String validateCustomerName(String rawName) throws ValidationException {
        return validateName(rawName, "Customer name", CUSTOMER_NAME_SYMBOLS, false);
    }

    // Checks a product code (1-12 letters, digits or '-') and returns it in uppercase.
    public static String validateProductCode(String rawCode) throws ValidationException {
        String code = rawCode.trim();
        if (code.isEmpty()) {
            throw new ValidationException("Product code is required.");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new ValidationException("Product code can have at most " + MAX_CODE_LENGTH + " characters.");
        }
        if (!PRODUCT_CODE.matcher(code).matches() || !containsLetterOrDigit(code)) {
            throw new ValidationException("Product code may contain only letters, digits and '-' (no spaces).");
        }
        return code.toUpperCase(Locale.ROOT);
    }

    // Parses text that must contain only digits and be inside [min, max].
    public static int parseWholeNumber(String text, int min, int max, String fieldName)
            throws ValidationException {
        String rangeMessage = fieldName + " must be a whole number from " + min + " to " + max + ".";
        if (!DIGITS_ONLY.matcher(text).matches()) {
            // Rejects blanks, signs (+3, -1), decimals (2.5), 1e3, 0x10, words and commas.
            throw new ValidationException(rangeMessage);
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException tooLarge) {
            // Only digits reach this point, so the number is too large for an int.
            throw new ValidationException(fieldName + " is too large. " + rangeMessage);
        }
        if (value < min || value > max) {
            throw new ValidationException(rangeMessage);
        }
        return value;
    }

    // Parses a transaction quantity (1 to 10000).
    public static int parseQuantity(String text) throws ValidationException {
        return parseWholeNumber(text, MIN_QUANTITY, MAX_QUANTITY, "Quantity");
    }

    // Parses an opening or edited stock amount (0 to 1000000).
    public static int parseStock(String text) throws ValidationException {
        return parseWholeNumber(text, MIN_STOCK, MAX_STOCK, "Stock");
    }

    // Checks an already-parsed transaction quantity.
    public static void checkQuantity(int quantity) throws ValidationException {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new ValidationException("Quantity must be a whole number from "
                    + MIN_QUANTITY + " to " + MAX_QUANTITY + ".");
        }
    }

    // Checks an already-parsed stock amount.
    public static void checkStock(int stock) throws ValidationException {
        if (stock < MIN_STOCK || stock > MAX_STOCK) {
            throw new ValidationException("Stock must be a whole number from "
                    + MIN_STOCK + " to " + MAX_STOCK + ".");
        }
    }

    // Parses a utang date written exactly as yyyy-MM-dd, then checks its range.
    public static LocalDate parseUtangDate(String text, LocalDate today) throws ValidationException {
        String formatMessage = "Date must be written as yyyy-MM-dd, for example " + formatDate(today) + ".";
        if (!DATE_SHAPE.matcher(text).matches()) {
            throw new ValidationException(formatMessage);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException impossibleDate) {
            throw new ValidationException(text + " is not a real calendar date. " + formatMessage);
        }
        checkUtangDate(date, today);
        return date;
    }

    // Utang dates may be in the past (old notebook entries) but not in the future or before 2000.
    public static void checkUtangDate(LocalDate date, LocalDate today) throws ValidationException {
        if (date.isAfter(today)) {
            throw new ValidationException("Utang date cannot be in the future (today is " + formatDate(today) + ").");
        }
        if (date.isBefore(EARLIEST_UTANG_DATE)) {
            throw new ValidationException("Utang date cannot be earlier than " + formatDate(EARLIEST_UTANG_DATE) + ".");
        }
    }

    // Formats a date as yyyy-MM-dd.
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    // Shared rules for product and customer names.
    private static String validateName(String rawName, String fieldName, String allowedSymbols,
            boolean digitsAllowed) throws ValidationException {
        String name = collapseSpaces(rawName);
        if (name.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(fieldName + " can have at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!containsLetterOrDigit(name)) {
            throw new ValidationException(fieldName + " must contain at least one letter or digit.");
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            boolean allowed = Character.isLetter(character) || character == ' '
                    || (digitsAllowed && isAsciiDigit(character))
                    || allowedSymbols.indexOf(character) >= 0;
            if (!allowed) {
                throw new ValidationException(fieldName + " contains an invalid character: '" + character
                        + "'. Only letters, " + (digitsAllowed ? "digits, " : "") + "spaces and the symbols "
                        + allowedSymbols + " are allowed.");
            }
        }
        return name;
    }

    private static boolean containsLetterOrDigit(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isLetter(character) || isAsciiDigit(character)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
