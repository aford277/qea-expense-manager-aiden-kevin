package support;

import db.Database;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * Base class for Manager App integration tests (Section 9.4).
 *
 * Every test gets its own temporary database, built from the real
 * ../sql/schema.sql and ../sql/seed.sql in @BeforeEach and deleted in @AfterEach.
 * Tests never use EM_DB_PATH or the real expense_manager.db.
 *
 * Usage:
 *   class ApprovalServiceIntegrationTest extends DatabaseTest {
 *       @Test
 *       void approvesPendingExpense() {
 *           ApprovalService service = new ApprovalService(new ApprovalDao(conn), SEED_CLOCK);
 *           ...
 *       }
 *   }
 *
 * Every subclass is tagged "integration" automatically (@Tag is inherited).
 */
@Tag("integration")
public abstract class DatabaseTest {

    // Paths are relative to manager-app-java/, which is where `mvn test` runs.
    private static final Path SCHEMA_SQL = Path.of("..", "sql", "schema.sql");
    private static final Path SEED_SQL = Path.of("..", "sql", "seed.sql");

    /** seed.sql dates were picked relative to this day. */
    protected static final LocalDate SEED_TODAY = LocalDate.of(2026, 10, 28);

    /** A Clock fixed at SEED_TODAY, so review_date (FR-M2) is predictable (Section 4.3). */
    protected static final Clock SEED_CLOCK = Clock.fixed(
            SEED_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    // Users from seed.sql
    protected static final User MANAGER = new User(1, "mgr_asha", "manager", "Asha Patel");
    protected static final User OTHER_MANAGER = new User(2, "mgr_daniel", "manager", "Daniel Kim");
    protected static final User EMPLOYEE = new User(3, "emp_jordan", "employee", "Jordan Lee");

    /** Path to this test's temporary database file. */
    protected Path dbFile;

    /** An open connection to it, made with the app's own Database.connect (foreign keys on). */
    protected Connection conn;

    @BeforeEach
    protected void createTestDatabase() throws IOException, SQLException {
        dbFile = Files.createTempFile("manager_app_test_", ".db");
        conn = Database.connect(dbFile.toString());
        runScript(SCHEMA_SQL);
        runScript(SEED_SQL);
    }

    @AfterEach
    protected void deleteTestDatabase() throws IOException, SQLException {
        if (conn != null) {
            conn.close();
        }
        if (dbFile != null) {
            Files.deleteIfExists(dbFile);
        }
    }

    /**
     * Runs a .sql file one statement at a time (JDBC runs one statement per call).
     * Skips "--" comment lines and the seed's BEGIN/COMMIT, since the connection
     * is already in auto-commit mode.
     * Note: splits on ';', so a ';' inside a quoted string in the .sql file would break it.
     */
    private void runScript(Path file) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        try (Statement st = conn.createStatement()) {
            for (String part : sql.toString().split(";")) {
                String statement = part.trim();
                String upper = statement.toUpperCase();
                if (statement.isEmpty() || upper.startsWith("BEGIN") || upper.equals("COMMIT")) {
                    continue;
                }
                st.execute(statement);
            }
        }
    }
}