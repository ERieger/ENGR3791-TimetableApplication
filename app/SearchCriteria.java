/** Holds user-supplied search criteria for class filtering. All fields optional (null = any). */
public class SearchCriteria {
    String topicCode;
    String topicName;
    String mode;
    String campus;
    String semester;
    int    offeringGroup  = 0;  // 0 = not specified
    String classType;
    int    instanceNumber = 0;
    String firstDate;
    String lastDate;
    String day;
    String timeStart;
    String timeEnd;
    String building;
    String room;

    boolean isEmpty() {
        return topicCode == null && topicName == null && mode == null && campus == null
            && semester == null && offeringGroup == 0 && classType == null && instanceNumber == 0
            && firstDate == null && lastDate == null && day == null
            && timeStart == null && timeEnd == null && building == null && room == null;
    }

    boolean hasSessionFilters() {
        return firstDate != null || lastDate != null || day != null
            || timeStart != null || timeEnd != null || building != null || room != null;
    }
}
