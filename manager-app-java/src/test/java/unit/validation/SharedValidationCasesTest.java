package unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import exception.ValidationException;
import validation.InputValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Shared validation cases (Section 9.5).
 * Reads shared-test-data/validation_cases.csv - the same file the Python suite reads.
 * The path is relative to manager-app-java/, which is where Maven runs the tests.
 */
class SharedValidationCasesTest {

    @ParameterizedTest(name = "{0} = [{1}] -> {2}")
    @CsvFileSource(files = "../shared-test-data/validation_cases.csv", numLinesToSkip = 1)
    @Tag("negative")
    @Tag("boundary")
    @DisplayName("Covers: VR-08, VR-09, FR-A1 (TV-01, TV-02) - shared CSV cases")
    void sharedValidationCases(String field, String input, boolean expected) {
        if (expected) {
            assertEquals(expectedResult(field, input), validate(field, input));
        } else {
            assertThrows(ValidationException.class, () -> validate(field, input));
        }
    }

    /** Calls the validator the way the Manager App does. Menu choice uses the main menu (0-3). */
    private static Object validate(String field, String input) {
        return switch (field) {
            case "expense_id" -> InputValidator.validateExpenseId(input);
            case "menu_choice" -> InputValidator.validateMenuChoice(input, 0, 1, 2, 3);
            case "login" -> InputValidator.validateLoginInput(input);
            default -> throw new IllegalArgumentException("Unknown field in CSV: " + field);
        };
    }

    /** What a valid input should come back as. */
    private static Object expectedResult(String field, String input) {
        if (field.equals("login")) {
            return input;
        }
        return Integer.parseInt(input.trim());
    }
}