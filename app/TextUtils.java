final class TextUtils {
    private TextUtils() {}

    static String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    static boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) return false;
        return value.toLowerCase().contains(query.toLowerCase());
    }

    static int parseIntOrZero(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
