import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Handles the Browse / View / Search / Edit / Delete classes screens. */
public class ClassesView {

    private final Database db;
    private final Scanner  sc;

    ClassesView(Database db, Scanner sc) {
        this.db = db;
        this.sc = sc;
    }

    // -------------------------------------------------------------------------
    // Entry – Classes View menu
    // -------------------------------------------------------------------------

    void show() throws Exception {
        while (true) {
            Con.header("CLASSES VIEW");
            Con.menuItem("1", "Browse all classes");
            Con.menuItem("2", "View individual class");
            Con.menuItem("3", "Search classes");
            Con.menuItem("4", "Edit a class");
            Con.menuItem("5", "Delete a class");
            Con.menuItem("0", "Back to main menu");
            String choice = Con.menuPrompt(sc);
            switch (choice) {
                case "1" -> browseAll();
                case "2" -> viewIndividual();
                case "3" -> searchClasses();
                case "4" -> editClassMenu();
                case "5" -> deleteClassMenu();
                case "0" -> { return; }
                default  -> Con.warn("Unknown option – please try again.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Browse all classes
    // -------------------------------------------------------------------------

    private void browseAll() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        Con.header("BROWSE ALL CLASSES");
        if (classes.isEmpty()) { Con.warn("No class data found. Import data first."); return; }
        printBrowseTable(classes);
        Con.blankLine();
        Con.info("Showing " + classes.size() + " class instances across " +
                 classes.stream().map(c -> c.topicCode).distinct().count() + " topics.");
    }

    private void printBrowseTable(List<ClassRecord> classes) {
        Con.divider();
        System.out.println("  " + Con.b(Con.pad("#",         4))
                + Con.b(Con.pad("Topic",      11))
                + Con.b(Con.pad("Class type", 16))
                + Con.b(Con.pad("Inst",        5))
                + Con.b(Con.pad("Campus",     22))
                + Con.b(Con.pad("Sem",         5))
                + Con.b(Con.pad("Avail",       6))
                + Con.b(Con.pad("Day",        13))
                + Con.b(Con.pad("Time",       13))
                + Con.b("Date range"));
        Con.divider();

        for (int i = 0; i < classes.size(); i++) {
            ClassRecord   cr  = classes.get(i);
            SessionRecord p   = cr.primarySession();
            String day   = p != null ? p.day                     : "—";
            String time  = p != null ? p.timeStart + "–" + p.timeEnd : "—";
            String dates = cr.firstDate + "  →  " + cr.lastDate;

            System.out.println("  "
                    + Con.dim(Con.pad(String.valueOf(i + 1), 4))
                    + Con.c(Con.pad(cr.topicCode, 11))
                    + Con.pad(cr.classType, 16)
                    + Con.lpad(String.valueOf(cr.instanceNumber), 3) + "  "
                    + Con.pad(cr.campus, 22)
                    + Con.pad(cr.semester, 5)
                    + Con.lpad(String.valueOf(cr.offeringGroup), 4) + "  "
                    + Con.pad(day, 13)
                    + Con.pad(time, 13)
                    + Con.dim(dates));
        }
        Con.divider();
    }

    // -------------------------------------------------------------------------
    // View individual class
    // -------------------------------------------------------------------------

    private void viewIndividual() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        if (classes.isEmpty()) { Con.warn("No class data found. Import data first."); return; }

        Con.header("VIEW INDIVIDUAL CLASS");
        printBrowseTable(classes);

        Con.blankLine();
        String input = Con.prompt(sc, "Enter class number to view (or 0 to cancel)");
        if (input.equals("0") || input.isBlank()) return;

        int idx = parseIdx(input, classes.size());
        if (idx < 0) return;
        printClassDetail(classes.get(idx));
    }

    void printClassDetail(ClassRecord cr) {
        Con.blankLine();
        String title = cr.topicCode + "  ·  " + cr.topicName;
        String sub   = cr.classType + ", Instance " + cr.instanceNumber;
        System.out.println(Con.CYN + Con.BD + "  ┌" + "─".repeat(Con.W - 6) + "┐" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  │  " + Con.b(title)
                + " ".repeat(Math.max(0, Con.W - 6 - title.length() - 2)) + Con.CYN + "  │" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  │  " + Con.DM + sub
                + " ".repeat(Math.max(0, Con.W - 6 - sub.length() - 2))   + Con.CYN + "  │" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  └" + "─".repeat(Con.W - 6) + "┘" + Con.R);
        Con.blankLine();

        field("Topic code",          cr.topicCode);
        field("Topic name",          cr.topicName);
        field("Attendance mode",     cr.mode);
        field("Campus",              cr.campus);
        field("Semester",            cr.semester);
        field("Availability no.",    String.valueOf(cr.offeringGroup));
        field("Class",               cr.classType);
        field("Instance",            String.valueOf(cr.instanceNumber));
        field("Date of first class", cr.firstDate);
        field("Date of last class",  cr.lastDate);

        SessionRecord p = cr.primarySession();
        if (p != null) {
            field("Day",         p.dayDisplay());
            field("Start time",  p.timeStart);
            field("End time",    p.timeEnd);
            field("Building",    p.building);
            field("Room",        p.room);
        }

        Con.blankLine();
        Con.subheader("All scheduled sessions");
        Con.divider();
        for (SessionRecord s : cr.sessions) {
            String tag = s.isRegular()  ? Con.g(" weekly   ")
                       : s.isOnceOnly() ? Con.r(" once-only")
                       :                  Con.y(" " + Con.pad(s.dayModifier, 9));
            System.out.println("  " + tag
                    + "  " + Con.b(Con.pad(s.dayDisplay(), 24))
                    + "  " + Con.c(s.timeStart + " – " + s.timeEnd)
                    + "  " + Con.pad(s.dateStart + "  →  " + s.dateEnd, 28)
                    + "  " + Con.dim(s.location));
        }
        Con.divider();
        Con.blankLine();
    }

    // -------------------------------------------------------------------------
    // Search classes
    // -------------------------------------------------------------------------

    private void searchClasses() throws Exception {
        Con.header("SEARCH CLASSES");
        Con.println("Enter search criteria (leave blank to skip a field).");
        Con.blankLine();

        SearchCriteria c = new SearchCriteria();
        c.topicCode      = nullIfBlank(Con.prompt(sc, "Topic code        (e.g. COMP1002)"));
        c.topicName      = nullIfBlank(Con.prompt(sc, "Topic name        (partial match)"));
        c.mode           = nullIfBlank(Con.prompt(sc, "Attendance mode   (e.g. In person)"));
        c.campus         = nullIfBlank(Con.prompt(sc, "Campus            (e.g. Bedford Park)"));
        c.semester       = nullIfBlank(Con.prompt(sc, "Semester          (S1 / S2)"));
        String ag        =             Con.prompt(sc, "Availability no.  (e.g. 1)");
        c.offeringGroup  = ag.isBlank() ? 0 : tryInt(ag);
        c.classType      = nullIfBlank(Con.prompt(sc, "Class type        (e.g. Lecture)"));
        String in        =             Con.prompt(sc, "Instance number   (e.g. 2)");
        c.instanceNumber = in.isBlank() ? 0 : tryInt(in);
        c.firstDate      = nullIfBlank(Con.prompt(sc, "Date of first class (e.g. 03 Mar)"));
        c.lastDate       = nullIfBlank(Con.prompt(sc, "Date of last class  (e.g. 10 Jun)"));
        c.day            = nullIfBlank(Con.prompt(sc, "Day               (e.g. Monday)"));
        c.timeStart      = nullIfBlank(Con.prompt(sc, "Start time        (e.g. 09:00)"));
        c.timeEnd        = nullIfBlank(Con.prompt(sc, "End time          (e.g. 11:00)"));
        c.building       = nullIfBlank(Con.prompt(sc, "Building          (partial match)"));
        c.room           = nullIfBlank(Con.prompt(sc, "Room              (partial match)"));

        List<ClassRecord> result = db.loadAllClasses().stream()
                .filter(cr -> cr.matchesSearch(c))
                .collect(Collectors.toList());

        Con.header("SEARCH RESULTS");
        if (result.isEmpty()) {
            Con.warn("No classes matched the search criteria.");
        } else {
            printBrowseTable(result);
            Con.blankLine();
            Con.info("Found " + result.size() + " matching class instance(s).");
            Con.blankLine();
            String pick = Con.prompt(sc, "View a class? Enter list number (or 0 to skip)");
            if (!pick.equals("0") && !pick.isBlank()) {
                int idx = parseIdx(pick, result.size());
                if (idx >= 0) printClassDetail(result.get(idx));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Edit class
    // -------------------------------------------------------------------------

    private void editClassMenu() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        if (classes.isEmpty()) { Con.warn("No class data found. Import data first."); return; }

        Con.header("EDIT A CLASS");
        printBrowseTable(classes);
        Con.blankLine();
        String input = Con.prompt(sc, "Enter class number to edit (or 0 to cancel)");
        if (input.equals("0") || input.isBlank()) return;

        int idx = parseIdx(input, classes.size());
        if (idx < 0) return;
        editClass(classes.get(idx));
    }

    private void editClass(ClassRecord cr) throws Exception {
        // Re-load to get the latest data before entering the edit loop
        List<ClassRecord> all = db.loadAllClasses();
        ClassRecord current = all.stream()
                .filter(r -> r.classInstanceId == cr.classInstanceId)
                .findFirst().orElse(cr);

        while (true) {
            SessionRecord p = current.primarySession();
            String pDay      = p != null ? p.dayDisplay() : "—";
            String pStart    = p != null ? p.timeStart    : "—";
            String pEnd      = p != null ? p.timeEnd      : "—";
            String pBuilding = p != null ? p.building     : "—";
            String pRoom     = p != null ? p.room         : "—";

            Con.header("EDIT CLASS  ·  " + current.shortLabel());
            Con.println(Con.dim("Note: fields [1–2] update all classes sharing this topic."));
            Con.println(Con.dim("      fields [3–6] update all classes sharing this offering."));
            Con.println(Con.dim("      fields [11–15] update all sessions of this class."));
            Con.blankLine();

            printEditMenu(current, pDay, pStart, pEnd, pBuilding, pRoom);

            String choice = Con.menuPrompt(sc);
            if (choice.equals("0")) return;

            int field;
            try { field = Integer.parseInt(choice); }
            catch (NumberFormatException e) { Con.warn("Invalid option."); continue; }
            if (field < 1 || field > 15) { Con.warn("Invalid option."); continue; }

            applyEdit(current, field, pBuilding, pRoom);

            // Re-load so the menu reflects the change on the next loop
            all = db.loadAllClasses();
            final int id = current.classInstanceId;
            current = all.stream().filter(r -> r.classInstanceId == id).findFirst().orElse(current);
        }
    }

    private void printEditMenu(ClassRecord cr, String day, String start, String end,
                                String building, String room) {
        printEditRow( 1, "Topic code",          cr.topicCode,               false);
        printEditRow( 2, "Topic name",           cr.topicName,               false);
        printEditRow( 3, "Attendance mode",      cr.mode,                    false);
        printEditRow( 4, "Campus",               cr.campus,                  false);
        printEditRow( 5, "Semester",             cr.semester,                false);
        printEditRow( 6, "Availability no.",     String.valueOf(cr.offeringGroup), false);
        printEditRow( 7, "Class type",           cr.classType,               false);
        printEditRow( 8, "Instance no.",         String.valueOf(cr.instanceNumber), false);
        printEditRow( 9, "Date of first class",  cr.firstDate,               false);
        printEditRow(10, "Date of last class",   cr.lastDate,                false);
        printEditRow(11, "Day",                  day,                        false);
        printEditRow(12, "Start time",           start,                      false);
        printEditRow(13, "End time",             end,                        false);
        printEditRow(14, "Building",             building,                   false);
        printEditRow(15, "Room",                 room,                       false);
        Con.menuItem("0", "Done / cancel");
    }

    private static void printEditRow(int n, String label, String value, boolean unused) {
        System.out.printf("    %s%-4s%s  %-22s  %s%n",
                Con.CYN + Con.BD, "[" + n + "]", Con.R,
                label,
                value != null ? value : Con.dim("—"));
    }

    private void applyEdit(ClassRecord cr, int field, String currentBuilding, String currentRoom)
            throws Exception {
        String newVal;

        switch (field) {
            // ---- Topic-level ------------------------------------------------
            case 1 -> {
                int affected = db.countClassesForTopic(cr.topicId);
                newVal = Con.prompt(sc, "New topic code (current: " + cr.topicCode + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("topic code", cr.topicCode, newVal, affected, "topic")) return;
                try {
                    db.updateTopicCode(cr.topicId, newVal);
                    Con.success("Topic code updated.");
                } catch (Exception e) {
                    Con.error("Could not update: " + e.getMessage());
                }
            }
            case 2 -> {
                int affected = db.countClassesForTopic(cr.topicId);
                newVal = Con.prompt(sc, "New topic name (current: " + cr.topicName + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("topic name", cr.topicName, newVal, affected, "topic")) return;
                db.updateTopicName(cr.topicId, newVal);
                Con.success("Topic name updated.");
            }

            // ---- Offering-level ---------------------------------------------
            case 3 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Con.prompt(sc, "New attendance mode (current: " + cr.mode + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("attendance mode", cr.mode, newVal, affected, "offering")) return;
                db.updateOfferingMode(cr.offeringId, newVal);
                Con.success("Attendance mode updated.");
            }
            case 4 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Con.prompt(sc, "New campus (current: " + cr.campus + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("campus", cr.campus, newVal, affected, "offering")) return;
                db.updateOfferingCampus(cr.offeringId, newVal);
                Con.success("Campus updated.");
            }
            case 5 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Con.prompt(sc, "New semester (current: " + cr.semester + ", e.g. S1 / S2)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("semester", cr.semester, newVal, affected, "offering")) return;
                try {
                    db.updateOfferingSemester(cr.offeringId, newVal);
                    Con.success("Semester updated.");
                } catch (Exception e) {
                    Con.error("Could not update: " + e.getMessage());
                }
            }
            case 6 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Con.prompt(sc, "New availability no. (current: " + cr.offeringGroup + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                int newInt = tryInt(newVal);
                if (newInt <= 0) { Con.error("Must be a positive integer."); return; }
                if (!confirmEdit("availability no.", String.valueOf(cr.offeringGroup),
                                 String.valueOf(newInt), affected, "offering")) return;
                try {
                    db.updateOfferingGroup(cr.offeringId, newInt);
                    Con.success("Availability no. updated.");
                } catch (Exception e) {
                    Con.error("Could not update: " + e.getMessage());
                }
            }

            // ---- Class-instance level ----------------------------------------
            case 7 -> {
                newVal = Con.prompt(sc, "New class type (current: " + cr.classType + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEdit("class type", cr.classType, newVal, 1, "class")) return;
                db.updateClassType(cr.classInstanceId, newVal);
                Con.success("Class type updated.");
            }
            case 8 -> {
                newVal = Con.prompt(sc, "New instance no. (current: " + cr.instanceNumber + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                int newInt = tryInt(newVal);
                if (newInt <= 0) { Con.error("Must be a positive integer."); return; }
                if (!confirmEdit("instance no.", String.valueOf(cr.instanceNumber),
                                 String.valueOf(newInt), 1, "class")) return;
                db.updateInstanceNumber(cr.classInstanceId, newInt);
                Con.success("Instance no. updated.");
            }

            // ---- Session level -----------------------------------------------
            case 9 -> {
                newVal = Con.prompt(sc, "New date of first class (current: " + cr.firstDate
                                      + ", e.g. 03 Mar)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("date of first class", cr.firstDate, newVal)) return;
                db.updateFirstDate(cr.classInstanceId, newVal);
                Con.success("Date of first class updated.");
            }
            case 10 -> {
                newVal = Con.prompt(sc, "New date of last class (current: " + cr.lastDate
                                      + ", e.g. 10 Jun)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("date of last class", cr.lastDate, newVal)) return;
                db.updateLastDate(cr.classInstanceId, newVal);
                Con.success("Date of last class updated.");
            }
            case 11 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.day : "—";
                newVal = Con.prompt(sc, "New day (current: " + cur + ", e.g. Monday)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                int n = cr.sessions.size();
                if (!confirmEditSimple("day", cur, newVal + "  (applies to all " + n + " session(s))")) return;
                db.updateAllSessionDays(cr.classInstanceId, newVal);
                Con.success("Day updated across all sessions.");
            }
            case 12 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.timeStart : "—";
                newVal = Con.prompt(sc, "New start time (current: " + cur + ", e.g. 14:00)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("start time", cur, newVal)) return;
                db.updateAllSessionTimeStart(cr.classInstanceId, newVal);
                Con.success("Start time updated.");
            }
            case 13 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.timeEnd : "—";
                newVal = Con.prompt(sc, "New end time (current: " + cur + ", e.g. 16:00)");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("end time", cur, newVal)) return;
                db.updateAllSessionTimeEnd(cr.classInstanceId, newVal);
                Con.success("End time updated.");
            }
            case 14 -> {
                newVal = Con.prompt(sc, "New building (current: " + currentBuilding + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("building", currentBuilding, newVal)) return;
                db.updateAllSessionBuilding(cr.classInstanceId, newVal);
                Con.success("Building updated.");
            }
            case 15 -> {
                newVal = Con.prompt(sc, "New room (current: " + currentRoom + ")");
                if (newVal.isBlank()) { Con.warn("No change."); return; }
                if (!confirmEditSimple("room", currentRoom, newVal)) return;
                db.updateAllSessionRoom(cr.classInstanceId, newVal);
                Con.success("Room updated.");
            }
        }
    }

    /**
     * Confirms an edit that may cascade to multiple class instances.
     * Shows a cascade warning when affected > 1.
     */
    private boolean confirmEdit(String fieldName, String oldVal, String newVal,
                                 int affected, String scope) {
        Con.blankLine();
        if (affected > 1) {
            Con.warn("This change affects the " + scope + " record shared by "
                    + Con.b(String.valueOf(affected)) + " class instance(s).");
            Con.warn("ALL of those classes will reflect the new " + fieldName + ".");
        }
        System.out.println("  " + Con.YEL + Con.BD + "  Change  " + Con.R
                + Con.b(fieldName) + "  from  " + Con.r("\"" + oldVal + "\"")
                + "  to  " + Con.g("\"" + newVal + "\"") + " ?");
        String answer = Con.prompt(sc, "Confirm (yes / no)");
        return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
    }

    /** Confirms a simple edit (no cascade). */
    private boolean confirmEditSimple(String fieldName, String oldVal, String newVal) {
        Con.blankLine();
        System.out.println("  " + Con.YEL + Con.BD + "  Change  " + Con.R
                + Con.b(fieldName) + "  from  " + Con.r("\"" + oldVal + "\"")
                + "  to  " + Con.g("\"" + newVal + "\"") + " ?");
        String answer = Con.prompt(sc, "Confirm (yes / no)");
        return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
    }

    // -------------------------------------------------------------------------
    // Delete class
    // -------------------------------------------------------------------------

    private void deleteClassMenu() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        if (classes.isEmpty()) { Con.warn("No class data found. Import data first."); return; }

        Con.header("DELETE A CLASS");
        printBrowseTable(classes);
        Con.blankLine();
        String input = Con.prompt(sc, "Enter class number to delete (or 0 to cancel)");
        if (input.equals("0") || input.isBlank()) return;

        int idx = parseIdx(input, classes.size());
        if (idx < 0) return;
        deleteClass(classes.get(idx));
    }

    private void deleteClass(ClassRecord cr) throws Exception {
        printClassDetail(cr);

        Con.blankLine();
        Con.warn("WARNING: You are about to permanently delete the following class:");
        Con.blankLine();
        System.out.println("  " + Con.b("  Topic     ")  + "  " + cr.topicCode + "  –  " + cr.topicName);
        System.out.println("  " + Con.b("  Class     ")  + "  " + cr.classType + ", Instance " + cr.instanceNumber);
        System.out.println("  " + Con.b("  Campus    ")  + "  " + cr.campus);
        System.out.println("  " + Con.b("  Semester  ")  + "  " + cr.semester + ", Availability " + cr.offeringGroup);
        System.out.println("  " + Con.b("  Sessions  ")  + "  " + cr.sessions.size() + " session(s) will also be deleted.");
        Con.blankLine();
        Con.warn("This action cannot be undone.");
        Con.blankLine();

        String answer = Con.prompt(sc, "Type  yes  to confirm deletion, or press Enter to cancel");
        if (!answer.equalsIgnoreCase("yes")) {
            Con.info("Deletion cancelled.");
            return;
        }

        db.deleteClassInstance(cr.classInstanceId);
        Con.blankLine();
        Con.success("Deleted: " + cr.classType + " #" + cr.instanceNumber
                + " for " + cr.topicCode + " (" + cr.sessions.size() + " session(s) removed).");
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private int parseIdx(String input, int size) {
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx < 0 || idx >= size) { Con.error("Number out of range."); return -1; }
            return idx;
        } catch (NumberFormatException e) {
            Con.error("Invalid number.");
            return -1;
        }
    }

    private static void field(String label, String value) {
        System.out.println("  " + Con.b(Con.pad(label, 20)) + "  " + (value != null ? value : Con.dim("—")));
    }

    private static String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static int tryInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
