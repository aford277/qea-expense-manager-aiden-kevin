import dao.ApprovalDao;
import dao.ReportDao;
import dao.UserDao;
import db.Database;
import service.ApprovalService;
import service.AuthService;
import service.ReportService;
import java.sql.Connection;
import java.util.Scanner;
import java.time.Clock;
import log.AppLogger;
 
/** Entry point: connect to the database, build the pieces, start the menu. */
public class Main {
 
    public static void main(String[] args) {
        int exitCode;
        try (Connection conn = Database.connect(Database.dbPath());
            Scanner scanner = new Scanner(System.in)) {
 
            AuthService authService = new AuthService(new UserDao(conn));
            ApprovalService approvalService = new ApprovalService(new ApprovalDao(conn), Clock.systemDefaultZone());
            ReportService reportService = new ReportService(new ReportDao(conn), Clock.systemDefaultZone());

            Menu menu = new Menu(scanner, authService, approvalService, reportService);
            exitCode = menu.run();
 
        } catch (Exception e) {
            AppLogger.error("Unexpected error", e);
            System.out.println("Unexpected error. See log.");
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
 