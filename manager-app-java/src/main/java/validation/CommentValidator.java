package validation;

import exception.ValidationException;

public class CommentValidator {
    public static boolean validateDenial(String comment){
        if (comment == null || comment.isBlank()){
            throw new ValidationException("Comment is empty! Denial comment is required.");
        } else if (comment.length() < 5) {
            throw new ValidationException("Comment is too short! Must be at least 5 characters.");
        } else if (comment.length() > 200) {
            throw new ValidationException("Comment is too long! Must be 200 characters at most.");
        }

        return true;
    }
}
