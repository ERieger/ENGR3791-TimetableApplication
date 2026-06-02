import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
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



class ClassesViewTest {
    private final InputStream originalInputStream = System.in;
    private final PrintStream originalOutputStream = System.out;
    private final ByteArrayOutputStream captureOutputStream = new ByteArrayOutputStream();

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

    @Test
    void show() {
    }

    @DisplayName("1.05.01 Class View Input (Correct) Modes 1 to 4")
    @Tag("Aidan")
    @Tag("Critical")
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void classViewInputN(int mode) throws Exception {
        //Test input
        String input = mode + System.lineSeparator() + 0;

        //Setup for testing input?
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //I haven't the faintest idea
        Scanner scan = new Scanner(captureInputStream);
        Database db = new Database("timetable.db");

        //Instance of ClassesView obj for testing
        ClassesView cv = new ClassesView(db, scan);
        cv.show();

        //Tests
        assertTrue(captureOutputStream.toString().contains("No class data found. Import data first."));
    }

    @DisplayName("1.05.01 Class View Input (Correct) Modes 1 to 4")
    @Tag("Aidan")
    @Tag("Critical")
    @Test
    void classViewInput0() throws Exception {
        //Test input
        String input = 0 + System.lineSeparator() + 0;

        //Setup for testing input?
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //I haven't the faintest idea
        Scanner scan = new Scanner(captureInputStream);
        Database db = new Database("timetable.db");

        //Instance of ClassesView obj for testing
        ClassesView cv = new ClassesView(db, scan);
        cv.show();

        //Tests
        assertAll(
                () -> assertFalse(captureOutputStream.toString().contains("No class data found. Import data first.")), //modes 1 to 4
                () -> assertFalse(captureOutputStream.toString().contains("Unknown option – please try again.")), //incorrect mode
                () -> assertTrue(captureOutputStream.toString().contains("Enter option:")) //Not unique hence above tests
        );
    }

    @DisplayName("1.06 Class View Input (Incorrect)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void classViewInputIncorrect() throws Exception {
        //Test input
        String input = "egg" + System.lineSeparator() + 0;

        //Setup for testing input?
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //I haven't the faintest idea
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
    @ValueSource(ints = {Integer.MIN_VALUE,Integer.MAX_VALUE})
    void classViewInputMaxAndMinInts(int mode) throws Exception {
        //Test input
        String input = mode + System.lineSeparator() + 0;

        //Setup for testing input?
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //I haven't the faintest idea
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
        String input = mode + System.lineSeparator() + 0;

        //Setup for testing input?
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        //I haven't the faintest idea
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
        String input = "10\n" + "0\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ClassesView testView = new ClassesView(null, new Scanner(System.in)); //I tried putting in "data-loader/timetable.db" but it was upset with that
        testView.show();

        assertTrue(captureOutputStream.toString().contains("Unknown option – please try again"));
    }
    //could also add in extra tests for edge cases (max/min inputs) if we get time! So its more like the pracs :)

    @DisplayName("2.02 viewing individual classes rejects incorrect inputs")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassIncorrectValues() throws Exception {
        String input = "2\n" + "2\n" + "notaclass\n" + "0\n" + "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        TimetableApp.main(new String[]{"data-loader/timetable.db"});

        System.setOut(System.out);
        String output = outputStream.toString();

        assertTrue(output.contains("Invalid number"));
    }

    @DisplayName("2.03 Browse all shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void browseAllNoDataWarning() throws Exception {
        String input = "2\n" + "1\n" + "0\n" + "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        TimetableApp.main(new String[]{":memory:"});

        System.setOut(System.out);
        String output = outputStream.toString();

        assertTrue(output.contains("No class data found"));
    }

    @DisplayName("2.04 viewing individual classes shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassViewNoDataWarning() throws Exception {
        String input = "2\n" + "2\n" + "0\n" + "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        TimetableApp.main(new String[]{":memory:"});

        System.setOut(System.out);
        String output = outputStream.toString();

        assertTrue(output.contains("No class data found"));
    }

    @DisplayName("2.05 Edit class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void editClassNoDataWarning() throws Exception  {
        String input = "2\n" + "3\n" + "0\n" + "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        TimetableApp.main(new String[]{":memory:"});

        System.setOut(System.out);
        String output = outputStream.toString();

        assertTrue(output.contains("No class data found"));
    }

    @DisplayName("2.06 Delete class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void deleteClassNoDataWarning() throws Exception {
        String input = "2\n" + "4\n" + "0\n" + "0\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        TimetableApp.main(new String[]{":memory:"});

        System.setOut(System.out);
        String output = outputStream.toString();

        assertTrue(output.contains("No class data found"));
    }

    @DisplayName("4.01 Blank criteria shows all classes")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void blankCriteriaShowsAllClasses() throws Exception {
        String input = "3\n" +
                "\n".repeat(16) +
                "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();


        assertAll(
                () -> assertTrue(output.contains("SEARCH RESULTS")),
                () -> assertTrue(output.contains("COMP1002")),
                () -> assertTrue(output.contains("COMP1102")),
                () -> assertTrue(output.contains("COMP1103")),
                () -> assertTrue(output.contains("COMP1701")),
                () -> assertTrue(output.contains("COMP1702")),
                () -> assertTrue(output.contains("COMP1711")),
                () -> assertTrue(output.contains("ENGR1401")),
                () -> assertTrue(output.contains("ENGR1762")),
                () -> assertTrue(output.contains("ENGR1401")),
                () -> assertTrue(output.contains("Found"))
        );



    }

    @DisplayName("4.02 Exact topic code retrieves only that class")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exactTopicCodeRetrievesOnlyThatClass() throws Exception {
        String input = "3\n" +
                        "COMP1711\n" +
                        "\n".repeat(14) +
                        "0\n" +
                        "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//        System.err.println("---- CAPTURED OUTPUT ----");
//        System.err.println(output);
//        System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("SEARCH RESULTS")),
                () -> assertTrue(output.contains("COMP1711")),
                () -> assertFalse(output.contains("COMP1003")),
                () -> assertTrue(output.contains("Found"))
        );
    }

    @DisplayName("4.03 Topic name is case-insensitive")
    @Tag("Elijah")
    @Tag("Additional")
    @ParameterizedTest
    @ValueSource(strings={"comp1711", "CoMp1711", "COmp1711", "ComP1711", "cOMp1711"})
    void topicNameIsCaseInsensitive(String topic) throws Exception {
        String input = "3\n" +
                 topic + "\n" +
                "\n".repeat(14) +
                "0\n" +
                "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();

//                System.err.println("---- CAPTURED OUTPUT ----");
//                System.err.println(output);
//                System.err.println("-------------------------");

        assertAll(
                () -> assertTrue(output.contains("SEARCH RESULTS")),
                () -> assertTrue(output.contains("COMP1711")),
                () -> assertFalse(output.contains("COMP1003")),
                () -> assertTrue(output.contains("Found"))
        );
    }

    @DisplayName("4.04 No Matches shows 'No classes found'")
    @Tag("Jayden")
    @Tag("Core")
    @Test
    void noClassMatchSearchMode() throws Exception {
        String input = "3\n" + "\n".repeat(15) + "0\n";
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
        String input = "3\n" + "\n".repeat(4) + "S1\n" + "\n".repeat(11) + "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
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
        String input = "3\n" + "\n".repeat(3) + "Tonsley\n" + "\n".repeat(12) + "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
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
        String input = "3\n" + "\n".repeat(4) + "S9\n" + "\n".repeat(11) + "0\n";
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("SEARCH RESULTS")),
                () -> assertTrue(captureOutputStream.toString().contains("Unknown option – please try again."))
        );
    }

}
