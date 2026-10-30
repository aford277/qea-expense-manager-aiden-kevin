package dao;
 
import model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import exception.DatabaseException;
 
/** The only place that writes SQL for the `users` table. */
public class UserDao {
 
    private final Connection conn;
 
    public UserDao(Connection conn) {
        this.conn = conn;
    }
 
    /** Returns the matching user, or Optional.empty() if the username/password is wrong. */
    public Optional<User> findByCredentials(String username, String password) {
        String sql = "SELECT id, username, role, full_name FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("full_name")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error during login.", e);
        }
    }
}