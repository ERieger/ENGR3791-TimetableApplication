import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Handles the Browse / View / Search classes screens. */
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
            Con.menuItem("0", "Back to main menu");
            String choice = Con.menuPrompt(sc);
            switch (choice) {
                case "1" -> browseAll();
                case "2" -> viewIndividual();
                case "3" -> searchClasses();
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
        // Header row
        Con.divider();
        System.out.println("  " + Con.b(Con.pad("#",    4))
                + Con.b(Con.pad("Topic",    11))
                + Con.b(Con.pad("Class type",    16))
                + Con.b(Con.pad("Inst", 5))
                + Con.b(Con.pad("Campus",         22))
                + Con.b(Con.pad("Sem", 5))
                + Con.b(Con.pad("Avail", 6))
                + Con.b(Con.pad("Day",            13))
                + Con.b(Con.pad("Time",           13))
                + Con.b("Date range"));
        Con.divider();

        for (int i = 0; i < classes.size(); i++) {
            ClassRecord cr = classes.get(i);
            SessionRecord p = cr.primarySession();
            String day      = p != null ? p.day         : "—";
            String time     = p != null ? p.timeStart + "–" + p.timeEnd : "—";
            String dates    = cr.firstDate + "  →  " + cr.lastDate;

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

        int idx;
        try {
            idx = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            Con.error("Invalid number.");
            return;
        }
        if (idx < 0 || idx >= classes.size()) {
            Con.error("Number out of range.");
            return;
        }
        printClassDetail(classes.get(idx));
    }

    void printClassDetail(ClassRecord cr) {
        Con.blankLine();
        // Title box
        String title = cr.topicCode + "  ·  " + cr.topicName;
        String sub   = cr.classType + ", Instance " + cr.instanceNumber;
        System.out.println(Con.CYN + Con.BD + "  ┌" + "─".repeat(Con.W - 6) + "┐" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  │  " + Con.b(title)
                + " ".repeat(Math.max(0, Con.W - 6 - title.length() - 2)) + Con.CYN + "  │" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  │  " + Con.DM + sub
                + " ".repeat(Math.max(0, Con.W - 6 - sub.length() - 2))   + Con.CYN + "  │" + Con.R);
        System.out.println(Con.CYN + Con.BD + "  └" + "─".repeat(Con.W - 6) + "┘" + Con.R);
        Con.blankLine();

        field("Topic code",       cr.topicCode);
        field("Topic name",       cr.topicName);
        field("Attendance mode",  cr.mode);
        field("Campus",           cr.campus);
        field("Semester",         cr.semester);
        field("Availability no.", String.valueOf(cr.offeringGroup));
        field("Class",            cr.classType);
        field("Instance",         String.valueOf(cr.instanceNumber));
        field("Date of first class", cr.firstDate);
        field("Date of last class",  cr.lastDate);

        // Primary session summary fields
        SessionRecord p = cr.primarySession();
        if (p != null) {
            field("Day",            p.dayDisplay());
            field("Start time",     p.timeStart);
            field("End time",       p.timeEnd);
            field("Building",       p.building);
            field("Room",           p.room);
        }

        // All sessions
        Con.blankLine();
        Con.subheader("All scheduled sessions");
        Con.divider();
        for (SessionRecord s : cr.sessions) {
            String typeTag = s.isRegular()  ? Con.g(" weekly   ")
                           : s.isOnceOnly() ? Con.r(" once-only")
                           :                  Con.y(" " + Con.pad(s.dayModifier, 9));
            System.out.println("  " + typeTag
                    + "  " + Con.b(Con.pad(s.dayDisplay(), 24))
                    + "  " + Con.c(s.timeStart + " – " + s.timeEnd)
                    + "  " + Con.pad(s.dateStart + "  →  " + s.dateEnd, 28)
                    + "  " + Con.dim(s.location));
        }
        Con.divider();
        Con.blankLine();
    }

    private static void field(String label, String value) {
        System.out.println("  " + Con.b(Con.pad(label, 20)) + "  " + (value != null ? value : Con.dim("—")));
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

        List<ClassRecord> all    = db.loadAllClasses();
        List<ClassRecord> result = all.stream()
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
            // Offer to view a result
            String pick = Con.prompt(sc, "View a class? Enter list number (or 0 to skip)");
            if (!pick.equals("0") && !pick.isBlank()) {
                try {
                    int idx = Integer.parseInt(pick) - 1;
                    if (idx >= 0 && idx < result.size()) printClassDetail(result.get(idx));
                    else Con.error("Number out of range.");
                } catch (NumberFormatException e) {
                    Con.error("Invalid number.");
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static int tryInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
