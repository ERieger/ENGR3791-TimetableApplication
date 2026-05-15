import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final Connection conn;

    Database(String dbPath) throws Exception {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
    }

    void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }

    /** Load all class instances with aggregate date bounds. Sessions loaded per record. */
    List<ClassRecord> loadAllClasses() throws SQLException {
        String sql = """
                SELECT
                    ci.class_instance_id,
                    t.topic_code, t.topic_name,
                    o.mode, ca.campus_name, o.semester, o.offering_group,
                    ci.class_type, ci.instance_number,
                    MIN(cs.date_start) AS first_date,
                    MAX(cs.date_end)   AS last_date
                FROM class_instances ci
                JOIN topic_offerings o  ON ci.offering_id   = o.offering_id
                JOIN topics t           ON o.topic_id        = t.topic_id
                JOIN campuses ca        ON o.campus_id       = ca.campus_id
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
}
