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
class FailedTests {

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

    @DisplayName("Failed Test, Splitting building and room when building is empty")
    @Tag("Jayden")
    @Tag("Additional")
    @Test
    void sessionRecordEmptyBuilding() {
        SessionRecord session = new SessionRecord(1,"01 Feb","02 Feb","Monday",null,"09:00","10:00",", 5.25");
        assertEquals("", session.building);
        assertEquals("5.25", session.room);
    }

    @DisplayName("Failed Test, Null Location crashing App")
    @Tag("Jayden")
    @Tag("Additional")
    @Test
    void sessionRecordNullRecordDoesNotCrash() {
        assertDoesNotThrow(() -> new SessionRecord(1,"01 Feb","02 Feb","Monday",null,"09:00","10:00",null));
    }

    @DisplayName("Failed Test, Setting impossible time")
    @Tag("Jayden")
    @Tag("Additional")
    @Test
    void timetableRejectingInvalidTime() throws Exception {

        String input = "2\n" + "3\n" + "1\n" + "12\n" + "25:99" + "yes\n" + "0\n".repeat(4);

        ByteArrayInputStream captureInputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(captureInputStream);

        TimetableApp.main(new String[]{dbPath.toString()});

        assertTrue(captureOutputStream.toString().contains("Failed to Update"));
    }



}
