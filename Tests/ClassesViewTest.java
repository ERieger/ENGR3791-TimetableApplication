import org.junit.jupiter.api.*;

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




    @Test
    void printClassDetail() {
    }
}