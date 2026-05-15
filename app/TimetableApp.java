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
        Con.blankLine();
        Con.banner();
        Con.blankLine();

        Database db;
        try {
            db = new Database(dbPath);
        } catch (Exception e) {
            Con.error("Could not open database: " + dbPath);
            Con.error(e.getMessage());
            Con.blankLine();
            Con.info("Run the data loader first:  cd ../data-loader && ./load.sh");
            return;
        }

        if (!db.hasData()) {
            Con.warn("Database is empty. Run the data loader first:");
            Con.info("  cd ../data-loader && ./load.sh");
            Con.blankLine();
        }

        ClassesView classesView = new ClassesView(db, sc);

        mainLoop:
        while (true) {
            Con.header("MAIN MENU");
            Con.menuItem("1", "Import Mode        " + Con.dim("(load CSV data)"));
            Con.menuItem("2", "Classes View       " + Con.dim("(browse, view, search)"));
            Con.menuItem("3", "Search Mode        " + Con.dim("(coming soon)"));
            Con.menuItem("4", "Timetable Mode     " + Con.dim("(coming soon)"));
            Con.menuItem("0", "Exit");

            String choice = Con.menuPrompt(sc);
            switch (choice) {
                case "1" -> importMode(dbPath);
                case "2" -> classesView.show();
                case "3" -> Con.warn("Search Mode is not yet implemented.");
                case "4" -> Con.warn("Timetable Mode is not yet implemented.");
                case "0" -> { break mainLoop; }
                default  -> Con.warn("Unknown option – please try again.");
            }
        }

        db.close();
        Con.blankLine();
        Con.println(Con.b("Goodbye."));
        Con.blankLine();
    }

    private static void importMode(String dbPath) {
        Con.header("IMPORT MODE");
        Con.println("To import CSV data, run the data loader from the command line:");
        Con.blankLine();
        Con.println(Con.c("  cd ../data-loader && ./load.sh [csv-dir] [db-file]"));
        Con.blankLine();
        Con.info("Default CSV directory : ../Spec and CSVs/CSV");
        Con.info("Default database file : " + dbPath);
        Con.blankLine();
    }
}
