import java.util.Locale;

// Small helper for consistent terminal output: headings, [OK]/[ERROR] messages and table cells.
// Demonstrates ENCAPSULATION: the look of the output is decided in one place.
public final class ConsoleFormat {
    private static final String ELLIPSIS = "...";
    private static final int LABEL_WIDTH = 26;

    private ConsoleFormat() {
        // Utility class: no objects needed.
    }

    // Prints a section heading with an underline.
    public static void printHeading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    // Prints a success message.
    public static void printOk(String message) {
        System.out.println("[OK] " + message);
    }

    // Prints an error message.
    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    // Cuts text that is longer than the column width so tables stay aligned ("Safeguard Sha...").
    public static String fit(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width - ELLIPSIS.length()) + ELLIPSIS;
    }

    // Formats one "label: value" line so details line up. Ends with a line break.
    public static String detailLine(String label, String value) {
        return String.format(Locale.US, "  %-" + LABEL_WIDTH + "s %s%n", label + ":", value);
    }

    // Returns a line of dashes as wide as the given text, used under table headers.
    public static String underline(String header) {
        return "-".repeat(header.length());
    }
}
