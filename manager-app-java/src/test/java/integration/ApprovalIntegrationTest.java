package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dao.ApprovalDao;
import exception.InvalidStateException;
import integration.support.DatabaseTest;
import integration.support.DbQueries;
import integration.support.DbQueries.ApprovalRow;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import service.ApprovalService;

/**
 * Integration tests for listing and deciding expenses, against a real temporary database.
 * Seed facts used here (sql/seed.sql):
 *   10 pending expenses; oldest is #1 (2026-10-01), newest is #6 (2026-10-28).
 *   #25 is pending; #7 is approved by manager 1 with comment "OK" on 2026-09-05.
 */
class ApprovalIntegrationTest extends DatabaseTest {

    private static final Path VALIDATION_SQL = Path.of("..", "sql", "validation");

    private ApprovalService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalService(new ApprovalDao(conn), SEED_CLOCK);
    }

    // TM-01: FR-M1

    @Test
    @Tag("smoke")
    @DisplayName("Covers: FR-M1 (TM-01) - lists only the 10 pending expenses, oldest first")
    void listsPendingOldestFirst() {
        List<Integer> ids = service.listPending(MANAGER).stream().map(Expense::id).toList();

        // Sorted by expense_date ascending (ties by id)
        assertEquals(List.of(1, 24, 12, 3, 13, 4, 17, 19, 25, 6), ids);
    }

    @Test
    @DisplayName("Covers: FR-M1 (TM-01) - each row has ID, employee name, date, category, amount, description")
    void pendingRowHasAllColumns() {
        Expense first = service.listPending(MANAGER).get(0);

        assertEquals(1, first.id());
        assertEquals("Jordan Lee", first.employeeName());          // joined from users
        assertEquals("2026-10-01", first.expenseDate());
        assertEquals("Travel", first.category());                  // joined from categories
        assertEquals(new BigDecimal("0.01"), first.amount());      // 2 decimals (FR-S5)
        assertEquals("Boundary: minimum amount", first.description());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M1, FR-S2 (TM-01) - a decided expense drops off the pending list")
    void decidedExpenseLeavesPendingList() {
        service.approve(MANAGER, 25, "OK");

        List<Integer> ids = service.listPending(MANAGER).stream().map(Expense::id).toList();
        assertEquals(9, ids.size());
        assertTrue(!ids.contains(25), "expense 25 should no longer be pending");
    }

    // TM-03: FR-M2, VR-06

    @Test
    @Tag("smoke")
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - manager approves a pending expense without a comment")
    void approveWithoutComment() throws SQLException {
        service.approve(MANAGER, 25, "");   // what the menu passes when the manager just presses Enter

        ApprovalRow row = DbQueries.approvalFor(conn, 25);
        assertEquals("approved", row.status());
        assertEquals(MANAGER.id(), row.reviewerId());
        assertEquals(SEED_TODAY.toString(), row.reviewDate());   // today from the fixed Clock (4.3)
        assertTrue(row.comment() == null, "a blank approval comment is stored as NULL");
    }

    @ParameterizedTest(name = "comment [{0}] is stored as [{1}]")
    @CsvSource({
            "'',               ",              // empty -> NULL (2nd column blank = null)
            "'   ',            ",              // spaces only -> NULL
            "OK,               OK",            // VR-06 valid example
            "'  Looks fine  ', Looks fine"     // trimmed before saving
    })
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - approve saves status, reviewer, comment and today's date")
    void approveIsSavedInDatabase(String comment, String expectedStored) throws SQLException {
        service.approve(MANAGER, 25, comment);

        ApprovalRow row = DbQueries.approvalFor(conn, 25);
        assertEquals("approved", row.status());
        assertEquals(MANAGER.id(), row.reviewerId());
        assertEquals("2026-10-28", row.reviewDate());   // today from SEED_CLOCK
        assertEquals(expectedStored, row.comment());
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-M2, VR-06 (TM-03) - a 200-character approval comment is saved in full")
    void approvalCommentOf200IsSaved() throws SQLException {
        String comment = "a".repeat(200);

        service.approve(MANAGER, 25, comment);

        assertEquals(comment, DbQueries.approvalFor(conn, 25).comment());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M2, FR-S2 - approving an already-approved expense changes nothing")
    void approvingDecidedExpenseChangesNothing() throws SQLException {
        assertThrows(InvalidStateException.class, () -> service.approve(OTHER_MANAGER, 7, "Again"));

        ApprovalRow row = DbQueries.approvalFor(conn, 7);   // still exactly as seeded
        assertEquals("approved", row.status());
        assertEquals(1, row.reviewerId());
        assertEquals("OK", row.comment());
        assertEquals("2026-09-05", row.reviewDate());
    }

    // FR-S5: shared data contract
    // What the Manager App writes must follow Section 8.3, so the Employee App reads it correctly.
    // Automates the Java side of the cross-app cases XW-01, XW-02 and XW-05.

    @ParameterizedTest(name = "{0} returns 0 rows")
    @CsvSource({
            "03_pending_with_review_data.sql",
            "04_decided_missing_review_data.sql",
            "05_invalid_status_contract.sql"
    })
    @DisplayName("Covers: FR-S5, FR-S2 - rows written by Java pass the shared SQL validation queries")
    void javaDecisionsKeepTheDataContract(String queryFile) throws IOException, SQLException {
        // Java writes one decision of each kind, by both managers
        service.approve(MANAGER, 25, "");                        // blank comment -> NULL
        service.deny(OTHER_MANAGER, 24, "Missing receipt");

        assertEquals(0, DbQueries.countRows(conn, VALIDATION_SQL.resolve(queryFile)));
    }

    @Test
    @DisplayName("Covers: FR-S5, XW-02 - a denial is stored exactly as the Python app expects to read it")
    void denialMatchesSharedContract() throws SQLException {
        service.deny(OTHER_MANAGER, 24, "  Missing receipt  ");

        ApprovalRow row = DbQueries.approvalFor(conn, 24);
        assertEquals("denied", row.status());                  // lowercase status
        assertEquals(OTHER_MANAGER.id(), row.reviewerId());    // a manager
        assertEquals("Missing receipt", row.comment());        // exact text, trimmed
        assertTrue(row.reviewDate().matches("\\d{4}-\\d{2}-\\d{2}"), "review_date must be YYYY-MM-DD");
        assertEquals("2026-10-28", row.reviewDate());
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-S5, XW-05 - boundary values written by the Python side are read exactly")
    void boundaryValuesReadExactly() {
        List<Expense> pending = service.listPending(MANAGER);
        Expense minAmount = pending.stream().filter(e -> e.id() == 1).findFirst().orElseThrow();
        Expense longDescription = pending.stream().filter(e -> e.id() == 3).findFirst().orElseThrow();

        assertEquals(new BigDecimal("0.01"), minAmount.amount());
        assertEquals(200, longDescription.description().length());
        assertEquals(new BigDecimal("75.00"), longDescription.amount());   // shown with 2 decimals
    }
}