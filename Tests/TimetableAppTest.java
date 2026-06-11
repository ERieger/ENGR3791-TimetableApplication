import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;


import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.DisplayName.class)
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



    @Tag("Lucy")
    @Tag("Critical")
    @DisplayName("1.01 Main Menu Inputs (Correct)")
    @Test
    void mainMenuInputsCorrect() throws Exception{
        String[] inputs = {
                "0\n",
                "1\n0\n",
                "2\n0\n0\n",
                "3\n" + "\n".repeat(15) + "0\n0\n",   //because "search classes" takes LOADS of inputs!!
                "4\n0\n0\n"
        };

        for (String input : inputs) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        TimetableApp.main(new String[]{"data-loader/timetable.db"});

        String output = captureOutputStream.toString();

        assertFalse(output.contains("Unknown option – please try again"));
        }
    }
    
    @Tag("Lucy")
    @Tag("Core")
    @DisplayName("1.02 Main Menu Inputs (Incorrect)")
    @ParameterizedTest
    @CsvSource ({"'7\n0\n'",
            "'30\n0\n'",
            "'b\n0\n'"})
    void mainMenuInputsIncorrect(String input) throws Exception{

            System.setIn(new ByteArrayInputStream(input.getBytes()));

            TimetableApp.main(new String[]{"data-loader/timetable.db"});

            String output = captureOutputStream.toString();

            assertTrue(output.contains("Unknown option – please try again"));
        }

    @Tag("Numa")
    @Tag("Core")
    @DisplayName("1.03 Main Menu Inputs (MAX and MIN Ints)")
    @ParameterizedTest
    @ValueSource(strings= {Integer.MAX_VALUE+"", Integer.MIN_VALUE+""})
    void mainMenuInputsMaxAndMinInts( String intput) throws Exception {
        String input = intput +
                "\n" +
                "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();
        assertTrue(output.contains("Unknown option – please try again"));

    }

    @Tag("Numa")
    @Tag("Additional")
    @DisplayName("1.04 Main Menu Inputs (Passing Nulls)")
    @Test
    void mainMenuInputsPassingNulls() throws Exception {
        String input = null +
                "\n" +
                "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        TimetableApp.main(new String[]{"data-loader/timetable.db"});
        String output = captureOutputStream.toString();
        assertTrue(output.contains("Unknown option – please try again"));
    }

    @Tag("Lucy")
    @Tag("Critical")
    @DisplayName("1.13 Exit Option 0")
    @Test
    void exitOption0() throws Exception{
        String input = "0\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        TimetableApp.main(new String[]{"data-loader/timetable.db"});

        System.setOut(System.out);
        String output = captureOutputStream.toString();

        assertTrue(output.contains("Goodbye"));
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
        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);
        TimetableApp.main(new String[]{":memory:"});
        assertAll(
                () -> assertTrue(captureOutputStream.toString().contains("MAIN MENU")),
                () -> assertTrue(captureOutputStream.toString().contains("IMPORT MODE"))
        );
    }

}
