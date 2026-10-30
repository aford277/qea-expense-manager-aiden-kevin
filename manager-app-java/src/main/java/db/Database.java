package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

public final class Database {
    public static Connection connect(String path) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5000);
        return DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
    }

    public static String dbPath() {
        String path = System.getenv("EM_DB_PATH");
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("EM_DB_PATH is not set.");
        }
        return path;
    }
}
// Usage: try (Connection conn = Database.connect(Database.dbPath())) { ... }