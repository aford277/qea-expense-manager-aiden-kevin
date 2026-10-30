package exception;
 
/** The action isn't allowed in the record's current state, e.g. deciding an expense that is no longer pending. */
public class InvalidStateException extends ExpenseManagerException {
    public InvalidStateException(String message) {
        super(message);
    }
 
    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}