// Which utang records to show in "View utang history": all, unpaid only, or paid only.
// Demonstrates ENCAPSULATION: the filtering rule lives with each constant.
public enum HistoryFilter {
    ALL("All utang records"),
    UNPAID("Unpaid utang records"),
    PAID("Paid utang records");

    private final String title;

    HistoryFilter(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    // Returns true if the record belongs in this view. Undone records appear only in ALL.
    public boolean matches(UtangTransaction record) {
        switch (this) {
            case ALL:
                return true;
            case UNPAID:
                return !record.isUndone() && !record.isPaid();
            case PAID:
                return !record.isUndone() && record.isPaid();
            default:
                throw new IllegalStateException("Unknown filter: " + this);
        }
    }
}
