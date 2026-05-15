import java.util.ArrayList;
import java.util.List;

/**
 * Represents one enrollable class instance (maps to class_instances + aggregate session data).
 * Multiple sessions with the same (offering, class_type, instance_number) are combined here.
 */
public class ClassRecord {
    final int    classInstanceId;
    final String topicCode;
    final String topicName;
    final String mode;           // attendance mode, e.g. "In person"
    final String campus;
    final String semester;
    final int    offeringGroup;  // availability number
    final String classType;
    final int    instanceNumber;
    final String firstDate;      // MIN(date_start) across all sessions
    final String lastDate;       // MAX(date_end) across all sessions
    List<SessionRecord> sessions = new ArrayList<>();

    ClassRecord(int classInstanceId, String topicCode, String topicName,
                String mode, String campus, String semester, int offeringGroup,
                String classType, int instanceNumber,
                String firstDate, String lastDate) {
        this.classInstanceId = classInstanceId;
        this.topicCode       = topicCode;
        this.topicName       = topicName;
        this.mode            = mode;
        this.campus          = campus;
        this.semester        = semester;
        this.offeringGroup   = offeringGroup;
        this.classType       = classType;
        this.instanceNumber  = instanceNumber;
        this.firstDate       = firstDate;
        this.lastDate        = lastDate;
    }

    /** Primary session: first regular (non-once-only) session, or first session if all are special. */
    SessionRecord primarySession() {
        return sessions.stream()
                .filter(SessionRecord::isRegular)
                .findFirst()
                .orElse(sessions.isEmpty() ? null : sessions.get(0));
    }

    /** Display label, e.g. "COMP1002  Lecture 1  Bedford Park  S1-1" */
    String shortLabel() {
        return String.format("%-10s %-14s #%d  %-22s  S%s-%d",
                topicCode, classType, instanceNumber, campus, semester.replace("S", ""), offeringGroup);
    }

    boolean matchesSearch(SearchCriteria c) {
        if (c.topicCode      != null && !topicCode.toLowerCase().contains(c.topicCode.toLowerCase()))           return false;
        if (c.topicName      != null && !topicName.toLowerCase().contains(c.topicName.toLowerCase()))           return false;
        if (c.mode           != null && !mode.toLowerCase().contains(c.mode.toLowerCase()))                     return false;
        if (c.campus         != null && !campus.toLowerCase().contains(c.campus.toLowerCase()))                 return false;
        if (c.semester       != null && !semester.equalsIgnoreCase(c.semester))                                 return false;
        if (c.offeringGroup  > 0     && offeringGroup != c.offeringGroup)                                       return false;
        if (c.classType      != null && !classType.toLowerCase().contains(c.classType.toLowerCase()))           return false;
        if (c.instanceNumber > 0     && instanceNumber != c.instanceNumber)                                     return false;
        if (c.hasSessionFilters()) return sessions.stream().anyMatch(s -> sessionMatches(s, c));
        return true;
    }

    private boolean sessionMatches(SessionRecord s, SearchCriteria c) {
        if (c.firstDate != null && !s.dateStart.toLowerCase().contains(c.firstDate.toLowerCase()))  return false;
        if (c.lastDate  != null && !s.dateEnd.toLowerCase().contains(c.lastDate.toLowerCase()))     return false;
        if (c.day       != null && !s.day.toLowerCase().contains(c.day.toLowerCase()))              return false;
        if (c.timeStart != null && !s.timeStart.equals(c.timeStart))                                return false;
        if (c.timeEnd   != null && !s.timeEnd.equals(c.timeEnd))                                    return false;
        if (c.building  != null && !s.building.toLowerCase().contains(c.building.toLowerCase()))    return false;
        if (c.room      != null && !s.room.toLowerCase().contains(c.room.toLowerCase()))            return false;
        return true;
    }
}
