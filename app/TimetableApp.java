import java.util.Scanner;

/**
 * Timetable Application – main entry point.
 *
 * Usage: java -cp ".:lib/*" TimetableApp [path/to/timetable.db]
 */
public class TimetableApp {

    private static final String DEFAULT_DB = "../data-loader/timetable.db";

    public static void main(String[] args) throws Exception {
        String dbPath = args.length > 0 ? args[0] : DEFAULT_DB;

        Scanner sc = new Scanner(System.in);
        Config.blankLine();
        Config.banner();
        Config.blankLine();

        Database db;
        try {
            db = new Database(dbPath);
        } catch (Exception e) {
            Config.error("Could not open database: " + dbPath);
            Config.error(e.getMessage());
            Config.blankLine();
            Config.info("Run the data loader first:  cd ../data-loader && ./load.sh");
            return;
        }

        if (!db.hasData()) {
            Config.warn("Database is empty. Run the data loader first:");
            Config.info("  cd ../data-loader && ./load.sh");
            Config.blankLine();
        }

        ClassesView classesView = new ClassesView(db, sc);
        TimetableMode timetableMode = new TimetableMode(db, sc);

        mainLoop:
        while (true) {
            Config.header("MAIN MENU");
            Config.menuItem("1", "Import Mode        " + Config.dim("(load CSV data)"));
            Config.menuItem("2", "Classes View       " + Config.dim("(browse, view, edit, delete)"));
            Config.menuItem("3", "Search Mode        " + Config.dim("(search classes)"));
            Config.menuItem("4", "Timetable Mode     " + Config.dim("(generate, view, edit, export, delete timetables)"));
            Config.menuItem("0", "Exit");

            String choice = Config.menuPrompt(sc);
            switch (choice) {
                case "1" -> importMode(dbPath);
                case "2" -> classesView.show();
                case "3" -> classesView.showSearchMode();
                case "4" -> timetableMode.show();
                case "0" -> { break mainLoop; }
                default  -> Config.warn("Unknown option – please try again.");
            }
        }

        db.close();
        Config.blankLine();
        Config.println(Config.b("Goodbye."));
        Config.blankLine();
    }

    static void importMode(String dbPath) {
        Config.header("IMPORT MODE");
        Config.println("To import CSV data, run the data loader from the command line:");
        Config.blankLine();
        Config.println(Config.c("  cd ../data-loader && ./load.sh [csv-dir] [db-file]"));
        Config.blankLine();
        Config.info("Default CSV directory : ../Spec and CSVs/CSV");
        Config.info("Default database file : " + dbPath);
        Config.blankLine();
    }
}
