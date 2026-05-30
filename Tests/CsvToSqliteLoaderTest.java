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

class CsvToSqliteLoaderTest {
    private final InputStream originalInputStream = System.in;
    private final PrintStream originalOutputStream = System.out;
    private final ByteArrayOutputStream captureOutputStream = new ByteArrayOutputStream();

    @BeforeAll
    static void setUpDb() throws Exception {
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

    @DisplayName("6.01 Not Null Import with Correct Data")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void importCSV() throws Exception {
        Path dbPath = Paths.get("data-loader", "timetable.db");
        CsvToSqliteLoader.main(new String[]{ //importing viable data from the CSV docs in the CSV directory
                "./Spec and CSVs/CSV",
                dbPath.toString()
        });
        String output = captureOutputStream.toString();

        assertAll(
                () -> assertTrue(output.contains("Loading: ENGR1762 Networks and Cybersecurity.csv")),
                () -> assertTrue(output.contains("Done. Loaded 8 file(s)"))
        );

    }

    @DisplayName("6.02 Incorrect File Type")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void importCSVError() throws Exception{
        Path dbPath = Paths.get("data-loader", "timetable.db");
        CsvToSqliteLoader.main(new String[]{ //importing data from the PDF doc in the notCSV directory
                "./Spec and CSVs/LoaderTests/notCSV",
                dbPath.toString()
        });
        String output = captureOutputStream.toString();

        assertAll(
                () -> assertFalse(output.contains("Loading: ENGR1762 Networks and Cybersecurity.pdf")),
                () -> assertTrue(output.contains("Done. Loaded 0 file(s), 0 data row(s)"))
        );
    }

    @DisplayName("6.03 Import with gaps in data")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void importDataErrors(){
        Path dbPath = Paths.get("data-loader", "timetable.db");

        assertThrowsExactly(IllegalArgumentException.class,
                () -> CsvToSqliteLoader.main(new String[]{ //importing incomplete data from the CSV doc in the IncompleteCSV directory
                "./Spec and CSVs/LoaderTests/IncompleteCSV", dbPath.toString() })
        );
    }

    @DisplayName("6.04 Passing Null Data")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void passingNullData() {
        Path dbPath = Paths.get("data-loader", "timetable.db");

        assertThrowsExactly(NullPointerException.class,
                () -> CsvToSqliteLoader.main(new String[]{ //importing null from the null null in the null null
                null, dbPath.toString() })
        );
    }

    @DisplayName("6.05 Passing Empty Data")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void passingEmptyData() throws Exception{
        Path dbPath = Paths.get("data-loader", "timetable.db");
        CsvToSqliteLoader.main(new String[]{ //importing no data from the not there docs in the nothing directory
                "./Spec and CSVs/LoaderTests/nothing",
                dbPath.toString()
        });
        String output = captureOutputStream.toString();
        assertAll(
                () -> assertFalse(output.contains("Loading")),
                () -> assertTrue(output.contains("Done. Loaded 0 file(s), 0 data row(s)"))
        );
    }

    @DisplayName("6.Extra Passing Empty Data")
    @Tag("Jayden")
    @Tag("Additional")
    @Test
    void passingEmptyData2() throws Exception{
        Path dbPath = Paths.get("data-loader", "timetable.db");
        Path tempdir = Files.createTempDirectory("csv-test"); //importing an empty csv file
        Files.createFile(tempdir.resolve("emptyTest.csv"));

        CsvToSqliteLoader.main(new String[]{
                tempdir.toString(),
                dbPath.toString()
        });
        String output = captureOutputStream.toString();
        assertAll(
                () -> assertTrue(output.contains("Loading")),
                () -> assertTrue(output.contains("Done. Loaded 1 file(s), 0 data row(s)"))
        );
    }


}