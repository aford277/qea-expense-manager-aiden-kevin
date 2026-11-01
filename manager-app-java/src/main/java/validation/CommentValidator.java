package validation;

import exception.ValidationException;

public class CommentValidator {

    public static final int MAX_LENGTH = 200;
    public static final int MIN_DENIAL_LENGTH = 5;

    public static void validateDenial(String comment){

        String trimmed = (comment == null) ? "" : comment.trim();

        if (trimmed.length() < MIN_DENIAL_LENGTH) {
            throw new ValidationException("Comment is too short! Must be at least 5 characters.");
        } else if (trimmed.length() > MAX_LENGTH) {
            throw new ValidationException("Comment is too long! Must be 200 characters at most.");
        }
    }

    public static void validateApproval(String comment) {
        if (comment != null && comment.length() > MAX_LENGTH) {
            throw new ValidationException("Approval comment must be 200 characters or fewer.");
        }
    }
}
