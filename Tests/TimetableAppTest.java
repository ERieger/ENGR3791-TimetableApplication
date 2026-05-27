import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;


import static org.junit.jupiter.api.Assertions.*;

class TimetableAppTest {
    private final InputStream originalInputStream = System.in;
    private final PrintStream originalOutputStream = System.out;
    private final ByteArrayOutputStream captureOutputStream = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(captureOutputStream));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalInputStream);
        System.setOut(originalOutputStream);
    }

    @Tag("Jayden")
    @Tag("Critical")
    @DisplayName("3.01 Shows the loader command with defaults")
    @Test
    void importModeExpectedOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            TimetableApp.importMode("test.db");
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();

        assertAll(
                () -> assertTrue(output.contains("IMPORT MODE")),
                () -> assertTrue(output.contains("To import CSV data, run the data loader from the command line:")),
                () -> assertTrue(output.contains("cd ../data-loader && ./load.sh [csv-dir] [db-file]"))
        );
    }

    @Tag("Jayden")
    @Tag("Core")
    @DisplayName("3.02 Shows the loader command with defaults")
    @Test
    void importModePathArgument() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            TimetableApp.importMode("test.db");
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();

        assertAll(
                () -> assertTrue(output.contains("Default CSV directory : ../Spec and CSVs/CSV")),
                () -> assertTrue(output.contains("Default database file : test.db"))
        );
    }



    @Tag("Jayden")
    @Tag("Critical")
    @DisplayName("3.03 Display Import Mode view")
    @Test
    void importModeGettingTo() throws Exception {
        String input = "1" + System.lineSeparator() + "0" + System.lineSeparator();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{":memory:"});
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("MAIN MENU")),
                () -> assertTrue(captureOutputStream.toString().contains("IMPORT MODE"))
        );
    }
}