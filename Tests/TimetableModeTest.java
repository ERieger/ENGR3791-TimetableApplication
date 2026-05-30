import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TimetableModeTest {

    private final InputStream originalInputStream = System.in;
    private final PrintStream originalOutputStream = System.out;
    private final ByteArrayOutputStream captureOutputStream = new ByteArrayOutputStream();

    @TempDir
    static Path sharedTempDir;

    @BeforeAll
    static void setUpDb() throws Exception {

        Path dbPath = Paths.get("data-loader", "timetable.db");

        Files.deleteIfExists(dbPath);
        Files.deleteIfExists(Paths.get("data-loader", "timetable.db-shm"));
        Files.deleteIfExists(Paths.get("data-loader", "timetable.db-wal"));

        CsvToSqliteLoader.main(new String[]{
                "./Spec and CSVs/CSV",
                dbPath.toString()
        });

        System.out.println("Database setup complete at: " + dbPath.toAbsolutePath());
    }

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(captureOutputStream));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalInputStream);
        System.setOut(originalOutputStream);
    }

    @DisplayName("1.09 Timetable Mode Input (Correct)")
    @Tag("Aidan")
    @Tag("Critical")
    @Test
    void timetableModeInput() throws Exception {

    }

    @DisplayName("1.10 Timetable Mode Input (Incorrect)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void timetableModeIncorrect() throws Exception {

    }

    @DisplayName("1.11 Timetable Mode Input (MAX and MIN Ints)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void timetableModeInputMaxAndMinInts() throws Exception {

    }

    @DisplayName("1.12 Timetable Mode Input (Passing Nulls)")
    @Tag("Aidan")
    @Tag("Additional")
    @Test
    void timetableModeInputPassingNulls() throws Exception {

    }

    @DisplayName(" 5.01 Generating Timetable with no Class data Warning")
    @Tag("Jayden")
    @Tag("Additional")
    @Test
    void generatingTimetableWithNoData() throws Exception {
        String input = "4\n" + "1\n" +"0\n" + "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{":memory:"});
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("TIMETABLE MODE")),
                () -> assertTrue(captureOutputStream.toString().contains("No class data found. Import data first."))
        );
    }

    @DisplayName("5.02 Special Characters in Timetable name")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void timetableModeInputSpecialCharacters() throws Exception {
        String input = "4\n" + "1\n" +"!@#$%^&*()\n" +"\n" + "1\n"+ "\n".repeat(3) + "0\n".repeat(3);
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("GENERATE TIMETABLE")),
                () -> assertTrue(captureOutputStream.toString().contains("!@#$%^&*()"))
        );
    }

    @DisplayName("5.03 Delete mode cancels correctly after prompt")
    @Tag("Elijah")
    @Tag("Critical")
    @Test
    void deleteModeCancel() throws Exception {
        String input =  "4\n" +                          // Main menu: Timetable Mode
                        "1\n" +                          // Generate timetable
                        "testTimetable1\n" +             // Timetable name
                        "\n" +                           // Default semester/availability
                        "COMP1002, COMP1102, COMP1103\n" + // Choose topics
                        "\n" +                           // All campuses
                        "yes\n" +                        // Allow lecture overlap
                        "\n" +                           // Default preferences
                        "4\n" +                          // Delete mode
                        "0\n" +                          // Cancel / back
                        "0\n" +                          // Exit timetable mode
                        "0\n";                           // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("DELETE GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Deletion cancelled")),
                () -> assertTrue(output.contains("Goodbye."))
        );
    }

    @DisplayName("5.04 Delete mode deletes correctly after prompt")
    @Tag("Elijah")
    @Tag("Critical")
    @Test
    void deleteModeDelete() throws Exception {
        String input =  "4\n" + // Enter timetable mode
                        "1\n" + // Create a timetable
                        "testTimetable1\n" + // Name timetable
                        "\n" + // Default semester
                        "COMP1002, COMP1102, COMP1103\n" + // Select topics
                        "\n" + // Default campus
                        "yes\n" + // Allow lecture overlap
                        "\n" + // Default preference order
                        "4\n" + // Delete mode
                        "1\n" + // Action delete
                        "Yes\n" + // Confirm delete
                        "2\n" + // Validate delete
                        "0\n" + // Exit timetable mode
                        "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("DELETE GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("You are about to permanently delete this generated timetable:")),
                () -> assertTrue(output.contains("Deleted timetable: testTimetable1")),
                () -> assertTrue(output.contains("No generated timetables in this session yet."))
        );
    }

    @DisplayName("5.05 Export Incorrect File Path")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportIncorrectFile() throws Exception {
        Random randGen = new Random();
        int randInt = randGen.nextInt(100);
        Path outPath = sharedTempDir.resolve(randInt + "/Timetable.csv");

        String input =
                "4\n" + // Enter timetable mode
                "1\n" + // Create a timetable
                "testTimetable1\n" + // Name timetable
                "\n" + // Default semester
                "COMP1002, COMP1102, COMP1103\n" + // Select topics
                "\n" + // Default campus
                "yes\n" + // Allow lecture overlap
                "\n" + // Default preference order
                "5\n" + // Export mode
                "1\n" + // Select fist (and only) timetable
                "1\n" + // CSV export
                outPath.toString() + "\n" + // Non-existent file path
                "0\n" + // Exit timetable mode
                "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("EXPORT GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Output file path")),
                () -> assertTrue(output.contains("Exported timetable to:")),
                () -> assertTrue(output.contains("\\" + randInt + "\\Timetable.csv")),
                () -> assertTrue(Files.exists(outPath))
        );
        Files.deleteIfExists(outPath);
        Files.deleteIfExists(outPath.getParent());
    }

    @DisplayName("5.06 Export Null File Path")
    @Tag("Elijah")
    @Tag("Additional")
    @Test
    void exportNullFile() throws Exception {
        Path outPath = Path.of("../Exported Timetables", "testTimetable1.csv");
        ;

        String input =  "4\n" + // Enter timetable mode
                        "1\n" + // Create a timetable
                        "testTimetable1\n" + // Name timetable
                        "\n" + // Default semester
                        "COMP1002, COMP1102, COMP1103\n" + // Select topics
                        "\n" + // Default campus
                        "yes\n" + // Allow lecture overlap
                        "\n" + // Default preference order
                        "5\n" + // Export mode
                        "1\n" + // Select fist (and only) timetable
                        "1\n" + // CSV export
                        "\n"+ // Null file path
                        "0\n" + // Exit timetable mode
                        "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("EXPORT GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Output file path")),
                () -> assertTrue(output.contains("Exported timetable to:")),
                () -> assertTrue(output.contains("\\Exported Timetables\\testTimetable1.csv")),
                () -> assertTrue(Files.exists(outPath))
        );
        Files.deleteIfExists(outPath);
        Files.deleteIfExists(outPath.getParent());
    }

    @DisplayName("5.07 Export Possible File Path")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportPossibleFile() throws Exception {
        // Potential logical duplicate of 5.05 please see gantt chart comments.
    }

    @DisplayName("5.08 Exporting in TSV")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void exportTSV() throws Exception {
        String userHome = System.getProperty("user.home");
        Path outPath = Path.of("../Exported Timetables/testTimetable1.tsv");

        String input =  "4\n" + // Enter timetable mode
                "1\n" + // Create a timetable
                "testTimetable1\n" + // Name timetable
                "\n" + // Default semester
                "COMP1002, COMP1102, COMP1103\n" + // Select topics
                "\n" + // Default campus
                "yes\n" + // Allow lecture overlap
                "\n" + // Default preference order
                "5\n" + // Export mode
                "1\n" + // Select fist (and only) timetable
                "2\n" + // TSV export
                "\n"+ // Null file path
                "0\n" + // Exit timetable mode
                "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();


        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("EXPORT GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Output file path")),
                () -> assertTrue(output.contains("Exported timetable to:")),
                () -> assertTrue(output.contains("\\Exported Timetables" + "\\testTimetable1.tsv")),
                () -> assertTrue(Files.exists(outPath))
        );

        Files.deleteIfExists(outPath);
        Files.deleteIfExists(outPath.getParent());
    }

    @DisplayName("5.09 Exporting in JSON")
    @Tag("Elijah")
    @Tag("Additional")
    @Test
    void exportJSON() throws Exception  {
        String userHome = System.getProperty("user.home");
        Path outPath = Path.of("../Exported Timetables/testTimetable1.json");

        String input =  "4\n" + // Enter timetable mode
                "1\n" + // Create a timetable
                "testTimetable1\n" + // Name timetable
                "\n" + // Default semester
                "COMP1002, COMP1102, COMP1103\n" + // Select topics
                "\n" + // Default campus
                "yes\n" + // Allow lecture overlap
                "\n" + // Default preference order
                "5\n" + // Export mode
                "1\n" + // Select fist (and only) timetable
                "3\n" + // JSON export
                "\n"+ // Null file path
                "0\n" + // Exit timetable mode
                "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("EXPORT GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Output file path")),
                () -> assertTrue(output.contains("Exported timetable to:")),
                () -> assertTrue(output.contains("\\Exported Timetables\\testTimetable1.json")),
                () -> assertTrue(Files.exists(outPath))
        );

        Files.deleteIfExists(outPath);
        Files.deleteIfExists(outPath.getParent());
    }

    // Logical duplicate of other other export tests which use csv for testing.
    @DisplayName("5.10 Exporting in CSV")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportCSV() throws Exception  {
        Path outPath = Path.of("../Exported Timetables/testTimetable1.csv");

        String input =  "4\n" + // Enter timetable mode
                "1\n" + // Create a timetable
                "testTimetable1\n" + // Name timetable
                "\n" + // Default semester
                "COMP1002, COMP1102, COMP1103\n" + // Select topics
                "\n" + // Default campus
                "yes\n" + // Allow lecture overlap
                "\n" + // Default preference order
                "5\n" + // Export mode
                "1\n" + // Select fist (and only) timetable
                "1\n" + // CSV export
                "\n"+ // Null file path
                "0\n" + // Exit timetable mode
                "0\n"; // Exit app

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("Generated timetable")),
                () -> assertTrue(output.contains("EXPORT GENERATED TIMETABLE")),
                () -> assertTrue(output.contains("Output file path")),
                () -> assertTrue(output.contains("Exported timetable to:")),
                () -> assertTrue(output.contains("\\Exported Timetables\\testTimetable1.csv")),
                () -> assertTrue(Files.exists(outPath))
        );

        Files.deleteIfExists(outPath);
        Files.deleteIfExists(outPath.getParent());
    }

    @DisplayName("5.11 Generating Timetable error/help messages")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void generatingModeErrors() throws Exception {
    }








}