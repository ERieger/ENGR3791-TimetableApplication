import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class ClassesViewTest {
    private final InputStream originalInputStream = System.in;
    private final PrintStream originalOutputStream = System.out;
    private final ByteArrayOutputStream captureOutputStream = new ByteArrayOutputStream();

    @TempDir
    Path tempDir;

    private Path dbPath;

    @BeforeEach
    void setUpDb() throws Exception {
        dbPath = tempDir.resolve("timetable.db");

        Files.deleteIfExists(dbPath);
        Files.deleteIfExists(tempDir.resolve("timetable.db-wal"));
        Files.deleteIfExists(tempDir.resolve("timetable.db-shm"));

        CsvToSqliteLoader.main(new String[]{"./Spec and CSVs/CSV", dbPath.toString()});
    }

    @AfterEach
    void cleanDb() throws Exception {
        Files.deleteIfExists(dbPath);
        Files.deleteIfExists(tempDir.resolve("timetable.db-wal"));
        Files.deleteIfExists(tempDir.resolve("timetable.db-shm"));
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

    @DisplayName("1.05 Class View Input (Correct) Modes 1 to 4")
    @Tag("Aidan")
    @Tag("Critical")
    @ParameterizedTest(name = "Input {0} should display {1}")
    @CsvSource({"1, BROWSE ALL CLASSES", "2, VIEW INDIVIDUAL CLASS", "3, EDIT A CLASS", "4, DELETE A CLASS"})
    void classViewInputN(int mode, String modeString) throws Exception {
        //Test input
        String input = "2\n" + mode + "\n" + "0\n" + "0\n" + "0\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Timetable App Start with DB
        TimetableApp.main(new String[]{dbPath.toString()});

        //Tests
        assertTrue(captureOutputStream.toString().contains("CLASSES VIEW"));
        assertTrue(captureOutputStream.toString().contains(modeString));

    }

    @DisplayName("1.06 Class View Input (Incorrect)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void classViewInputIncorrect() throws Exception {
        //Test input
        String input = "egg" + System.lineSeparator() + 0;

        //Setup for testing input
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //Setting up input capture and db for test
        Scanner scan = new Scanner(captureInputStream);
        Database db = new Database("timetable.db");

        //Instance of ClassesView obj for testing
        ClassesView cv = new ClassesView(db, scan);
        cv.show();

        //Tests
        assertTrue(captureOutputStream.toString().contains("Unknown option – please try again."));
    }

    @DisplayName("1.07 Class View Input (MAX and MIN Ints)")
    @Tag("Aidan")
    @Tag("Core")
    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    void classViewInputMaxAndMinInts(int mode) throws Exception {
        //Test input
        String input = mode + System.lineSeparator() + 0;

        //Setup for testing input
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //Setting up input capture and db for test
        Scanner scan = new Scanner(captureInputStream);
        Database db = new Database("timetable.db");

        //Instance of ClassesView obj for testing
        ClassesView cv = new ClassesView(db, scan);
        cv.show();

        //Tests
        assertTrue(captureOutputStream.toString().contains("Unknown option – please try again."));
    }

    @DisplayName("1.08 Class View Input (Passing Null and Empty)")
    @Tag("Aidan")
    @Tag("Additional")
    @ParameterizedTest
    @NullAndEmptySource
    void classViewInputPassingNulls(String mode) throws Exception {
        //Test input
        String input = mode +
                System.lineSeparator() +
                0;

        //Setup for testing input
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //Setting up input capture and db for test
        Scanner scan = new Scanner(captureInputStream);
        Database db = new Database("timetable.db");

        //Instance of ClassesView obj for testing
        ClassesView cv = new ClassesView(db, scan);
        cv.show();

        //Tests
        assertTrue(captureOutputStream.toString().contains("Unknown option – please try again."));
    }

    @DisplayName("2.01 Invalid input shows warning")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void invalidInputShowsWarning() throws Exception {
        // Test Input
        String input = "10\n" + "0\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Setting up Class view instance
        ClassesView testView = new ClassesView(null, new Scanner(System.in)); //I tried putting in "data-loader/timetable.db" but it was upset with that
        testView.show();

        // Test
        assertTrue(captureOutputStream.toString().contains("Unknown option – please try again"));
    }
    //could also add in extra tests for edge cases (max/min inputs) if we get time! So its more like the pracs :)

    @DisplayName("2.02 viewing individual classes rejects incorrect inputs")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassIncorrectValues() throws Exception {
        //Test Input
        String input = "2\n" +
                        "2\n" +
                        "notaclass\n" +
                        "0\n" +
                        "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        //Timetable App Start
        TimetableApp.main(new String[]{dbPath.toString()});

        //Tests
        assertTrue(captureOutputStream.toString().contains("Invalid number"));
    }

    @DisplayName("2.03 Browse all shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void browseAllNoDataWarning() throws Exception {
        //Test Input
        String input = "2\n" +
                        "1\n" +
                        "0\n" +
                        "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        //Timetable App start with no DB
        TimetableApp.main(new String[]{":memory:"});

        //Tests
        assertTrue(captureOutputStream.toString().contains("No class data found"));
    }

    @DisplayName("2.04 viewing individual classes shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassViewNoDataWarning() throws Exception {
        //Test Input
        String input = "2\n" +
                        "2\n" +
                        "0\n" +
                        "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        //Timetable App start with no DB
        TimetableApp.main(new String[]{":memory:"});

        //Tests
        assertTrue(captureOutputStream.toString().contains("No class data found"));
    }

    @DisplayName("2.05 Edit class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void editClassNoDataWarning() throws Exception {
        //Test input
        String input = "2\n" + "3\n" + "0\n" + "0\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        //Timetable start with no DB
        TimetableApp.main(new String[]{":memory:"});

        //Tests
        assertTrue(captureOutputStream.toString().contains("No class data found"));
    }

    @DisplayName("2.06 Delete class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void deleteClassNoDataWarning() throws Exception {
        //Test inputs
        String input = "2\n" +
                        "4\n" +
                        "0\n" +
                        "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        //Timetable start with no DB
        TimetableApp.main(new String[]{":memory:"});

        //Tests
        assertTrue(captureOutputStream.toString().contains("No class data found"));
    }

    @DisplayName("4.01 Blank criteria shows all classes")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void blankCriteriaShowsAllClasses() throws Exception {
        //Test Input
        String input = "3\n" +
                "\n".repeat(16) +
                "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));

        //Timetable start with DB
        TimetableApp.main(new String[]{dbPath.toString()});

        //Tests
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1002")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1102")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1103")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1701")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1702")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1711")),
                () -> assertTrue(captureOutputStream.toString().contains("ENGR1401")),
                () -> assertTrue(captureOutputStream.toString().contains("ENGR1762")),
                () -> assertTrue(captureOutputStream.toString().contains("ENGR1401")),
                () -> assertTrue(captureOutputStream.toString().contains("Found"))
        );
    }

    @DisplayName("4.02 Exact topic code retrieves only that class")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exactTopicCodeRetrievesOnlyThatClass() throws Exception {
        //Test input
        String input = "3\n" +
                        "COMP1711\n" +
                        "\n".repeat(14) +
                        "0\n" +
                        "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));

        //Timetable start with DB
        TimetableApp.main(new String[]{dbPath.toString()});

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        //Tests
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1711")),
                () -> assertFalse(captureOutputStream.toString().contains("COMP1003")),
                () -> assertTrue(captureOutputStream.toString().contains("Found"))
        );
    }

    @DisplayName("4.03 Topic name is case-insensitive")
    @Tag("Elijah")
    @Tag("Additional")
    @ParameterizedTest
    @ValueSource(strings = {"comp1711", "CoMp1711", "COmp1711", "ComP1711", "cOMp1711"})
    void topicNameIsCaseInsensitive(String topic) throws Exception {
        String input = "3\n" +
                        topic + "\n" +
                        "\n".repeat(14) +
                        "0\n" +
                        "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{dbPath.toString()});
        String output = captureOutputStream.toString();

//                System.err.println("---- CAPTURED OUTPUT ----");
//                System.err.println(output);
//                System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("COMP1711")),
                () -> assertFalse(captureOutputStream.toString().contains("COMP1003")),
                () -> assertTrue(captureOutputStream.toString().contains("Found"))
        );
    }

    @DisplayName("4.04 No Matches shows 'No classes found'")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void noClassMatchSearchMode() throws Exception {
        String input = "3\n" +
                        "\n".repeat(15) +
                        "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{":memory:"});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("No classes matched the search criteria."))
        );
    }

    @DisplayName("4.05 Semester filter works")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void semesterSearchMode() throws Exception {
        String input = "3\n" +
                "\n".repeat(4) +
                "S1\n" +
                "\n".repeat(11) +
                "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("S1")),
                () -> assertTrue(captureOutputStream.toString().contains("Found 56 matching class instance(s)"))
        );
    }

    @DisplayName("4.06 Campus filter works")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void campusSearchMode() throws Exception {
        String input = "3\n" +
                "\n".repeat(3) +
                "Tonsley\n" +
                "\n".repeat(12) +
                "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("Tonsley")),
                () -> assertTrue(captureOutputStream.toString().contains("Found 7 matching class instance(s)"))
        );
    }

    @DisplayName("4.07 Invalid input does not crash the app")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void invalidInputSearchMode() throws Exception {
        String input = "3\n" +
                "\n".repeat(4) +
                "S9\n" +
                "\n".repeat(11) +
                "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("Unknown option – please try again."))
        );
    }

    @DisplayName("4.Extra Printing a Specific Class' Details")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void printClassDetails() throws Exception {
        String input = "2\n" +
                        "2\n" +
                        "1\n" +
                        "0\n".repeat(4);

        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("COMP1002")),
                () -> assertTrue(captureOutputStream.toString().contains("Fundamentals of Artificial Intelligence")),
                () -> assertTrue(captureOutputStream.toString().contains("301 BYOD Computer Lab"))
        );
    }

    @DisplayName("4.Extra Editing a Specific Class' Details")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void editClassDetails() throws Exception {
        String input = "2\n" + "3\n" + "1\n" + "4\n" + "Tonsley" + "yes\n" + "0\n".repeat(4);

        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("COMP1002")),
                () -> assertTrue(captureOutputStream.toString().contains("Fundamentals of Artificial Intelligence")),
                () -> assertTrue(captureOutputStream.toString().contains("ALL of those classes will reflect the new campus."))
        );
    }

    @DisplayName("4.Extra Deleting a Specific Class' Details")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void deleteAClass() throws Exception {
        String input = "2\n" + "4\n" + "1\n" + "yes\n" + "0\n".repeat(4);

        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("COMP1002")),
                () -> assertTrue(captureOutputStream.toString().contains("Fundamentals of Artificial Intelligence")),
                () -> assertTrue(captureOutputStream.toString().contains("This action cannot be undone.")),
                () -> assertTrue(captureOutputStream.toString().contains("Deleted: Laboratory #1 for COMP1002"))
        );
    }

    @DisplayName("4.Extra Editing All of a Specific Class' Details")
    @Tag("Jayden")
    @Tag("Core")
    @ParameterizedTest(name = "Editing detail {0} to {1}")
    @CsvSource({
            "1, COMP9999", //Topic Code
            "2, COMP9999 Extra AI", //Topic Name
            "3, Online",  //Attendance Type
            "4, Tonsley",  // Campus
            "5, S2", // Semester
            "6, 2", // Availability Number
            "7, Workshop", //Class type
            "8, 1", // Instance Number
            "9, 12 Mar", // Start Date
            "10, 9 Jun", // End date
            "11, Tuesday", // Day of Week
            "12, 12:00", // Start time
            "13, 14:00", // End Time
            "14, Festival Tower", // Building
            "15, 501" // Room
    })
    void editingAllDetailsClass(int detailNo, String change) throws Exception {
        String input = "2\n" + "3\n" + "1\n" + detailNo + "\n" + change + "\n" + "yes\n" + "0\n".repeat(4);

        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertAll(() -> assertTrue(captureOutputStream.toString().contains("Confirm (yes / no):")));
    }


}
