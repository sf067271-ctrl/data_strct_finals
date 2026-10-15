import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

// The ONLY class that reads from the Scanner. Every read method prompts, checks the text,
// and asks again until the input is valid, the operator types /cancel, or the input ends.
// Demonstrates ENCAPSULATION (all input handling in one place) and POLYMORPHISM (overloaded readInt()).
public class ConsoleInput {
    private static final String CANCEL_HINT = " (or " + Validator.CANCEL_COMMAND + "): ";

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    // Reads a main-menu or sub-menu choice from 1 to maxOption. Menus are left with their own
    // "Back"/"Exit" option, so /cancel is not offered here.
    public int readMenuChoice(int maxOption) throws EndOfInputException {
        String prompt = "Choose an option (1-" + maxOption + "): ";
        while (true) {
            String line = readLine(prompt);
            try {
                Validator.checkPrintable(line);
                return Validator.parseWholeNumber(line.trim(), 1, maxOption, "Menu choice");
            } catch (ValidationException invalidChoice) {
                ConsoleFormat.printError(invalidChoice.getMessage());
            }
        }
    }

    // Reads a whole number from min to max, for example a number picked from a list.
    public int readInt(String prompt, int min, int max) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Validator.parseWholeNumber(text, min, max, "Your choice");
            } catch (ValidationException invalidNumber) {
                ConsoleFormat.printError(invalidNumber.getMessage());
            }
        }
    }

    // OVERLOADING: the same method with no minimum. Lists always start at 1, so this
    // shorter version reads a number from 1 to max.
    public int readInt(String prompt, int max) throws InputCancelledException, EndOfInputException {
        return readInt(prompt, 1, max);
    }

    // Reads a transaction quantity (1 to 10000).
    public int readQuantity(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Validator.parseQuantity(text);
            } catch (ValidationException invalidQuantity) {
                ConsoleFormat.printError(invalidQuantity.getMessage());
            }
        }
    }

    // Reads an opening stock amount (0 to 1000000).
    public int readStock(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Validator.parseStock(text);
            } catch (ValidationException invalidStock) {
                ConsoleFormat.printError(invalidStock.getMessage());
            }
        }
    }

    // Reads a price above 0.00 and at most PHP 100000.00.
    public BigDecimal readPrice(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Money.parsePrice(text);
            } catch (ValidationException invalidPrice) {
                ConsoleFormat.printError(invalidPrice.getMessage());
            }
        }
    }

    // Reads a payment amount above 0.00 and not above the remaining amount.
    public BigDecimal readPaymentAmount(String prompt, BigDecimal remaining)
            throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Money.parsePayment(text, remaining);
            } catch (ValidationException | InvalidPaymentException invalidPayment) {
                ConsoleFormat.printError(invalidPayment.getMessage());
            }
        }
    }

    // Reads a product code and returns it in uppercase.
    public String readProductCode(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Validator.validateProductCode(text);
            } catch (ValidationException invalidCode) {
                ConsoleFormat.printError(invalidCode.getMessage());
            }
        }
    }

    // Reads and cleans a product name.
    public String readProductName(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readRequiredText(prompt + CANCEL_HINT);
            try {
                return Validator.validateProductName(text);
            } catch (ValidationException invalidName) {
                ConsoleFormat.printError(invalidName.getMessage());
            }
        }
    }

    // Reads search text (a code or a full or partial name), with spaces cleaned up.
    public String readSearchText(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = Validator.collapseSpaces(readRequiredText(prompt + CANCEL_HINT));
            if (text.length() <= Validator.MAX_NAME_LENGTH) {
                return text;
            }
            ConsoleFormat.printError("Search text can have at most " + Validator.MAX_NAME_LENGTH + " characters.");
        }
    }

    // Reads y/yes or n/no in any letter case. Anything else is rejected and asked again.
    public boolean readYesNo(String question) throws InputCancelledException, EndOfInputException {
        String prompt = question + " (y/n, or " + Validator.CANCEL_COMMAND + "): ";
        while (true) {
            String answer = readRequiredText(prompt).toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            ConsoleFormat.printError("Please answer y, yes, n or no.");
        }
    }

    // Reads the date of an utang. Pressing Enter (empty input) means today.
    public LocalDate readUtangDate(LocalDate today) throws InputCancelledException, EndOfInputException {
        String prompt = "Date of this utang (yyyy-MM-dd). Press Enter to use today's date ("
                + Validator.formatDate(today) + ")" + CANCEL_HINT;
        while (true) {
            String text = readCancellableLine(prompt);
            if (text.isEmpty()) {
                return today;
            }
            try {
                return Validator.parseUtangDate(text, today);
            } catch (ValidationException invalidDate) {
                ConsoleFormat.printError(invalidDate.getMessage());
            }
        }
    }

    // Reads a line that must not be blank. Blank input is rejected and asked again.
    private String readRequiredText(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String text = readCancellableLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            ConsoleFormat.printError("A value is required. Type a value or " + Validator.CANCEL_COMMAND + ".");
        }
    }

    // Reads one line, rejects control characters, trims it, and checks for /cancel.
    private String readCancellableLine(String prompt) throws InputCancelledException, EndOfInputException {
        while (true) {
            String line = readLine(prompt);
            try {
                Validator.checkPrintable(line);
            } catch (ValidationException badCharacters) {
                ConsoleFormat.printError(badCharacters.getMessage());
                continue;
            }
            String text = line.trim();
            if (Validator.isCancelCommand(text)) {
                throw new InputCancelledException();
            }
            return text;
        }
    }

    // Prints the prompt and reads one raw line. If the input has ended, throws instead of looping.
    private String readLine(String prompt) throws EndOfInputException {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            throw new EndOfInputException();
        }
        return scanner.nextLine();
    }
}
