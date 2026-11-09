package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dao.UserDao;
import exception.AuthenticationException;
import exception.AuthorizationException;
import java.util.Optional;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for AuthService, with a mocked UserDao (no database). */
class AuthServiceTest {

    private static final User MANAGER = new User(1, "mgr_asha", "manager", "Asha Patel");
    private static final User EMPLOYEE = new User(3, "emp_jordan", "employee", "Jordan Lee");

    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        authService = new AuthService(userDao);
    }

    @Test
    @DisplayName("Covers: FR-A1 - manager logs in; username is trimmed before the lookup")
    void managerLogsIn() {
        when(userDao.findByCredentials("mgr_asha", "asha123")).thenReturn(Optional.of(MANAGER));

        assertEquals(MANAGER, authService.login("  mgr_asha  ", "asha123"));
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A1 - wrong credentials throw AuthenticationException with the spec message")
    void wrongCredentialsAreRejected() {
        when(userDao.findByCredentials("mgr_asha", "wrong")).thenReturn(Optional.empty());

        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> authService.login("mgr_asha", "wrong"));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A2 - an employee is refused by the Manager App with the spec message")
    void employeeIsRefused() {
        when(userDao.findByCredentials("emp_jordan", "jordan123")).thenReturn(Optional.of(EMPLOYEE));

        AuthorizationException e = assertThrows(AuthorizationException.class,
                () -> authService.login("emp_jordan", "jordan123"));
        assertEquals("This account does not have access to this app.", e.getMessage());
    }
}