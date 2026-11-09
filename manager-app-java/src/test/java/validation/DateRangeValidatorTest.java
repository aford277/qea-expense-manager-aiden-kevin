package validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import exception.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for DateRangeValidator (VR-07, report date range). */
class DateRangeValidatorTest {

    /** True if the range is accepted, false if it is rejected with ValidationException. */
    private static boolean acceptsRange(LocalDate start, LocalDate end) {
        try {
            DateRangeValidator.validateRange(start, end);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Covers: FR-M3, VR-07 (TM-05) - a valid YYYY-MM-DD date is parsed")
    void validDateIsParsed() {
        assertEquals(LocalDate.of(2026, 10, 28), DateRangeValidator.validateDate("2026-10-28"));
    }

    @ParameterizedTest(name = "[{0}] is rejected")
    @CsvSource({
            "''",            // empty
            "2026/10/28",    // wrong separator
            "28-10-2026",    // wrong order
            "2026-02-30",    // not a real date
            "2026-13-01",    // no month 13
            "abc"
    })
    @Tag("negative")
    @DisplayName("Covers: FR-M3, VR-07 (TM-05) - bad date format is rejected")
    void badDateIsRejected(String input) {
        assertThrows(ValidationException.class, () -> DateRangeValidator.validateDate(input));
    }

    @ParameterizedTest(name = "{0} to {1} -> {2}")
    @CsvSource({
            "2026-10-28, 2026-10-28, true",    // same day: the valid example from VR-07
            "2026-10-01, 2026-10-31, true",    // start before end
            "2026-10-29, 2026-10-28, false"    // start one day after end
    })
    @Tag("boundary")
    @Tag("negative")
    @DisplayName("Covers: FR-M3, VR-07 (TM-05) - start must be on or before end")
    void startMustNotBeAfterEnd(String start, String end, boolean expected) {
        assertEquals(expected, acceptsRange(LocalDate.parse(start), LocalDate.parse(end)));
    }
}