import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class TimetableExportEngine {
    private static final String EMPTY_FIELD = "";
    private static final String[] EXPORT_COLUMNS = {
            "timetable_name", "topic_code", "topic_name", "class_type", "instance_number",
            "campus", "semester", "offering_group", "mode",
            "session_day", "session_day_modifier", "session_time_start", "session_time_end",
            "session_location", "session_date_start", "session_date_end"
    };

    Path writeTimetable(TimetableMode.GeneratedTimetable timetable,
                        String outputPath,
                        TimetableMode.ExportFormat format) throws IOException {
        Path path = Paths.get(outputPath).toAbsolutePath().normalize();
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);

        return switch (format) {
            case CSV -> writeTimetableDelimited(timetable, path, ",");
            case TSV -> writeTimetableDelimited(timetable, path, "\t");
            case JSON -> writeTimetableJson(timetable, path);
        };
    }

    private Path writeTimetableDelimited(TimetableMode.GeneratedTimetable timetable, Path path, String delimiter) throws IOException {
        List<ClassRecord> classes = new ArrayList<>(timetable.selectedClasses);
        classes.sort(Comparator
                .comparing((ClassRecord c) -> c.topicCode)
                .thenComparing(c -> c.classType)
                .thenComparingInt(c -> c.instanceNumber));

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(String.join(delimiter, EXPORT_COLUMNS));
            writer.newLine();

            for (ClassRecord cr : classes) {
                if (cr.sessions.isEmpty()) {
                    writer.write(String.join(delimiter, exportDelimitedValues(
                            timetable, cr,
                            EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD,
                            EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD)));
                    writer.newLine();
                    continue;
                }

                for (SessionRecord s : cr.sessions) {
                    writer.write(String.join(delimiter, exportDelimitedValues(
                            timetable, cr,
                            s.day, s.dayModifier, s.timeStart, s.timeEnd,
                            s.location, s.dateStart, s.dateEnd)));
                    writer.newLine();
                }
            }
        }
        return path;
    }

    private Path writeTimetableJson(TimetableMode.GeneratedTimetable timetable, Path path) throws IOException {
        List<ClassRecord> classes = new ArrayList<>(timetable.selectedClasses);
        classes.sort(Comparator
                .comparing((ClassRecord c) -> c.topicCode)
                .thenComparing(c -> c.classType)
                .thenComparingInt(c -> c.instanceNumber));

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("[");
            writer.newLine();
            boolean first = true;

            for (ClassRecord cr : classes) {
                if (cr.sessions.isEmpty()) {
                    first = writeJsonRow(writer, first,
                            timetable.name, cr.topicCode, cr.topicName, cr.classType,
                            String.valueOf(cr.instanceNumber), cr.campus, cr.semester, String.valueOf(cr.offeringGroup),
                            cr.mode,
                            EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD);
                    continue;
                }

                for (SessionRecord s : cr.sessions) {
                    first = writeJsonRow(writer, first,
                            timetable.name, cr.topicCode, cr.topicName, cr.classType,
                            String.valueOf(cr.instanceNumber), cr.campus, cr.semester, String.valueOf(cr.offeringGroup),
                            cr.mode, s.day, s.dayModifier, s.timeStart, s.timeEnd, s.location, s.dateStart, s.dateEnd);
                }
            }

            writer.newLine();
            writer.write("]");
            writer.newLine();
        }
        return path;
    }

    private boolean writeJsonRow(BufferedWriter writer,
                                 boolean first,
                                 String timetableName,
                                 String topicCode,
                                 String topicName,
                                 String classType,
                                 String instanceNumber,
                                 String campus,
                                 String semester,
                                 String offeringGroup,
                                 String mode,
                                 String sessionDay,
                                 String sessionDayModifier,
                                 String sessionTimeStart,
                                 String sessionTimeEnd,
                                 String sessionLocation,
                                 String sessionDateStart,
                                 String sessionDateEnd) throws IOException {
        if (!first) {
            writer.write(",");
            writer.newLine();
        }
        writer.write("  {");
        writer.newLine();
        writer.write("    \"timetable_name\": " + json(timetableName) + ",");
        writer.newLine();
        writer.write("    \"topic_code\": " + json(topicCode) + ",");
        writer.newLine();
        writer.write("    \"topic_name\": " + json(topicName) + ",");
        writer.newLine();
        writer.write("    \"class_type\": " + json(classType) + ",");
        writer.newLine();
        writer.write("    \"instance_number\": " + json(instanceNumber) + ",");
        writer.newLine();
        writer.write("    \"campus\": " + json(campus) + ",");
        writer.newLine();
        writer.write("    \"semester\": " + json(semester) + ",");
        writer.newLine();
        writer.write("    \"offering_group\": " + json(offeringGroup) + ",");
        writer.newLine();
        writer.write("    \"mode\": " + json(mode) + ",");
        writer.newLine();
        writer.write("    \"session_day\": " + json(sessionDay) + ",");
        writer.newLine();
        writer.write("    \"session_day_modifier\": " + json(sessionDayModifier) + ",");
        writer.newLine();
        writer.write("    \"session_time_start\": " + json(sessionTimeStart) + ",");
        writer.newLine();
        writer.write("    \"session_time_end\": " + json(sessionTimeEnd) + ",");
        writer.newLine();
        writer.write("    \"session_location\": " + json(sessionLocation) + ",");
        writer.newLine();
        writer.write("    \"session_date_start\": " + json(sessionDateStart) + ",");
        writer.newLine();
        writer.write("    \"session_date_end\": " + json(sessionDateEnd));
        writer.newLine();
        writer.write("  }");
        return false;
    }

    private String[] exportDelimitedValues(TimetableMode.GeneratedTimetable timetable,
                                           ClassRecord cr,
                                           String sessionDay,
                                           String sessionDayModifier,
                                           String sessionTimeStart,
                                           String sessionTimeEnd,
                                           String sessionLocation,
                                           String sessionDateStart,
                                           String sessionDateEnd) {
        return new String[]{
                delimited(timetable.name),
                delimited(cr.topicCode),
                delimited(cr.topicName),
                delimited(cr.classType),
                delimited(String.valueOf(cr.instanceNumber)),
                delimited(cr.campus),
                delimited(cr.semester),
                delimited(String.valueOf(cr.offeringGroup)),
                delimited(cr.mode),
                delimited(sessionDay),
                delimited(sessionDayModifier),
                delimited(sessionTimeStart),
                delimited(sessionTimeEnd),
                delimited(sessionLocation),
                delimited(sessionDateStart),
                delimited(sessionDateEnd)
        };
    }

    private String delimited(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String json(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder(safe.length() + 8);
        out.append('"');
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
