package unit.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dao.ApprovalDao;
import dao.ReportDao;
import dao.UserDao;

/**
 * A database failure (SQLException) must become our own DatabaseException (FR-S4),
 * keeping the SQLException as the cause so the details still reach the log.
 * The Connection is a Mockito mock that fails on every query.
 */
class DaoErrorTest {

    private Connection brokenConn;

    @BeforeEach
    void setUp() throws SQLException {
        brokenConn = mock(Connection.class);
        when(brokenConn.prepareStatement(anyString())).thenThrow(new SQLException("disk I/O error"));
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-S4 - ApprovalDao turns SQLException into DatabaseException")
    void approvalDaoWrapsSqlException() {
        DatabaseException e = assertThrows(DatabaseException.class,
                () -> new ApprovalDao(brokenConn).findPending());
        assertTrue(e.getCause() instanceof SQLException);
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-S4, FR-A1 - UserDao turns SQLException during login into DatabaseException")
    void userDaoWrapsSqlException() {
        DatabaseException e = assertThrows(DatabaseException.class,
                () -> new UserDao(brokenConn).findByCredentials("mgr_asha", "asha123"));
        assertTrue(e.getCause() instanceof SQLException);
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-S4 - ReportDao turns SQLException into DatabaseException")
    void reportDaoWrapsSqlException() {
        DatabaseException e = assertThrows(DatabaseException.class,
                () -> new ReportDao(brokenConn).byEmployee());
        assertTrue(e.getCause() instanceof SQLException);
    }
}