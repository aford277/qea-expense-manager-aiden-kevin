package exception;
 
/** A database problem. Wraps the original SQLException so the details still reach the log. */
public class DatabaseException extends ExpenseManagerException {
    public DatabaseException(String message) {
        super(message);
    }
 
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}