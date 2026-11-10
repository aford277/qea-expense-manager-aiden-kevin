package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dao.ApprovalDao;
import dao.ReportDao;
import integration.support.DatabaseTest;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import model.ReportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import service.ApprovalService;
import service.ReportService;

/**
 * Integration tests for the reports (FR-M3), against a real temporary database.
 * Expected numbers were worked out by hand from sql/seed.sql
 * (25 expenses; approved = #2, 5, 7, 9, 10, 15, 16, 21).
 */
class ReportIntegrationTest extends DatabaseTest {

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(new ReportDao(conn), SEED_CLOCK);
    }

    /** Finds the report row with this name, or fails the test if it is missing. */
    private static ReportRow rowNamed(List<ReportRow> rows, String name) {
        return rows.stream().filter(r -> r.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No report row named " + name));
    }

    // ---------------------------------------------------------------- TM-04: by employee

    @ParameterizedTest(name = "{0}: {1} expenses, total {2}, approved {3}")
    @CsvSource({
            "Jordan Lee,   7, 1190.16,  852.00",
            "Maria Santos, 6, 12407.99, 11200.00",
            "Omar Haddad,  6, 11680.82, 1499.99",
            "Wei Chen,     6, 1152.65,  420.20"
    })
    @Tag("smoke")
    @DisplayName("Covers: FR-M3, FR-S5 (TM-04) - report by employee: count, total and approved total")
    void reportByEmployee(String name, int count, String total, String approved) {
        ReportRow row = rowNamed(service.byEmployee(MANAGER), name);

        assertEquals(count, row.count());
        // BigDecimal("...") keeps 2 decimals, so this also checks the 2-decimal rule
        assertEquals(new BigDecimal(total), row.totalAmount());
        assertEquals(new BigDecimal(approved), row.totalApproved());
    }

    @Test
    @DisplayName("Covers: FR-M3 (TM-04) - report by employee has one row per employee, sorted by name")
    void reportByEmployeeRowsAndOrder() {
        List<String> names = service.byEmployee(MANAGER).stream().map(ReportRow::name).toList();

        // Managers have no expenses, so they don't appear
        assertEquals(List.of("Jordan Lee", "Maria Santos", "Omar Haddad", "Wei Chen"), names);
    }

    // ---------------------------------------------------------------- TM-04: by category

    @ParameterizedTest(name = "{0}: {1} expenses, total {2}, approved {3}")
    @CsvSource({
            "Lodging,         4, 11884.49, 909.00",
            "Meals,           5, 193.65,   117.60",
            "Office Supplies, 4, 417.14,   0.00",
            "Other,           3, 99.99,    0.00",
            "Training,        4, 12249.99, 11499.99",
            "Travel,          5, 1586.36,  1445.60"
    })
    @Tag("smoke")
    @DisplayName("Covers: FR-M3, FR-S5 (TM-04) - report by category: count, total and approved total")
    void reportByCategory(String name, int count, String total, String approved) {
        ReportRow row = rowNamed(service.byCategory(MANAGER), name);

        assertEquals(count, row.count());
        assertEquals(new BigDecimal(total), row.totalAmount());
        assertEquals(new BigDecimal(approved), row.totalApproved());
    }

    @Test
    @DisplayName("Covers: FR-M3 (TM-04) - report by category has one row per category, sorted by name")
    void reportByCategoryRowsAndOrder() {
        List<String> names = service.byCategory(MANAGER).stream().map(ReportRow::name).toList();

        assertEquals(List.of("Lodging", "Meals", "Office Supplies", "Other", "Training", "Travel"), names);
    }

    // ---------------------------------------------------------------- TM-04: the 2-decimal rule

    /**
     * Replaces the seed expenses with two approved Meals expenses for Jordan: 0.10 and 0.20.
     * In SQLite (REAL) 0.10 + 0.20 = 0.30000000000000004, so this proves the app rounds to 2 decimals.
     */
    private void useOnlyTenAndTwentyCents() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM approvals");
            st.execute("DELETE FROM expenses");
            st.execute("""
                    INSERT INTO expenses (id, user_id, category_id, amount, description, expense_date, created_at) VALUES
                        (101, 3, 2, 0.10, 'Gum',   '2026-10-20', '2026-10-20 09:00:00'),
                        (102, 3, 2, 0.20, 'Mints', '2026-10-21', '2026-10-21 09:00:00')
                    """);
            st.execute("""
                    INSERT INTO approvals (expense_id, status, reviewer_id, comment, review_date) VALUES
                        (101, 'approved', 1, NULL, '2026-10-22'),
                        (102, 'approved', 1, NULL, '2026-10-22')
                    """);
        }
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-M3, FR-S5 (TM-04) - by employee: 0.10 + 0.20 shows as 0.30")
    void byEmployeeRoundsToTwoDecimals() throws SQLException {
        useOnlyTenAndTwentyCents();

        List<ReportRow> rows = service.byEmployee(MANAGER);

        assertEquals(1, rows.size());
        assertEquals("Jordan Lee", rows.get(0).name());
        assertEquals(2, rows.get(0).count());
        assertEquals(new BigDecimal("0.30"), rows.get(0).totalAmount());
        assertEquals(new BigDecimal("0.30"), rows.get(0).totalApproved());
    }

    @Test
    @Tag("boundary")
    @DisplayName("Covers: FR-M3, FR-S5 (TM-04) - by category: 0.10 + 0.20 shows as 0.30")
    void byCategoryRoundsToTwoDecimals() throws SQLException {
        useOnlyTenAndTwentyCents();

        List<ReportRow> rows = service.byCategory(MANAGER);

        assertEquals(1, rows.size());
        assertEquals("Meals", rows.get(0).name());
        assertEquals(2, rows.get(0).count());
        assertEquals(new BigDecimal("0.30"), rows.get(0).totalAmount());
        assertEquals(new BigDecimal("0.30"), rows.get(0).totalApproved());
    }

    // ---------------------------------------------------------------- TM-04: both reports agree

    @Test
    @DisplayName("Covers: FR-M3, XW-06 (TM-04) - both reports add up to the same grand total")
    void bothReportsHaveSameGrandTotal() {
        BigDecimal byEmployee = service.byEmployee(MANAGER).stream()
                .map(ReportRow::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal byCategory = service.byCategory(MANAGER).stream()
                .map(ReportRow::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("26431.62"), byEmployee);
        assertEquals(byEmployee, byCategory);
    }

    @Test
    @DisplayName("Covers: FR-M3, FR-M2 (TM-04) - an approval made in Java shows up in the approved total")
    void newApprovalChangesApprovedTotal() {
        // Jordan's #25 (300.00) is pending; approve it through the real service
        new ApprovalService(new ApprovalDao(conn), SEED_CLOCK).approve(MANAGER, 25, "OK");

        ReportRow jordan = rowNamed(service.byEmployee(MANAGER), "Jordan Lee");
        assertEquals(new BigDecimal("1190.16"), jordan.totalAmount());     // total unchanged
        assertEquals(new BigDecimal("1152.00"), jordan.totalApproved());   // 852.00 + 300.00
    }
}