package service;
 
import dao.UserDao;
import exception.AuthenticationException;
import exception.AuthorizationException;
import model.User;
 
/** Login rules (spec FR-A1, FR-A2). No printing here — the Menu does that. */
public class AuthService {
 
    private final UserDao userDao;
 
    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }
 
    public User login(String username, String password) {
        User user = userDao.findByCredentials(username.trim(), password)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password."));
 
        if (!user.isManager()) {
            throw new AuthorizationException("This account does not have access to this app.");
        }
        return user;
    }
}