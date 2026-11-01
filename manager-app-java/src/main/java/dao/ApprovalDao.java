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
import model.Expense;

/** SQL for the `expenses` and `approvals` tables (spec §8.2). */
public class ApprovalDao {

    private final Connection conn;

    public ApprovalDao(Connection conn) {
        this.conn = conn;
    }

    /** FR-M1: every pending expense with its employee and category names, oldest first. */
    public List<Expense> findPending() {
        String sql = """
                SELECT e.id, u.full_name, e.expense_date, c.name AS category, e.amount, e.description
                FROM expenses e
                JOIN approvals a  ON a.expense_id = e.id
                JOIN users u      ON u.id = e.user_id
                JOIN categories c ON c.id = e.category_id
                WHERE a.status = 'pending'
                ORDER BY e.expense_date ASC, e.id ASC
                """;
        List<Expense> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("expense_date"),
                        rs.getString("category"),
                        // spec §8.3: exact conversion so totals match the Python app
                        BigDecimal.valueOf(rs.getDouble("amount")).setScale(2, RoundingMode.HALF_UP),
                        rs.getString("description")));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load pending expenses.", e);
        }
    }

    public String findStatus(int expenseId) {
        String sql = "SELECT status FROM approvals WHERE expense_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not look up the expense.", e);
        }
    }
 
    public boolean saveDecision(int expenseId, String status, int reviewerId, String comment, LocalDate reviewDate) {
        String sql = """
                UPDATE approvals
                SET status = ?, reviewer_id = ?, comment = ?, review_date = ?
                WHERE expense_id = ? AND status = 'pending'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reviewerId);
            ps.setString(3, comment);                 // null is allowed (no comment)
            ps.setString(4, reviewDate.toString());   // LocalDate.toString() is YYYY-MM-DD (spec §8.3)
            ps.setInt(5, expenseId);
            return ps.executeUpdate() == 1;           // number of rows changed
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the decision.", e);
        }
    }
}