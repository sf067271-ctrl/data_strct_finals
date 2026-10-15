/**
 * Thrown by InputHelper when the operator types X to cancel.
 *
 * Main catches it, prints "Cancelled", and returns to the menu. Because every
 * workflow only changes records AFTER all input is collected and confirmed,
 * cancelling never leaves a half-finished change behind.
 *
 * It is UNCHECKED (extends RuntimeException) so it does not have to be
 * declared on every method that reads input.
 */
public class InputCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InputCancelledException() {
        super("Cancelled by the operator.");
    }
}
