import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Database {
    private final Connection conn;

    Database(String dbPath) throws Exception {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        ensureSchema();
    }

    void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /** Load all class instances with aggregate date bounds. Sessions loaded per record. */
    List<ClassRecord> loadAllClasses() throws SQLException {
        String sql = """
                SELECT
                    ci.class_instance_id,
                    t.topic_id, o.offering_id,
                    t.topic_code, t.topic_name,
                    o.mode, ca.campus_name, o.semester, o.offering_group,
                    ci.class_type, ci.instance_number,
                    MIN(cs.date_start) AS first_date,
                    MAX(cs.date_end)   AS last_date
                FROM class_instances ci
                JOIN topic_offerings o  ON ci.offering_id      = o.offering_id
                JOIN topics t           ON o.topic_id           = t.topic_id
                JOIN campuses ca        ON o.campus_id          = ca.campus_id
                JOIN class_sessions cs  ON ci.class_instance_id = cs.class_instance_id
                GROUP BY ci.class_instance_id
                ORDER BY t.topic_code, ca.campus_name, o.semester, o.offering_group,
                         ci.class_type, ci.instance_number
                """;
        List<ClassRecord> list = new ArrayList<>();
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ClassRecord(
                        rs.getInt("class_instance_id"),
                        rs.getInt("topic_id"),
                        rs.getInt("offering_id"),
                        rs.getString("topic_code"),
                        rs.getString("topic_name"),
                        rs.getString("mode"),
                        rs.getString("campus_name"),
                        rs.getString("semester"),
                        rs.getInt("offering_group"),
                        rs.getString("class_type"),
                        rs.getInt("instance_number"),
                        rs.getString("first_date"),
                        rs.getString("last_date")));
            }
        }
        for (ClassRecord cr : list) cr.sessions = loadSessions(cr.classInstanceId);
        return list;
    }

    List<SessionRecord> loadSessions(int classInstanceId) throws SQLException {
        String sql = """
                SELECT session_id, date_start, date_end, day, day_modifier,
                       time_start, time_end, location
                FROM class_sessions
                WHERE class_instance_id = ?
                ORDER BY CASE WHEN day_modifier IS NULL THEN 0 ELSE 1 END, session_id
                """;
        List<SessionRecord> sessions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classInstanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(new SessionRecord(
                            rs.getInt("session_id"),
                            rs.getString("date_start"),
                            rs.getString("date_end"),
                            rs.getString("day"),
                            rs.getString("day_modifier"),
                            rs.getString("time_start"),
                            rs.getString("time_end"),
                            rs.getString("location")));
                }
            }
        }
        return sessions;
    }

    /** Number of class instances sharing this offering (used for cascade-edit warnings). */
    int countClassesForOffering(int offeringId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM class_instances WHERE offering_id = ?")) {
            ps.setInt(1, offeringId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Number of class instances sharing this topic (used for cascade-edit warnings). */
    int countClassesForTopic(int topicId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) FROM class_instances ci
                JOIN topic_offerings o USING(offering_id)
                WHERE o.topic_id = ?""")) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Returns distinct campus names. */
    List<String> campuses() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT campus_name FROM campuses ORDER BY campus_name")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    /** Returns distinct topic codes + names. */
    List<String[]> topics() throws SQLException {
        List<String[]> out = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT topic_code, topic_name FROM topics ORDER BY topic_code")) {
            while (rs.next()) out.add(new String[]{rs.getString(1), rs.getString(2)});
        }
        return out;
    }

    boolean hasData() throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM class_instances")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /** Deletes a class instance and all its sessions. */
    void deleteClassInstance(int classInstanceId) throws SQLException {
        runInTransaction(() -> {
            exec("DELETE FROM class_sessions WHERE class_instance_id = ?", classInstanceId);
            exec("DELETE FROM class_instances WHERE class_instance_id = ?", classInstanceId);
        });
    }

    // -------------------------------------------------------------------------
    // Edit – topic level  (affects ALL classes that share this topic)
    // -------------------------------------------------------------------------

    void updateTopicCode(int topicId, String newCode) throws SQLException {
        exec("UPDATE topics SET topic_code = ? WHERE topic_id = ?", newCode, topicId);
    }

    void updateTopicName(int topicId, String newName) throws SQLException {
        exec("UPDATE topics SET topic_name = ? WHERE topic_id = ?", newName, topicId);
    }

    // -------------------------------------------------------------------------
    // Edit – offering level  (affects ALL classes that share this offering)
    // -------------------------------------------------------------------------

    void updateOfferingMode(int offeringId, String newMode) throws SQLException {
        exec("UPDATE topic_offerings SET mode = ? WHERE offering_id = ?", newMode, offeringId);
    }

    void updateOfferingSemester(int offeringId, String newSemester) throws SQLException {
        exec("UPDATE topic_offerings SET semester = ? WHERE offering_id = ?", newSemester, offeringId);
    }

    void updateOfferingGroup(int offeringId, int newGroup) throws SQLException {
        exec("UPDATE topic_offerings SET offering_group = ? WHERE offering_id = ?", newGroup, offeringId);
    }

    /** Updates campus for this offering, creating the campus record if it does not exist. */
    void updateOfferingCampus(int offeringId, String newCampusName) throws SQLException {
        int campusId = getOrCreateCampus(newCampusName);
        exec("UPDATE topic_offerings SET campus_id = ? WHERE offering_id = ?", campusId, offeringId);
    }

    // -------------------------------------------------------------------------
    // Edit – class instance level  (only this class)
    // -------------------------------------------------------------------------

    void updateClassType(int classInstanceId, String newClassType) throws SQLException {
        exec("UPDATE class_instances SET class_type = ? WHERE class_instance_id = ?",
                newClassType, classInstanceId);
    }

    void updateInstanceNumber(int classInstanceId, int newInstanceNumber) throws SQLException {
        exec("UPDATE class_instances SET instance_number = ? WHERE class_instance_id = ?",
                newInstanceNumber, classInstanceId);
    }

    // -------------------------------------------------------------------------
    // Edit – session level  (all sessions of this class)
    // -------------------------------------------------------------------------

    /** Updates day across ALL sessions of this class (preserves day_modifier). */
    void updateAllSessionDays(int classInstanceId, String newDay) throws SQLException {
        exec("UPDATE class_sessions SET day = ? WHERE class_instance_id = ?",
                newDay, classInstanceId);
    }

    void updateAllSessionTimeStart(int classInstanceId, String newTimeStart) throws SQLException {
        exec("UPDATE class_sessions SET time_start = ? WHERE class_instance_id = ?",
                newTimeStart, classInstanceId);
    }

    void updateAllSessionTimeEnd(int classInstanceId, String newTimeEnd) throws SQLException {
        exec("UPDATE class_sessions SET time_end = ? WHERE class_instance_id = ?",
                newTimeEnd, classInstanceId);
    }

    /** Updates building+room across ALL sessions (rebuilds location string). */
    void updateAllSessionBuilding(int classInstanceId, String newBuilding) throws SQLException {
        updateAllSessionLocations(classInstanceId,
                s -> s.room.isBlank() ? newBuilding : newBuilding + ", " + s.room);
    }

    /** Updates room across ALL sessions (preserves existing building portion). */
    void updateAllSessionRoom(int classInstanceId, String newRoom) throws SQLException {
        updateAllSessionLocations(classInstanceId,
                s -> s.building.isBlank() ? newRoom : s.building + ", " + newRoom);
    }

    /**
     * Updates date_start of the earliest session (by text order).
     * Updates ALL sessions that share that same date_start value.
     */
    void updateFirstDate(int classInstanceId, String newDateStart) throws SQLException {
        String earliest = queryString(
                "SELECT MIN(date_start) FROM class_sessions WHERE class_instance_id = ?",
                classInstanceId);
        if (earliest == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE class_sessions SET date_start = ? WHERE class_instance_id = ? AND date_start = ?")) {
            ps.setString(1, newDateStart);
            ps.setInt(2, classInstanceId);
            ps.setString(3, earliest);
            ps.executeUpdate();
        }
    }

    /**
     * Updates date_end of the latest session (by text order).
     * Updates ALL sessions that share that same date_end value.
     */
    void updateLastDate(int classInstanceId, String newDateEnd) throws SQLException {
        String latest = queryString(
                "SELECT MAX(date_end) FROM class_sessions WHERE class_instance_id = ?",
                classInstanceId);
        if (latest == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE class_sessions SET date_end = ? WHERE class_instance_id = ? AND date_end = ?")) {
            ps.setString(1, newDateEnd);
            ps.setInt(2, classInstanceId);
            ps.setString(3, latest);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int getOrCreateCampus(String name) throws SQLException {
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
        throw new SQLException("Campus not found after insert: " + name);
    }

    /** Single-param string query helper. */
    private String queryString(String sql, int param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** Ensures required tables exist so the app can start with an empty database file. */
    private void ensureSchema() throws SQLException {
        try (Statement s = conn.createStatement()) {
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

            s.execute("""
                    CREATE TABLE IF NOT EXISTS class_instances (
                        class_instance_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        offering_id       INTEGER NOT NULL REFERENCES topic_offerings(offering_id),
                        class_type        TEXT    NOT NULL,
                        instance_number   INTEGER NOT NULL,
                        UNIQUE(offering_id, class_type, instance_number)
                    )""");

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

    /** Executes an UPDATE/DELETE with up to two parameters (int or String). */
    private void exec(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer n) ps.setInt(i + 1, n);
                else                                 ps.setString(i + 1, (String) params[i]);
            }
            ps.executeUpdate();
        }
    }

    private void updateAllSessionLocations(int classInstanceId,
                                           Function<SessionRecord, String> locationBuilder) throws SQLException {
        List<SessionRecord> sessions = loadSessions(classInstanceId);
        runInTransaction(() -> {
            for (SessionRecord session : sessions) {
                exec("UPDATE class_sessions SET location = ? WHERE session_id = ?",
                        locationBuilder.apply(session), session.sessionId);
            }
        });
    }

    private void runInTransaction(SqlAction action) throws SQLException {
        boolean previousAutoCommit = conn.getAutoCommit();
        boolean autoCommitChanged = false;
        try {
            conn.setAutoCommit(false);
            autoCommitChanged = true;
            action.run();
            conn.commit();
        } catch (SQLException e) {
            if (autoCommitChanged) conn.rollback();
            throw e;
        } finally {
            if (autoCommitChanged) conn.setAutoCommit(previousAutoCommit);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
