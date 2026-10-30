import dao.UserDao;
import db.Database;
import service.AuthService;
import java.sql.Connection;
import java.util.Scanner;
 
/** Entry point: connect to the database, build the pieces, start the menu. */
public class Main {
 
    public static void main(String[] args) {
        int exitCode;
        try (Connection conn = Database.connect(Database.dbPath());
            Scanner scanner = new Scanner(System.in)) {
 
            UserDao userDao = new UserDao(conn);
            AuthService authService = new AuthService(userDao);
            Menu menu = new Menu(scanner, authService);
            exitCode = menu.run();
 
        } catch (Exception e) {
            System.out.println("Could not start: " + e.getMessage());
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
 