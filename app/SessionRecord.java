/** One row from class_sessions – a date-range block for a class instance. */
public class SessionRecord {
    final int    sessionId;
    final String dateStart;
    final String dateEnd;
    final String day;
    final String dayModifier; // null = weekly recurring; e.g. "once-only", "fortnightly"
    final String timeStart;
    final String timeEnd;
    final String location;
    final String building;
    final String room;

    SessionRecord(int sessionId, String dateStart, String dateEnd,
                  String day, String dayModifier,
                  String timeStart, String timeEnd, String location) {
        this.sessionId   = sessionId;
        this.dateStart   = dateStart;
        this.dateEnd     = dateEnd;
        this.day         = day;
        this.dayModifier = dayModifier;
        this.timeStart   = timeStart;
        this.timeEnd     = timeEnd;
        this.location    = location;
        int comma = location.indexOf(',');
        if (comma > 0) {
            this.building = location.substring(0, comma).trim();
            this.room     = location.substring(comma + 1).trim();
        } else {
            this.building = location.trim();
            this.room     = "";
        }
    }

    boolean isRegular()  { return dayModifier == null; }
    boolean isOnceOnly() { return "once-only".equalsIgnoreCase(dayModifier); }

    String dayDisplay() {
        return dayModifier != null ? day + " (" + dayModifier + ")" : day;
    }
}
