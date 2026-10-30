package exception;
 
/** The requested record doesn't exist, e.g. an expense ID with no matching row. */
public class NotFoundException extends ExpenseManagerException {
    public NotFoundException(String message) {
        super(message);
    }
 
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}