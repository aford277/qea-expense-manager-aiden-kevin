package exception;

public class AuthenticationException extends ExpenseManagerException {
    public AuthenticationException(String message) {
        super(message);
    }
 
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
 