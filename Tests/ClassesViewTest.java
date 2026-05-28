import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @DisplayName("1.05 Class View Input (Correct)")
    @Tag("Aidan")
    @Tag("Critical")
    @Test
    void classViewInput() throws Exception {

    }

    @DisplayName("1.06 Class View Input (Incorrect)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void classViewInputIncorrect() throws Exception {

    }

    @DisplayName("1.07 Class View Input (MAX and MIN Ints)")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void classViewInputMaxAndMinInts() throws Exception {

    }

    @DisplayName("1.08 Class View Input (Passing Nulls)")
    @Tag("Aidan")
    @Tag("Additional")
    @Test
    void classViewInputPassingNulls() throws Exception {

    }

    @DisplayName("2.01 Invalid input shows warning")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void invalidInputShowsWarning() throws Exception {

    }

    @DisplayName("2.02 viewing individual classes rejects incorrect inputs")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassIncorrectValues() throws Exception {

    }

    @DisplayName("2.03 Browse all shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void browseAllNoDataWarning() throws Exception {

    }

    @DisplayName("2.04 viewing individual classes shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void individualClassViewNoDataWarning() throws Exception {

    }

    @DisplayName("2.05 Edit class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void editClassNoDataWarning() throws Exception {

    }

    @DisplayName("2.06 Delete class shows warning with no class data")
    @Tag("Lucy")
    @Tag("Core")
    @Test
    void deleteClassNoDataWarning() throws Exception {

    }

    @DisplayName("4.01 Blank criteria shows all classes")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void blankCriteriaShowsAllClasses() throws Exception {

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