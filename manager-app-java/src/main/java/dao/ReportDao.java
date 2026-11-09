package dao;

import exception.DatabaseException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.ReportRow;

public class ReportDao {

    private final Connection conn;

    public ReportDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * One row per employee who has expenses.
     * Grouped by u.id (unique) so two employees with the same name stay separate.
     */
    public List<ReportRow> byEmployee() {
        String sql = """
                SELECT u.full_name AS name,
                       COUNT(*) AS expense_count,
                       SUM(e.amount) AS total_amount,
                       SUM(CASE WHEN a.status = 'approved' THEN e.amount ELSE 0 END) AS total_approved
                FROM expenses e
                JOIN approvals a ON a.expense_id = e.id
                JOIN users u     ON u.id = e.user_id
                GROUP BY u.id, u.full_name
                ORDER BY u.full_name, u.id
                """;
        return runReport(sql);
    }

    /** One row per category that has expenses. */
    public List<ReportRow> byCategory() {
        String sql = """
                SELECT c.name AS name,
                       COUNT(*) AS expense_count,
                       SUM(e.amount) AS total_amount,
                       SUM(CASE WHEN a.status = 'approved' THEN e.amount ELSE 0 END) AS total_approved
                FROM expenses e
                JOIN approvals a  ON a.expense_id = e.id
                JOIN categories c ON c.id = e.category_id
                GROUP BY c.id, c.name
                ORDER BY c.name
                """;
        return runReport(sql);
    }

    public List<ReportRow> byEmployeeInRange(LocalDate start, LocalDate end) {
        String sql = """
                SELECT u.full_name AS name,
                       COUNT(*) AS expense_count,
                       SUM(e.amount) AS total_amount,
                       SUM(CASE WHEN a.status = 'approved' THEN e.amount ELSE 0 END) AS total_approved
                FROM expenses e
                JOIN approvals a ON a.expense_id = e.id
                JOIN users u     ON u.id = e.user_id
                WHERE e.expense_date BETWEEN ? AND ?
                GROUP BY u.id, u.full_name
                ORDER BY u.full_name, u.id
                """;

        return runReport(sql, start.toString(), end.toString());
    }

    /** Runs a report query and turns each row into a ReportRow. Both queries use the same column names. */
    private List<ReportRow> runReport(String sql, String... params) {
        List<ReportRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);  
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReportRow(
                            rs.getString("name"),
                            rs.getInt("expense_count"),
                            toMoney(rs.getDouble("total_amount")),
                            toMoney(rs.getDouble("total_approved"))));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load the report.", e);
        }
    }

    /** spec §8.3: exact conversion so totals match the Python app. */
    private BigDecimal toMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}