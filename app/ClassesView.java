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
            Config.header("CLASSES VIEW");
            Config.menuItem("1", "Browse all classes");
            Config.menuItem("2", "View individual class");
            Config.menuItem("3", "Edit a class");
            Config.menuItem("4", "Delete a class");
            Config.menuItem("0", "Back to main menu");
            String choice = Config.menuPrompt(sc);
            switch (choice) {
                case "1" -> browseAll();
                case "2" -> viewIndividual();
                case "3" -> editClassMenu();
                case "4" -> deleteClassMenu();
                case "0" -> { return; }
                default  -> Config.warn("Unknown option – please try again.");
            }
        }
    }

    public void showSearchMode() throws Exception {
        searchClasses();
    }

    // -------------------------------------------------------------------------
    // Browse all classes
    // -------------------------------------------------------------------------

    private void browseAll() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        Config.header("BROWSE ALL CLASSES");
        if (classes.isEmpty()) { Config.warn("No class data found. Import data first."); return; }
        printBrowseTable(classes);
        Config.blankLine();
        Config.info("Showing " + classes.size() + " class instances across " +
                 classes.stream().map(c -> c.topicCode).distinct().count() + " topics.");
    }

    private void printBrowseTable(List<ClassRecord> classes) {
        Config.divider();
        System.out.println("  " + Config.b(Config.pad("#",         4))
                + Config.b(Config.pad("Topic",      11))
                + Config.b(Config.pad("Class type", 16))
                + Config.b(Config.pad("Inst",        5))
                + Config.b(Config.pad("Campus",     22))
                + Config.b(Config.pad("Sem",         5))
                + Config.b(Config.pad("Avail",       6))
                + Config.b(Config.pad("Day",        13))
                + Config.b(Config.pad("Time",       13))
                + Config.b("Date range"));
        Config.divider();

        for (int i = 0; i < classes.size(); i++) {
            ClassRecord   cr  = classes.get(i);
            SessionRecord p   = cr.primarySession();
            String day   = p != null ? p.day                     : "—";
            String time  = p != null ? p.timeStart + "–" + p.timeEnd : "—";
            String dates = cr.firstDate + "  →  " + cr.lastDate;

            System.out.println("  "
                    + Config.dim(Config.pad(String.valueOf(i + 1), 4))
                    + Config.c(Config.pad(cr.topicCode, 11))
                    + Config.pad(cr.classType, 16)
                    + Config.lpad(String.valueOf(cr.instanceNumber), 3) + "  "
                    + Config.pad(cr.campus, 22)
                    + Config.pad(cr.semester, 5)
                    + Config.lpad(String.valueOf(cr.offeringGroup), 4) + "  "
                    + Config.pad(day, 13)
                    + Config.pad(time, 13)
                    + Config.dim(dates));
        }
        Config.divider();
    }

    // -------------------------------------------------------------------------
    // View individual class
    // -------------------------------------------------------------------------

    private void viewIndividual() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        if (classes.isEmpty()) { Config.warn("No class data found. Import data first."); return; }

        Config.header("VIEW INDIVIDUAL CLASS");
        printBrowseTable(classes);

        Config.blankLine();
        String input = Config.prompt(sc, "Enter class number to view (or 0 to cancel)");
        if (input.equals("0") || input.isBlank()) return;

        int idx = parseIdx(input, classes.size());
        if (idx < 0) return;
        printClassDetail(classes.get(idx));
    }

    void printClassDetail(ClassRecord cr) {
        Config.blankLine();
        String title = cr.topicCode + "  ·  " + cr.topicName;
        String sub   = cr.classType + ", Instance " + cr.instanceNumber;
        System.out.println(Config.CYN + Config.BD + "  ┌" + "─".repeat(Config.W - 6) + "┐" + Config.R);
        System.out.println(Config.CYN + Config.BD + "  │  " + Config.b(title)
                + " ".repeat(Math.max(0, Config.W - 6 - title.length() - 2)) + Config.CYN + "  │" + Config.R);
        System.out.println(Config.CYN + Config.BD + "  │  " + Config.DM + sub
                + " ".repeat(Math.max(0, Config.W - 6 - sub.length() - 2))   + Config.CYN + "  │" + Config.R);
        System.out.println(Config.CYN + Config.BD + "  └" + "─".repeat(Config.W - 6) + "┘" + Config.R);
        Config.blankLine();

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

        Config.blankLine();
        Config.subheader("All scheduled sessions");
        Config.divider();
        for (SessionRecord s : cr.sessions) {
            String tag = s.isRegular()  ? Config.g(" weekly   ")
                       : s.isOnceOnly() ? Config.r(" once-only")
                       :                  Config.y(" " + Config.pad(s.dayModifier, 9));
            System.out.println("  " + tag
                    + "  " + Config.b(Config.pad(s.dayDisplay(), 24))
                    + "  " + Config.c(s.timeStart + " – " + s.timeEnd)
                    + "  " + Config.pad(s.dateStart + "  →  " + s.dateEnd, 28)
                    + "  " + Config.dim(s.location));
        }
        Config.divider();
        Config.blankLine();
    }

    // -------------------------------------------------------------------------
    // Search classes
    // -------------------------------------------------------------------------

    private void searchClasses() throws Exception {
        Config.header("SEARCH CLASSES");
        Config.println("Enter search criteria (leave blank to skip a field).");
        Config.blankLine();

        SearchCriteria c = new SearchCriteria();
        c.topicCode      = TextUtils.nullIfBlank(Config.prompt(sc, "Topic code        (e.g. COMP1002)"));
        c.topicName      = TextUtils.nullIfBlank(Config.prompt(sc, "Topic name        (partial match)"));
        c.mode           = TextUtils.nullIfBlank(Config.prompt(sc, "Attendance mode   (e.g. In person)"));
        c.campus         = TextUtils.nullIfBlank(Config.prompt(sc, "Campus            (e.g. Bedford Park)"));
        c.semester       = TextUtils.nullIfBlank(Config.prompt(sc, "Semester          (S1 / S2)"));
        String ag        =             Config.prompt(sc, "Availability no.  (e.g. 1)");
        c.offeringGroup  = ag.isBlank() ? 0 : TextUtils.parseIntOrZero(ag);
        c.classType      = TextUtils.nullIfBlank(Config.prompt(sc, "Class type        (e.g. Lecture)"));
        String in        =             Config.prompt(sc, "Instance number   (e.g. 2)");
        c.instanceNumber = in.isBlank() ? 0 : TextUtils.parseIntOrZero(in);
        c.firstDate      = TextUtils.nullIfBlank(Config.prompt(sc, "Date of first class (e.g. 03 Mar)"));
        c.lastDate       = TextUtils.nullIfBlank(Config.prompt(sc, "Date of last class  (e.g. 10 Jun)"));
        c.day            = TextUtils.nullIfBlank(Config.prompt(sc, "Day               (e.g. Monday)"));
        c.timeStart      = TextUtils.nullIfBlank(Config.prompt(sc, "Start time        (e.g. 09:00)"));
        c.timeEnd        = TextUtils.nullIfBlank(Config.prompt(sc, "End time          (e.g. 11:00)"));
        c.building       = TextUtils.nullIfBlank(Config.prompt(sc, "Building          (partial match)"));
        c.room           = TextUtils.nullIfBlank(Config.prompt(sc, "Room              (partial match)"));

        List<ClassRecord> result = db.loadAllClasses().stream()
                .filter(cr -> cr.matchesSearch(c))
                .collect(Collectors.toList());

        Config.header("SEARCH RESULTS");
        if (result.isEmpty()) {
            Config.warn("No classes matched the search criteria.");
        } else {
            printBrowseTable(result);
            Config.blankLine();
            Config.info("Found " + result.size() + " matching class instance(s).");
            Config.blankLine();
            String pick = Config.prompt(sc, "View a class? Enter list number (or 0 to skip)");
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
        if (classes.isEmpty()) { Config.warn("No class data found. Import data first."); return; }

        Config.header("EDIT A CLASS");
        printBrowseTable(classes);
        Config.blankLine();
        String input = Config.prompt(sc, "Enter class number to edit (or 0 to cancel)");
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

            Config.header("EDIT CLASS  ·  " + current.shortLabel());
            Config.println(Config.dim("Note: fields [1–2] update all classes sharing this topic."));
            Config.println(Config.dim("      fields [3–6] update all classes sharing this offering."));
            Config.println(Config.dim("      fields [11–15] update all sessions of this class."));
            Config.blankLine();

            printEditMenu(current, pDay, pStart, pEnd, pBuilding, pRoom);

            String choice = Config.menuPrompt(sc);
            if (choice.equals("0")) return;

            int field;
            try { field = Integer.parseInt(choice); }
            catch (NumberFormatException e) { Config.warn("Invalid option."); continue; }
            if (field < 1 || field > 15) { Config.warn("Invalid option."); continue; }

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
        Config.menuItem("0", "Done / cancel");
    }

    private static void printEditRow(int n, String label, String value, boolean unused) {
        System.out.printf("    %s%-4s%s  %-22s  %s%n",
                Config.CYN + Config.BD, "[" + n + "]", Config.R,
                label,
                value != null ? value : Config.dim("—"));
    }

    private void applyEdit(ClassRecord cr, int field, String currentBuilding, String currentRoom)
            throws Exception {
        String newVal;

        switch (field) {
            // ---- Topic-level ------------------------------------------------
            case 1 -> {
                int affected = db.countClassesForTopic(cr.topicId);
                newVal = Config.prompt(sc, "New topic code (current: " + cr.topicCode + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("topic code", cr.topicCode, newVal, affected, "topic")) return;
                try {
                    db.updateTopicCode(cr.topicId, newVal);
                    Config.success("Topic code updated.");
                } catch (Exception e) {
                    Config.error("Could not update: " + e.getMessage());
                }
            }
            case 2 -> {
                int affected = db.countClassesForTopic(cr.topicId);
                newVal = Config.prompt(sc, "New topic name (current: " + cr.topicName + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("topic name", cr.topicName, newVal, affected, "topic")) return;
                db.updateTopicName(cr.topicId, newVal);
                Config.success("Topic name updated.");
            }

            // ---- Offering-level ---------------------------------------------
            case 3 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Config.prompt(sc, "New attendance mode (current: " + cr.mode + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("attendance mode", cr.mode, newVal, affected, "offering")) return;
                db.updateOfferingMode(cr.offeringId, newVal);
                Config.success("Attendance mode updated.");
            }
            case 4 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Config.prompt(sc, "New campus (current: " + cr.campus + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("campus", cr.campus, newVal, affected, "offering")) return;
                db.updateOfferingCampus(cr.offeringId, newVal);
                Config.success("Campus updated.");
            }
            case 5 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Config.prompt(sc, "New semester (current: " + cr.semester + ", e.g. S1 / S2)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("semester", cr.semester, newVal, affected, "offering")) return;
                try {
                    db.updateOfferingSemester(cr.offeringId, newVal);
                    Config.success("Semester updated.");
                } catch (Exception e) {
                    Config.error("Could not update: " + e.getMessage());
                }
            }
            case 6 -> {
                int affected = db.countClassesForOffering(cr.offeringId);
                newVal = Config.prompt(sc, "New availability no. (current: " + cr.offeringGroup + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                int newInt = TextUtils.parseIntOrZero(newVal);
                if (newInt <= 0) { Config.error("Must be a positive integer."); return; }
                if (!confirmEdit("availability no.", String.valueOf(cr.offeringGroup),
                                 String.valueOf(newInt), affected, "offering")) return;
                try {
                    db.updateOfferingGroup(cr.offeringId, newInt);
                    Config.success("Availability no. updated.");
                } catch (Exception e) {
                    Config.error("Could not update: " + e.getMessage());
                }
            }

            // ---- Class-instance level ----------------------------------------
            case 7 -> {
                newVal = Config.prompt(sc, "New class type (current: " + cr.classType + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEdit("class type", cr.classType, newVal, 1, "class")) return;
                db.updateClassType(cr.classInstanceId, newVal);
                Config.success("Class type updated.");
            }
            case 8 -> {
                newVal = Config.prompt(sc, "New instance no. (current: " + cr.instanceNumber + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                int newInt = TextUtils.parseIntOrZero(newVal);
                if (newInt <= 0) { Config.error("Must be a positive integer."); return; }
                if (!confirmEdit("instance no.", String.valueOf(cr.instanceNumber),
                                 String.valueOf(newInt), 1, "class")) return;
                db.updateInstanceNumber(cr.classInstanceId, newInt);
                Config.success("Instance no. updated.");
            }

            // ---- Session level -----------------------------------------------
            case 9 -> {
                newVal = Config.prompt(sc, "New date of first class (current: " + cr.firstDate
                                      + ", e.g. 03 Mar)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("date of first class", cr.firstDate, newVal)) return;
                db.updateFirstDate(cr.classInstanceId, newVal);
                Config.success("Date of first class updated.");
            }
            case 10 -> {
                newVal = Config.prompt(sc, "New date of last class (current: " + cr.lastDate
                                      + ", e.g. 10 Jun)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("date of last class", cr.lastDate, newVal)) return;
                db.updateLastDate(cr.classInstanceId, newVal);
                Config.success("Date of last class updated.");
            }
            case 11 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.day : "—";
                newVal = Config.prompt(sc, "New day (current: " + cur + ", e.g. Monday)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                int n = cr.sessions.size();
                if (!confirmEditSimple("day", cur, newVal + "  (applies to all " + n + " session(s))")) return;
                db.updateAllSessionDays(cr.classInstanceId, newVal);
                Config.success("Day updated across all sessions.");
            }
            case 12 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.timeStart : "—";
                newVal = Config.prompt(sc, "New start time (current: " + cur + ", e.g. 14:00)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("start time", cur, newVal)) return;
                db.updateAllSessionTimeStart(cr.classInstanceId, newVal);
                Config.success("Start time updated.");
            }
            case 13 -> {
                SessionRecord p = cr.primarySession();
                String cur = p != null ? p.timeEnd : "—";
                newVal = Config.prompt(sc, "New end time (current: " + cur + ", e.g. 16:00)");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("end time", cur, newVal)) return;
                db.updateAllSessionTimeEnd(cr.classInstanceId, newVal);
                Config.success("End time updated.");
            }
            case 14 -> {
                newVal = Config.prompt(sc, "New building (current: " + currentBuilding + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("building", currentBuilding, newVal)) return;
                db.updateAllSessionBuilding(cr.classInstanceId, newVal);
                Config.success("Building updated.");
            }
            case 15 -> {
                newVal = Config.prompt(sc, "New room (current: " + currentRoom + ")");
                if (newVal.isBlank()) { Config.warn("No change."); return; }
                if (!confirmEditSimple("room", currentRoom, newVal)) return;
                db.updateAllSessionRoom(cr.classInstanceId, newVal);
                Config.success("Room updated.");
            }
        }
    }

    /**
     * Confirms an edit that may cascade to multiple class instances.
     * Shows a cascade warning when affected > 1.
     */
    private boolean confirmEdit(String fieldName, String oldVal, String newVal,
                                 int affected, String scope) {
        Config.blankLine();
        if (affected > 1) {
            Config.warn("This change affects the " + scope + " record shared by "
                    + Config.b(String.valueOf(affected)) + " class instance(s).");
            Config.warn("ALL of those classes will reflect the new " + fieldName + ".");
        }
        System.out.println("  " + Config.YEL + Config.BD + "  Change  " + Config.R
                + Config.b(fieldName) + "  from  " + Config.r("\"" + oldVal + "\"")
                + "  to  " + Config.g("\"" + newVal + "\"") + " ?");
        String answer = Config.prompt(sc, "Confirm (yes / no)");
        return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
    }

    /** Confirms a simple edit (no cascade). */
    private boolean confirmEditSimple(String fieldName, String oldVal, String newVal) {
        Config.blankLine();
        System.out.println("  " + Config.YEL + Config.BD + "  Change  " + Config.R
                + Config.b(fieldName) + "  from  " + Config.r("\"" + oldVal + "\"")
                + "  to  " + Config.g("\"" + newVal + "\"") + " ?");
        String answer = Config.prompt(sc, "Confirm (yes / no)");
        return answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y");
    }

    // -------------------------------------------------------------------------
    // Delete class
    // -------------------------------------------------------------------------

    private void deleteClassMenu() throws Exception {
        List<ClassRecord> classes = db.loadAllClasses();
        if (classes.isEmpty()) { Config.warn("No class data found. Import data first."); return; }

        Config.header("DELETE A CLASS");
        printBrowseTable(classes);
        Config.blankLine();
        String input = Config.prompt(sc, "Enter class number to delete (or 0 to cancel)");
        if (input.equals("0") || input.isBlank()) return;

        int idx = parseIdx(input, classes.size());
        if (idx < 0) return;
        deleteClass(classes.get(idx));
    }

    private void deleteClass(ClassRecord cr) throws Exception {
        printClassDetail(cr);

        Config.blankLine();
        Config.warn("WARNING: You are about to permanently delete the following class:");
        Config.blankLine();
        System.out.println("  " + Config.b("  Topic     ")  + "  " + cr.topicCode + "  –  " + cr.topicName);
        System.out.println("  " + Config.b("  Class     ")  + "  " + cr.classType + ", Instance " + cr.instanceNumber);
        System.out.println("  " + Config.b("  Campus    ")  + "  " + cr.campus);
        System.out.println("  " + Config.b("  Semester  ")  + "  " + cr.semester + ", Availability " + cr.offeringGroup);
        System.out.println("  " + Config.b("  Sessions  ")  + "  " + cr.sessions.size() + " session(s) will also be deleted.");
        Config.blankLine();
        Config.warn("This action cannot be undone.");
        Config.blankLine();

        String answer = Config.prompt(sc, "Type  yes  to confirm deletion, or press Enter to cancel");
        if (!answer.equalsIgnoreCase("yes")) {
            Config.info("Deletion cancelled.");
            return;
        }

        db.deleteClassInstance(cr.classInstanceId);
        Config.blankLine();
        Config.success("Deleted: " + cr.classType + " #" + cr.instanceNumber
                + " for " + cr.topicCode + " (" + cr.sessions.size() + " session(s) removed).");
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private int parseIdx(String input, int size) {
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx < 0 || idx >= size) { Config.error("Number out of range."); return -1; }
            return idx;
        } catch (NumberFormatException e) {
            Config.error("Invalid number.");
            return -1;
        }
    }

    private static void field(String label, String value) {
        System.out.println("  " + Config.b(Config.pad(label, 20)) + "  " + (value != null ? value : Config.dim("—")));
    }

}
