package validation;

import exception.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Report date range rules (spec §7: VR-07; used by the date-range report in FR-M3). */
public final class DateRangeValidator {

    public static LocalDate validateDate(String input) {
        if (input == null || input.isBlank()) {
            throw new ValidationException("Date is required (YYYY-MM-DD).");
        }
        try {
            // LocalDate.parse only accepts YYYY-MM-DD and rejects impossible dates like Feb 30
            return LocalDate.parse(input.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date. Use YYYY-MM-DD.");
        }
    }

    public static void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new ValidationException("Start date must be on or before end date.");
        }
    }
}