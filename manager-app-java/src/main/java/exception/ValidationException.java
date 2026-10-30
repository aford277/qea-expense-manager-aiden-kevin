package exception;
 
/** Input breaks a validation rule. */
public class ValidationException extends ExpenseManagerException {
    public ValidationException(String message) {
        super(message);
    }
 
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}