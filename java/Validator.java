/** Member 5: input validation used by every admin screen. */
public final class Validator {
    private Validator() {}
    public static String text(String s, String field, int max) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be empty.");
        if (s.trim().length() > max) throw new IllegalArgumentException(field + " is too long (max " + max + ").");
        return s.trim();
    }
    public static int positiveInt(String s, String field) {
        try {
            int v = Integer.parseInt(s == null ? "" : s.trim());
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) { throw new IllegalArgumentException(field + " must be a positive number."); }
    }
    public static int rating(String s) {
        int v = positiveInt(s, "Rating");
        if (v > 5) throw new IllegalArgumentException("Rating must be between 1 and 5.");
        return v;
    }
}
