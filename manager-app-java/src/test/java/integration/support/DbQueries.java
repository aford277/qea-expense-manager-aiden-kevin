package integration.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Small read-only helpers for integration tests.
 * Tests use these to look straight at the database after a service call,
 * so they check what was really stored, not just what the service returned.
 */
public final class DbQueries {

    private DbQueries() { }

    /** One row of the approvals table. reviewerId is null when the column is NULL. */
    public record ApprovalRow(String status, Integer reviewerId, String comment, String reviewDate) { }

    /** Reads the approvals row for an expense, or null if there is none. */
    public static ApprovalRow approvalFor(Connection conn, int expenseId) throws SQLException {
        String sql = "SELECT status, reviewer_id, comment, review_date FROM approvals WHERE expense_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int reviewer = rs.getInt("reviewer_id");
                Integer reviewerId = rs.wasNull() ? null : reviewer;   // check wasNull right after getInt
                return new ApprovalRow(
                        rs.getString("status"),
                        reviewerId,
                        rs.getString("comment"),
                        rs.getString("review_date"));
            }
        }
    }

    /**
     * Runs one of the sql/validation/*.sql queries and returns how many rows it found.
     * Each of those queries should return zero rows when the data is correct (Section 9.3).
     */
    public static int countRows(Connection conn, Path sqlFile) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(sqlFile, StandardCharsets.UTF_8)) {
            if (!line.trim().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        String query = sql.toString().trim();
        if (query.endsWith(";")) {
            query = query.substring(0, query.length() - 1);
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            int rows = 0;
            while (rs.next()) {
                rows++;
            }
            return rows;
        }
    }
}
