package exception;
 
/**
 * Base class for every app-specific error.
 * It extends RuntimeException, which makes it "unchecked": methods don't have to
 * declare it with `throws`, and callers aren't forced to catch it.
 */
public class ExpenseManagerException extends RuntimeException {
    public ExpenseManagerException(String message) {
        super(message);
    }
 
    public ExpenseManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
 