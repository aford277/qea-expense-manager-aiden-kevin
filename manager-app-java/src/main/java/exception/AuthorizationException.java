package exception;
 
/** User is not allowed to do this (FR-A2, FR-A4). */
public class AuthorizationException extends ExpenseManagerException {
    public AuthorizationException(String message) {
        super(message);
    }
 
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
 