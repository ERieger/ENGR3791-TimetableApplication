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
    }

    @Test
    void main_invalidOptionThenImportMode_displaysWarningThenImportMode() throws Exception {
        String output = runMainWithInput("9\n1\n0\n");

        assertTrue(output.contains("unknown option"));
        assertTrue(output.contains("import mode"));
        assertTrue(output.contains("to import csv data, run the data loader from the command line:"));
    }

    private String runMainWithInput(String input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Path tempDb = Files.createTempFile("timetable-app-test-", ".db");

        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

            TimetableApp.main(new String[]{tempDb.toString()});
        } finally {
            Files.deleteIfExists(tempDb);
        }

        String raw = out.toString(StandardCharsets.UTF_8);
        String noAnsi = raw.replaceAll("\\u001B\\[[;\\d]*m", "");
        return noAnsi.toLowerCase(Locale.ROOT);
    }
}