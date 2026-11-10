package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dao.ApprovalDao;
import dao.ReportDao;
import dao.UserDao;
import exception.AuthorizationException;
import integration.support.DatabaseTest;
import integration.support.DbQueries;
import integration.support.DbQueries.ApprovalRow;
import java.sql.SQLException;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import service.ApprovalService;
import service.AuthService;
import service.ReportService;

/**
 * Integration tests for login and role checks, against a real temporary database
 * built from sql/schema.sql and sql/seed.sql (see DatabaseTest).
 */
class AuthIntegrationTest extends DatabaseTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Runs after DatabaseTest's @BeforeEach, so conn is already open and seeded
        authService = new AuthService(new UserDao(conn));
    }

    // TA-05: FR-A1

    @Test
    @Tag("smoke")
    @DisplayName("Covers: FR-A1 (TA-05) - manager logs in with valid credentials and gets their user record")
    void managerLogsInWithValidCredentials() {
        User user = authService.login("mgr_asha", "asha123");

        assertEquals(1, user.id());
        assertEquals("mgr_asha", user.username());
        assertEquals("manager", user.role());
        assertEquals("Asha Patel", user.fullName());   // Menu prints "Welcome, " + fullName
    }

    // TA-02: FR-A2

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A2 (TA-02) - employee logs in to the Manager App and is refused")
    void employeeIsRefusedByManagerApp() {
        AuthorizationException e = assertThrows(AuthorizationException.class,
                () -> authService.login("emp_jordan", "jordan123"));
        assertEquals("This account does not have access to this app.", e.getMessage());
    }

    @ParameterizedTest(name = "{0} is refused")
    @CsvSource({
            "emp_maria, maria123",
            "emp_wei,   wei123",
            "emp_omar,  omar123"
    })
    @Tag("negative")
    @DisplayName("Covers: FR-A2 (TA-02) - every other seeded employee is refused too")
    void otherEmployeesAreRefused(String username, String password) {
        assertThrows(AuthorizationException.class, () -> authService.login(username, password));
    }

    // FR-A4: role checks in the services
    // The services refuse non-managers themselves, not just the menu.
    // Complements the Mockito unit tests for TA-04 by checking the real database row is unchanged,
    // and covers the reports side of FR-A4, which has no unit test.

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A4 (TA-04, integration) - an employee can't approve or deny; the row is unchanged")
    void employeeCannotDecideAndNothingChanges() throws SQLException {
        ApprovalService approvalService = new ApprovalService(new ApprovalDao(conn), SEED_CLOCK);

        assertThrows(AuthorizationException.class, () -> approvalService.approve(EMPLOYEE, 25, "OK"));
        assertThrows(AuthorizationException.class, () -> approvalService.deny(EMPLOYEE, 25, "Missing receipt"));

        // Seed expense 25 is pending: it must still be pending with no review data
        ApprovalRow row = DbQueries.approvalFor(conn, 25);
        assertEquals("pending", row.status());
        assertTrue(row.reviewerId() == null, "reviewer_id should still be NULL");
        assertTrue(row.reviewDate() == null, "review_date should still be NULL");
        assertTrue(row.comment() == null, "comment should still be NULL");
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A4 (integration) - an employee can't run reports")
    void employeeCannotRunReports() {
        ReportService reportService = new ReportService(new ReportDao(conn), SEED_CLOCK);

        assertThrows(AuthorizationException.class, () -> reportService.byEmployee(EMPLOYEE));
        assertThrows(AuthorizationException.class, () -> reportService.byCategory(EMPLOYEE));
    }
}