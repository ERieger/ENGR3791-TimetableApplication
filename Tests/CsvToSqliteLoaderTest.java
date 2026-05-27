import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CsvToSqliteLoaderTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @DisplayName("6.01 Not Null Import with Correct Data")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void importCSV() {}

    @DisplayName("6.02 Incorrect File Type")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void importCSVError() {}

    @DisplayName("6.03 Import with gaps in data")
    @Tag("Numa")
    @Tag("Core")
    @Test
    void importDataErrors(){}

    @DisplayName("6.04 Passing Null Data")
    @Tag("Numa")
    @Tag("Additional")
    @Test
    void passingNullData(){}

}