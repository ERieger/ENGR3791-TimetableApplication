import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetableAppTest {
    private PrintStream originalOut;
    private java.io.InputStream originalIn;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    void main_importModePath_displaysMenuAndImportMode() throws Exception {
        String output = runMainWithInput("1\n0\n");

        assertTrue(output.contains("main menu"));
        assertTrue(output.contains("import mode"));
        assertTrue(output.contains("to import csv data, run the data loader from the command line:"));
        assertInOrder(output,
                "main menu",
                "enter option:",
                "import mode",
                "to import csv data, run the data loader from the command line:");
    }

    @Test
    void main_invalidOptionThenImportMode_displaysWarningThenImportMode() throws Exception {
        String output = runMainWithInput("9\n1\n0\n");

        assertTrue(output.contains("unknown option"));
        assertTrue(output.contains("import mode"));
        assertTrue(output.contains("to import csv data, run the data loader from the command line:"));
        assertInOrder(output, "unknown option", "import mode");
    }

    private String runMainWithInput(String input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Path tempDb = Files.createTempFile("timetable-app-test-", ".db");

        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

            TimetableApp.main(new String[]{tempDb.toString()});
        } finally {
            boolean deleted = Files.deleteIfExists(tempDb);
            assertTrue(deleted || !Files.exists(tempDb), "Temporary test DB file was not cleaned up");
        }

        String raw = out.toString(StandardCharsets.UTF_8);
        String noAnsi = raw.replaceAll("\\u001B\\[[;\\d]*m", "");
        return noAnsi.toLowerCase(Locale.ROOT);
    }

    private void assertInOrder(String text, String first, String second) {
        int firstIndex = text.indexOf(first);
        int secondIndex = text.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing text: " + first);
        assertTrue(secondIndex >= 0, "Missing text: " + second);
        assertTrue(firstIndex < secondIndex,
                "Expected '" + first + "' to appear before '" + second + "'");
    }

    private void assertInOrder(String text, String first, String second, String third, String fourth) {
        assertInOrder(text, first, second);
        assertInOrder(text, second, third);
        assertInOrder(text, third, fourth);
    }
}