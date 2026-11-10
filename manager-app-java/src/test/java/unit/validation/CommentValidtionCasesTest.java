package unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import exception.ValidationException;
import validation.CommentValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for CommentValidator (VR-05 denial comment, VR-06 approval comment). */
class CommentValidatorTest {

    /** True if the denial comment is accepted, false if it is rejected with ValidationException. */
    private static boolean acceptsDenial(String comment) {
        try {
            CommentValidator.validateDenial(comment);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    /** True if the approval comment is accepted, false if it is rejected with ValidationException. */
    private static boolean acceptsApproval(String comment) {
        try {
            CommentValidator.validateApproval(comment);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- VR-05 denial comment (TM-02)

    @ParameterizedTest(name = "[{0}] -> {1}")
    @CsvSource({
            "'', false",                // empty
            "'     ', false",           // spaces only
            "abcd, false",              // 4 characters
            "'  abcd  ', false",        // 4 characters after trimming
            "abcde, true",              // 5 characters: the minimum
            "Missing receipt, true"     // the valid example from VR-05
    })
    @Tag("boundary")
    @Tag("negative")
    @DisplayName("Covers: FR-M2, VR-05 (TM-02) - denial comment needs 5+ characters after trimming")
    void denialCommentMinimumLength(String comment, boolean expected) {
        assertEquals(expected, acceptsDenial(comment));
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-M2, VR-05 (TM-02) - 200-character denial comment is accepted")
    void denialCommentOf200IsAccepted() {
        assertTrue(acceptsDenial("a".repeat(200)));
    }

    @Test
    @Tag("boundary")
    @Tag("negative")
    @DisplayName("Covers: FR-M2, VR-05 (TM-02) - 201-character denial comment is rejected")
    void denialCommentOf201IsRejected() {
        assertThrows(ValidationException.class, () -> CommentValidator.validateDenial("a".repeat(201)));
    }

    // ---------------------------------------------------------------- VR-06 approval comment (TM-03)

    @ParameterizedTest(name = "[{0}] -> {1}")
    @CsvSource({
            "'', true",     // empty: the comment is optional
            "OK, true"      // the valid example from VR-06
    })
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - approval comment is optional")
    void approvalCommentIsOptional(String comment, boolean expected) {
        assertEquals(expected, acceptsApproval(comment));
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - 200-character approval comment is accepted")
    void approvalCommentOf200IsAccepted() {
        assertTrue(acceptsApproval("a".repeat(200)));
    }

    @Test
    @Tag("boundary")
    @Tag("negative")
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - 201-character approval comment is rejected")
    void approvalCommentOf201IsRejected() {
        assertThrows(ValidationException.class, () -> CommentValidator.validateApproval("a".repeat(201)));
    }
}