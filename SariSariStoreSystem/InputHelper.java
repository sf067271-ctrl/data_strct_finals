import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Reads and VALIDATES everything the operator types.
 *
 * Rules followed by every method:
 *   - Always uses scanner.nextLine(), then converts the text, so a wrong
 *     entry never leaves leftover input in the Scanner.
 *   - Invalid input shows a clear message and asks again (a loop, not a crash).
 *   - Typing X (upper or lower case) cancels the current operation by
 *     throwing InputCancelledException. Main catches it and returns to the menu.
 *   - If the input ends (terminal closed), EndOfInputException is thrown and
 *     Main exits politely.
 *
 * The program uses ONE Scanner. It is created in Main and passed in here.
 */
public class InputHelper {

    /** Type this at any transaction prompt to cancel. */
    public static final String CANCEL_WORD = "X";

    /** At most 7 digits before the decimal point (up to 9,999,999.99). */
    private static final int MAX_MONEY_DIGITS = 7;

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("A Scanner is required.");
        }
        this.scanner = scanner;
    }

    // =====================================================================
    //  Basic reading
    // =====================================================================

    /**
     * Reads one line, trimmed.
     *
     * @throws EndOfInputException if there is no more input
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();
        }
        return scanner.nextLine().trim();
    }

    /** Reads a line and throws InputCancelledException if it is the cancel word. */
    private String readLineOrCancel(String prompt) {
        String text = readLine(prompt);
        if (text.equalsIgnoreCase(CANCEL_WORD)) {
            throw new InputCancelledException();
        }
        return text;
    }

    // =====================================================================
    //  Text
    // =====================================================================

    /**
     * Reads a product name, customer name or search keyword.
     * Spaces, apostrophes and hyphens are allowed (e.g. "D'Angelo Cruz-Reyes").
     * Extra spaces are removed.
     */
    public String readName(String prompt) {
        while (true) {
            String text = readLineOrCancel(prompt);
            String problem = TextRules.checkName(text);
            if (problem == null) {
                return TextRules.clean(text);
            }
            printError(problem);
        }
    }

    /** Reads a product code (letters, numbers, hyphens; saved in upper case). */
    public String readCode(String prompt) {
        while (true) {
            String text = readLineOrCancel(prompt);
            String problem = TextRules.checkCode(text);
            if (problem == null) {
                return TextRules.clean(text).toUpperCase();
            }
            printError(problem);
        }
    }

    // =====================================================================
    //  Whole numbers
    // =====================================================================

    /**
     * Reads a whole number from min to max. Typing X cancels.
     * Rejects letters, decimals, negative numbers and numbers too large for an int.
     */
    public int readWholeNumber(String prompt, int min, int max) {
        while (true) {
            String text = readLineOrCancel(prompt);
            Integer value = parseWholeNumber(text, min, max);
            if (value != null) {
                return value;
            }
        }
    }

    /** Reads a menu choice from min to max. X is NOT treated as cancel here. */
    public int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            Integer value = parseWholeNumber(text, min, max);
            if (value != null) {
                return value;
            }
        }
    }

    /**
     * Converts text to a whole number and checks the range.
     * Prints the reason and returns null if the text is not acceptable.
     */
    private Integer parseWholeNumber(String text, int min, int max) {
        if (text.isEmpty()) {
            printError("Please enter a number.");
            return null;
        }
        if (text.startsWith("-")) {
            printError("Negative numbers are not allowed.");
            return null;
        }
        if (text.contains(".")) {
            printError("Please enter a whole number (no decimals).");
            return null;
        }
        if (!text.matches("\\d+")) {
            printError("\"" + text + "\" is not a number. Please type digits only.");
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {   // happens when the number is too large for an int
            printError("That number is too large.");
            return null;
        }
        if (value < min || value > max) {
            printError("Please enter a number from " + min + " to " + max + ".");
            return null;
        }
        return value;
    }

    // =====================================================================
    //  Money
    // =====================================================================

    /**
     * Reads a peso amount from min to max, e.g. "25", "25.5" or "25.50".
     * Typing X cancels.
     *
     * Rejected: blank, negative, letters, NaN, Infinity, scientific notation
     * (1e3), commas, more than two decimal places, and amounts out of range.
     * The BigDecimal is built from the validated text, never from a double.
     */
    public BigDecimal readMoney(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            String text = readLineOrCancel(prompt);

            if (text.isEmpty()) {
                printError("Please enter an amount.");
                continue;
            }
            if (text.startsWith("-")) {
                printError("The amount cannot be negative.");
                continue;
            }
            if (text.contains(",")) {
                printError("Please type the amount without commas, e.g. 1500.50");
                continue;
            }
            // Only digits, with an optional decimal part: this rejects NaN,
            // Infinity, 1e3, letters and symbols in one check.
            if (!text.matches("\\d+(\\.\\d+)?")) {
                printError("\"" + text + "\" is not a valid amount. Type a plain amount such as 9 or 9.50");
                continue;
            }
            String[] parts = text.split("\\.");
            if (parts.length == 2 && parts[1].length() > 2) {
                printError("Please use at most two decimal places (centavos), e.g. 9.75");
                continue;
            }
            if (parts[0].replaceFirst("^0+(?=\\d)", "").length() > MAX_MONEY_DIGITS) {
                printError("That amount is too large.");
                continue;
            }

            BigDecimal amount;
            try {
                amount = Money.of(text);
            } catch (NumberFormatException | ArithmeticException e) {
                printError("\"" + text + "\" is not a valid amount.");
                continue;
            }
            if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
                printError("Please enter an amount from " + Money.format(min) + " to " + Money.format(max) + ".");
                continue;
            }
            return amount;
        }
    }

    // =====================================================================
    //  Yes / No and pause
    // =====================================================================

    /**
     * Asks a yes/no question. Returns true for Y, false for N.
     * Typing X cancels the whole operation.
     */
    public boolean confirm(String question) {
        while (true) {
            String text = readLineOrCancel(question + " (Y/N): ");
            if (text.equalsIgnoreCase("Y") || text.equalsIgnoreCase("YES")) {
                return true;
            }
            if (text.equalsIgnoreCase("N") || text.equalsIgnoreCase("NO")) {
                return false;
            }
            printError("Please type Y for yes or N for no.");
        }
    }

    /** Waits for Enter, so the operator can read the result before the menu appears again. */
    public void pause() {
        readLine("\nPress Enter to continue...");
    }

    private void printError(String message) {
        System.out.println("  [ERROR] " + message);
    }
}
