import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Loads timetable CSV files into an SQLite database.
 *
 * Usage: java -cp ".:lib/*" CsvToSqliteLoader [csv-dir] [output.db]
 *
 * Schema overview:
 *   campuses          – unique campus names
 *   topics            – course code + full name
 *   topic_offerings   – one row per (topic, campus, semester, group)
 *   class_instances   – enrollable choice, e.g. "Practical 5"
 *   class_sessions    – one row per CSV data row (day/time/location can vary
 *                        across sessions of the same instance, e.g. makeup classes)
 */
public class CsvToSqliteLoader {

    public static void main(String[] args) throws Exception {
        String csvDir = args.length > 0 ? args[0] : "../Spec and CSVs/CSV";
        String dbFile = args.length > 1 ? args[1] : "timetable.db";

        System.out.println("CSV directory : " + csvDir);
        System.out.println("Database file : " + dbFile);

        // Register SQLite driver (bundled in sqlite-jdbc JAR)
        Class.forName("org.sqlite.JDBC");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            // Set WAL mode before any transaction
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA journal_mode = WAL");
            }
            conn.setAutoCommit(false);
            createSchema(conn);
            conn.commit();

            int fileCount = 0;
            int rowCount = 0;

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(csvDir), "*.csv")) {
                for (Path csvFile : ds) {
                    System.out.println("  Loading: " + csvFile.getFileName());
                    rowCount += loadCsvFile(conn, csvFile);
                    conn.commit();
                    fileCount++;
                }
            }

            System.out.printf("Done. Loaded %d file(s), %d data row(s).%n", fileCount, rowCount);
        }
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");

            s.execute("""
                    CREATE TABLE IF NOT EXISTS campuses (
                        campus_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        campus_name TEXT NOT NULL UNIQUE
                    )""");

            s.execute("""
                    CREATE TABLE IF NOT EXISTS topics (
                        topic_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        topic_code TEXT NOT NULL UNIQUE,
                        topic_name TEXT NOT NULL
                    )""");

            // One offering per (topic, campus, semester, group number).
            // "mode" is the delivery mode, e.g. "In person".
            s.execute("""
                    CREATE TABLE IF NOT EXISTS topic_offerings (
                        offering_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                        topic_id       INTEGER NOT NULL REFERENCES topics(topic_id),
                        campus_id      INTEGER NOT NULL REFERENCES campuses(campus_id),
                        mode           TEXT    NOT NULL,
                        semester       TEXT    NOT NULL,
                        offering_group INTEGER NOT NULL,
                        UNIQUE(topic_id, campus_id, semester, offering_group)
                    )""");

            // Represents one enrollable class choice, e.g. "Practical instance 2".
            s.execute("""
                    CREATE TABLE IF NOT EXISTS class_instances (
                        class_instance_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        offering_id       INTEGER NOT NULL REFERENCES topic_offerings(offering_id),
                        class_type        TEXT    NOT NULL,
                        instance_number   INTEGER NOT NULL,
                        UNIQUE(offering_id, class_type, instance_number)
                    )""");

            // One row per CSV data row. Location, day, and time are stored here
            // because makeup / once-only sessions can differ from the regular slot.
            // day_modifier: NULL = weekly, "once-only", "fortnightly", etc.
            s.execute("""
                    CREATE TABLE IF NOT EXISTS class_sessions (
                        session_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        class_instance_id INTEGER NOT NULL REFERENCES class_instances(class_instance_id),
                        date_start        TEXT NOT NULL,
                        date_end          TEXT NOT NULL,
                        day               TEXT NOT NULL,
                        day_modifier      TEXT,
                        time_start        TEXT NOT NULL,
                        time_end          TEXT NOT NULL,
                        location          TEXT NOT NULL,
                        UNIQUE(class_instance_id, date_start, date_end, day, time_start, time_end, location)
                    )""");
        }
    }

    // -------------------------------------------------------------------------
    // CSV loading
    // -------------------------------------------------------------------------

    private static int loadCsvFile(Connection conn, Path csvFile) throws Exception {
        List<String[]> rows = parseCsv(csvFile);
        if (rows.size() < 2) return 0;

        String[] header = rows.get(0);
        int iTopic    = CsvParsingUtils.findCol(header, "Topic");
        int iAvail    = CsvParsingUtils.findCol(header, "Availability");
        int iClass    = CsvParsingUtils.findCol(header, "Class");
        int iInstance = CsvParsingUtils.findCol(header, "Class instance");
        int iDate     = CsvParsingUtils.findCol(header, "Date");
        int iDay      = CsvParsingUtils.findCol(header, "Day");
        int iTime     = CsvParsingUtils.findCol(header, "Time");
        int iLocation = CsvParsingUtils.findColAny(header, "Location", "Room");

        int loaded = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length == 0 || (row.length == 1 && row[0].isBlank())) continue;

            String topicFull  = CsvParsingUtils.cell(row, iTopic);
            String avail      = CsvParsingUtils.cell(row, iAvail);
            String classType  = CsvParsingUtils.cell(row, iClass);
            int instanceNum   = CsvParsingUtils.parseInt(CsvParsingUtils.cell(row, iInstance), csvFile, i);
            String dateRange  = CsvParsingUtils.cell(row, iDate);
            String dayFull    = CsvParsingUtils.cell(row, iDay);
            String timeRange  = CsvParsingUtils.cell(row, iTime);
            String location   = CsvParsingUtils.cell(row, iLocation);

            String topicCode  = topicFull.split("\\s+")[0];

            String[] availParts = CsvParsingUtils.parseAvailability(avail);
            String mode         = availParts[0];
            String campusName   = availParts[1];
            String semester     = availParts[2];
            int offeringGroup   = CsvParsingUtils.parseInt(availParts[3], csvFile, i);

            String[] dayParts   = CsvParsingUtils.parseDay(dayFull);
            String day          = dayParts[0];
            String dayModifier  = dayParts[1]; // may be null

            String[] timeParts  = CsvParsingUtils.parseTime(timeRange);
            String timeStart    = timeParts[0];
            String timeEnd      = timeParts[1];

            String[] dateParts  = CsvParsingUtils.parseDateRange(dateRange);
            String dateStart    = dateParts[0];
            String dateEnd      = dateParts[1];

            int campusId        = getOrCreateCampus(conn, campusName);
            int topicId         = getOrCreateTopic(conn, topicCode, topicFull);
            int offeringId      = getOrCreateOffering(conn, topicId, campusId, mode, semester, offeringGroup);
            int classInstanceId = getOrCreateClassInstance(conn, offeringId, classType, instanceNum);
            insertSession(conn, classInstanceId, dateStart, dateEnd, day, dayModifier, timeStart, timeEnd, location);

            loaded++;
        }
        return loaded;
    }

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    /** "In person - Bedford Park - S1 - 1"  →  ["In person", "Bedford Park", "S1", "1"] */
    private static String[] parseAvailability(String s) {
        String[] parts = s.split(" - ", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Cannot parse availability: " + s);
        }
        return new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()};
    }

    /** "Monday (once-only)" → ["Monday", "once-only"],  "Wednesday" → ["Wednesday", null] */
    private static String[] parseDay(String s) {
        int open = s.indexOf('(');
        if (open >= 0 && s.endsWith(")")) {
            String day      = s.substring(0, open).trim();
            String modifier = s.substring(open + 1, s.length() - 1).trim();
            return new String[]{day, modifier};
        }
        return new String[]{s.trim(), null};
    }

    /** "14:00 - 16:00" → ["14:00", "16:00"] */
    private static String[] parseTime(String s) {
        String[] parts = s.split(" - ", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cannot parse time: " + s);
        }
        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    /** "11 Mar - 08 Apr" → ["11 Mar", "08 Apr"] */
    private static String[] parseDateRange(String s) {
        // Split on " - " but be careful: "02 Mar - 02 Mar"
        int idx = s.indexOf(" - ");
        if (idx < 0) {
            throw new IllegalArgumentException("Cannot parse date range: " + s);
        }
        return new String[]{s.substring(0, idx).trim(), s.substring(idx + 3).trim()};
    }

    // -------------------------------------------------------------------------
    // DB helpers – getOrCreate pattern using INSERT OR IGNORE + SELECT
    // -------------------------------------------------------------------------

    private static int getOrCreateCampus(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO campuses (campus_name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT campus_id FROM campuses WHERE campus_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to get campus: " + name);
    }

    private static int getOrCreateTopic(Connection conn, String code, String fullName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO topics (topic_code, topic_name) VALUES (?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, fullName);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT topic_id FROM topics WHERE topic_code = ?")) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to get topic: " + code);
    }

    private static int getOrCreateOffering(Connection conn, int topicId, int campusId,
                                            String mode, String semester, int group) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO topic_offerings
                    (topic_id, campus_id, mode, semester, offering_group)
                VALUES (?, ?, ?, ?, ?)""")) {
            ps.setInt(1, topicId);
            ps.setInt(2, campusId);
            ps.setString(3, mode);
            ps.setString(4, semester);
            ps.setInt(5, group);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT offering_id FROM topic_offerings
                WHERE topic_id = ? AND campus_id = ? AND semester = ? AND offering_group = ?""")) {
            ps.setInt(1, topicId);
            ps.setInt(2, campusId);
            ps.setString(3, semester);
            ps.setInt(4, group);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to get offering for topicId=" + topicId);
    }

    private static int getOrCreateClassInstance(Connection conn, int offeringId,
                                                  String classType, int instanceNum) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO class_instances
                    (offering_id, class_type, instance_number)
                VALUES (?, ?, ?)""")) {
            ps.setInt(1, offeringId);
            ps.setString(2, classType);
            ps.setInt(3, instanceNum);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT class_instance_id FROM class_instances
                WHERE offering_id = ? AND class_type = ? AND instance_number = ?""")) {
            ps.setInt(1, offeringId);
            ps.setString(2, classType);
            ps.setInt(3, instanceNum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to get class instance for offeringId=" + offeringId);
    }

    private static void insertSession(Connection conn, int classInstanceId,
                                       String dateStart, String dateEnd,
                                       String day, String dayModifier,
                                       String timeStart, String timeEnd,
                                       String location) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO class_sessions
                    (class_instance_id, date_start, date_end, day, day_modifier,
                     time_start, time_end, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")) {
            ps.setInt(1, classInstanceId);
            ps.setString(2, dateStart);
            ps.setString(3, dateEnd);
            ps.setString(4, day);
            if (dayModifier != null) ps.setString(5, dayModifier);
            else ps.setNull(5, Types.VARCHAR);
            ps.setString(6, timeStart);
            ps.setString(7, timeEnd);
            ps.setString(8, location);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // CSV parsing
    // -------------------------------------------------------------------------

    private static List<String[]> parseCsv(Path file) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    if (line.startsWith("﻿")) line = line.substring(1); // strip UTF-8 BOM
                    first = false;
                }
                if (!line.isBlank()) result.add(CsvParsingUtils.splitCsvLine(line));
            }
        }
        return result;
    }
}
