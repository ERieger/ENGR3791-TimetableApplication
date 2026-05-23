import java.util.ArrayList;
import java.util.List;

/**
 * Represents one enrollable class instance (maps to class_instances + aggregate session data).
 * Multiple sessions with the same (offering, class_type, instance_number) are combined here.
 */
public class ClassRecord {
    final int    classInstanceId;
    final int    topicId;         // needed for topic-level edits
    final int    offeringId;      // needed for offering-level edits
    final String topicCode;
    final String topicName;
    final String mode;            // attendance mode, e.g. "In person"
    final String campus;
    final String semester;
    final int    offeringGroup;   // availability number
    final String classType;
    final int    instanceNumber;
    final String firstDate;       // MIN(date_start) across all sessions
    final String lastDate;        // MAX(date_end) across all sessions
    List<SessionRecord> sessions = new ArrayList<>();

    ClassRecord(int classInstanceId, int topicId, int offeringId,
                String topicCode, String topicName,
                String mode, String campus, String semester, int offeringGroup,
                String classType, int instanceNumber,
                String firstDate, String lastDate) {
        this.classInstanceId = classInstanceId;
        this.topicId         = topicId;
        this.offeringId      = offeringId;
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
        if (c.topicCode      != null && !TextUtils.containsIgnoreCase(topicCode, c.topicCode))                  return false;
        if (c.topicName      != null && !TextUtils.containsIgnoreCase(topicName, c.topicName))                  return false;
        if (c.mode           != null && !TextUtils.containsIgnoreCase(mode, c.mode))                            return false;
        if (c.campus         != null && !TextUtils.containsIgnoreCase(campus, c.campus))                        return false;
        if (c.semester       != null && !semester.equalsIgnoreCase(c.semester))                                 return false;
        if (c.offeringGroup  > 0     && offeringGroup != c.offeringGroup)                                       return false;
        if (c.classType      != null && !TextUtils.containsIgnoreCase(classType, c.classType))                  return false;
        if (c.instanceNumber > 0     && instanceNumber != c.instanceNumber)                                     return false;
        if (c.hasSessionFilters()) return sessions.stream().anyMatch(s -> sessionMatches(s, c));
        return true;
    }

    private boolean sessionMatches(SessionRecord s, SearchCriteria c) {
        if (c.firstDate != null && !TextUtils.containsIgnoreCase(s.dateStart, c.firstDate))         return false;
        if (c.lastDate  != null && !TextUtils.containsIgnoreCase(s.dateEnd, c.lastDate))            return false;
        if (c.day       != null && !TextUtils.containsIgnoreCase(s.day, c.day))                     return false;
        if (c.timeStart != null && !s.timeStart.equals(c.timeStart))                                return false;
        if (c.timeEnd   != null && !s.timeEnd.equals(c.timeEnd))                                    return false;
        if (c.building  != null && !TextUtils.containsIgnoreCase(s.building, c.building))           return false;
        if (c.room      != null && !TextUtils.containsIgnoreCase(s.room, c.room))                   return false;
        return true;
    }
}
