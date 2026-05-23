import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class CsvParsingUtils {
    private CsvParsingUtils() {}

    /** "In person - Bedford Park - S1 - 1"  →  ["In person", "Bedford Park", "S1", "1"] */
    static String[] parseAvailability(String value) {
        String[] parts = value.split(" - ", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Cannot parse availability: " + value);
        }
        return new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()};
    }

    /** "Monday (once-only)" → ["Monday", "once-only"], "Wednesday" → ["Wednesday", null] */
    static String[] parseDay(String value) {
        int open = value.indexOf('(');
        if (open >= 0 && value.endsWith(")")) {
            String day = value.substring(0, open).trim();
            String modifier = value.substring(open + 1, value.length() - 1).trim();
            return new String[]{day, modifier};
        }
        return new String[]{value.trim(), null};
    }

    /** "14:00 - 16:00" → ["14:00", "16:00"] */
    static String[] parseTime(String value) {
        String[] parts = value.split(" - ", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cannot parse time: " + value);
        }
        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    /** "11 Mar - 08 Apr" → ["11 Mar", "08 Apr"] */
    static String[] parseDateRange(String value) {
        int idx = value.indexOf(" - ");
        if (idx < 0) {
            throw new IllegalArgumentException("Cannot parse date range: " + value);
        }
        return new String[]{value.substring(0, idx).trim(), value.substring(idx + 3).trim()};
    }

    static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    static String cell(String[] row, int col) {
        return col < row.length ? row[col].trim() : "";
    }

    static int findCol(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(name)) return i;
        }
        throw new IllegalArgumentException("Column not found: " + name);
    }

    static int findColAny(String[] header, String... candidates) {
        for (String name : candidates) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].trim().equalsIgnoreCase(name)) return i;
            }
        }
        throw new IllegalArgumentException("None of these columns found: " + Arrays.toString(candidates));
    }

    static int parseInt(String text, Path file, int rowIndex) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Expected integer at row %d of %s, got: '%s'", rowIndex, file.getFileName(), text));
        }
    }
}
