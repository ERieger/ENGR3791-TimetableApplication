import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TimetableModeTest {

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
    }

    @DisplayName("5.03 Delete mode cancels correctly after prompt")
    @Tag("Elijah")
    @Tag("Critical")
    @Test
    void deleteModeCancel() throws Exception {}

    @DisplayName("5.04 Delete mode deletes correctly after prompt")
    @Tag("Elijah")
    @Tag("Critical")
    @Test
    void deleteModeDelete() {
    }

    @DisplayName("5.05 Export Incorrect File Path")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportIncorrectFile() throws Exception {}

    @DisplayName("5.06 Export Null File Path")
    @Tag("Elijah")
    @Tag("Additional")
    @Test
    void exportNullFile() throws Exception {}

    @DisplayName("5.07 Export Possible File Path")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportPossibleFile() throws Exception {}

    @DisplayName("5.08 Exporting in TSV")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void exportTSV() throws Exception {}

    @DisplayName("5.09 Exporting in JSON")
    @Tag("Elijah")
    @Tag("Additional")
    @Test
    void exportJSON() throws Exception {}

    @DisplayName("5.10 Exporting in CSV")
    @Tag("Elijah")
    @Tag("Core")
    @Test
    void exportCSV() throws Exception {}

    @DisplayName("5.11 Generating Timetable error/help messages")
    @Tag("Aidan")
    @Tag("Core")
    @Test
    void generatingModeErrors() throws Exception {
    }








}