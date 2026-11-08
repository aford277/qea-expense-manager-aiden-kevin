import exception.AuthenticationException;
import exception.AuthorizationException;
import exception.ExpenseManagerException;
import exception.ValidationException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import model.Expense;
import model.User;
import model.ReportRow;
import service.AuthService;
import service.ApprovalService;
import service.ReportService;
import validation.InputValidator;
import log.AppLogger;

/** All input/output lives here. No business rules — it just calls services. */
public class Menu {

    private final Scanner in;
    private final AuthService authService;
    private final ApprovalService approvalService;
    private final ReportService reportService;
    private User manager;

    public Menu(Scanner in, AuthService authService, ApprovalService approvalService, ReportService reportService) {
        this.in = in;
        this.authService = authService;
        this.approvalService = approvalService;
        this.reportService = reportService;
    }

    /** Runs the app. Returns the exit code (0 = normal, 1 = error). */
    public int run() {
        manager = login();
        if (manager == null) {
            return 1;
        }
        System.out.println("Welcome, " + manager.fullName());
        mainLoop();
        return 0;
    }

    // ---- Login (FR-A1, FR-A2, FR-A3) ----
    private User login() {
        int failed = 0;
        while (true) {
            String username = askNonBlank("Username: ");
            String password = askNonBlank("Password: ");
            
            try {
                User user = authService.login(username, password);
                AppLogger.info("Login successful: " + user.username());
                return user;
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
                failed++;
                AppLogger.warning("Failed login attempt for username: " + username);   
                if (failed == 3) {
                    System.out.println("Too many failed attempts.");
                    AppLogger.warning("Too many failed login attempts. Exiting.");
                    return null;
                }
            } catch (AuthorizationException e) {
                System.out.println(e.getMessage());
                AppLogger.warning("Access denied (wrong role) for username: " + username);
                return null;
            }
        }
    }

    // ---- Main menu (spec §6.3) ----
    private void mainLoop() {
        while (true) {
            System.out.println();
            System.out.println("1) List pending expenses");
            System.out.println("2) Review an expense");
            System.out.println("3) Reports");
            System.out.println("0) Exit");

            int choice;

            try {
                choice = InputValidator.validateMenuChoice(ask("Choose an option: "), 0, 1, 2, 3);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> listPending();
                    case 2 -> reviewExpense();
                    case 3 -> reportsMenu();
                    case 0 -> {
                        System.out.println("Goodbye.");
                        return;
                    }
                }
            } catch (ExpenseManagerException e) {
                System.out.println(e.getMessage());               // expected errors
            } catch (RuntimeException e) {
                System.out.println("Unexpected error. See log."); // anything else
            }
        }
    }

    private void listPending() {
        List<Expense> pending = approvalService.listPending(manager);
        if (pending.isEmpty()) {
            System.out.println("No pending expenses.");
            return;
        }
        String format = "%-5s %-20s %-10s %-16s %10s  %s%n";
        System.out.printf(format, "ID", "Employee", "Date", "Category", "Amount", "Description");
        for (Expense e : pending) {
            System.out.printf(format,
                    e.id(), e.employeeName(), e.expenseDate(), e.category(), e.amount(), e.description());
        }
    }

    private void reviewExpense() {
        // VR-08: a bad ID throws ValidationException; mainLoop prints it and shows the menu again
        int expenseId = InputValidator.validateExpenseId(ask("Expense ID: "));
 
        // Missing, deleted, or already decided? Stop now (NotFound / InvalidState, caught in mainLoop)
        approvalService.checkCanDecide(manager, expenseId);
 
        String decision;
        while (true) {
            decision = ask("Approve or deny? (a/d, c to cancel): ").trim().toLowerCase();
            if (decision.equals("c")){
                return;
            } else if (decision.equals("a") || decision.equals("d")) {
                break;
            }
            System.out.println("Enter a or d.");
        }
 
        // Ask for the comment until it passes (if comment is)
        while (true) {
            String prompt = decision.equals("a")
                    ? "Comment (optional, press Enter to skip): "
                    : "Reason for denial (5-200 characters): ";
            String comment = ask(prompt);
            try {
                if (decision.equals("a")) {
                    approvalService.approve(manager, expenseId, comment);
                    System.out.println("Expense " + expenseId + " approved.");
                } else {
                    approvalService.deny(manager, expenseId, comment);
                    System.out.println("Expense " + expenseId + " denied.");
                }
                return;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());   // bad comment: show the rule and ask again
            }
        }
    }

    private void reportsMenu() {
        System.out.println();
        System.out.println("1) By employee");
        System.out.println("2) By category");
        System.out.println("3) By employee with date range");
        System.out.println("0) Back");
        int choice = InputValidator.validateMenuChoice(ask("Choose a report: "), 0, 1, 2, 3); // VR-09
 
        if (choice == 0) {
            return;   // back to main menu
        }
 
        // Remember which report was shown, so it can be exported afterwards
        String type;            // used in the CSV filename
        String nameHeader;      // first column title on screen
        List<ReportRow> rows;
        if (choice == 1) {
            type = "employee";
            nameHeader = "Employee";
            rows = reportService.byEmployee(manager);
        } else if (choice == 2) {
            type = "category";
            nameHeader = "Category";
            rows = reportService.byCategory(manager);
        } else {
            String start = ask("Start date (YYYY-MM-DD): ");
            String end = ask("End date (YYYY-MM-DD): ");
            type = "employee_range";
            nameHeader = "Employee";
            rows = reportService.byEmployeeInRange(manager, start, end);
        }
 
        printReport(nameHeader, rows);
        if (rows.isEmpty()) {
            return;   // nothing to export
        }
 
        // Export the report that's on screen. A write failure throws ExpenseManagerException
        // mainLoop prints it and the report stays above.
        String answer = ask("Export to CSV? (y/n): ").trim();
        if (answer.equalsIgnoreCase("y")) {
            Path file = reportService.exportCsv(manager, type, rows);
            System.out.println("Report saved to " + file);
        }
    }
 
    /** Prints a report table. nameHeader is the first column's title ("Employee" or "Category"). */
    private void printReport(String nameHeader, List<ReportRow> rows) {
        if (rows.isEmpty()) {
            System.out.println("No data for the selected criteria.");   // exact text from FR-M3
            return;
        }
        String format = "%-24s %6s %14s %14s%n";
        System.out.printf(format, nameHeader, "Count", "Total", "Approved");
        for (ReportRow r : rows) {
            System.out.printf(format, r.name(), r.count(), r.totalAmount(), r.totalApproved());
        }
    }

    /** Keeps asking the same question until something other than blank/spaces is typed. */
    private String askNonBlank(String prompt) {
        while (true) {
            String value = ask(prompt);
            if (!value.isBlank()) {
                return value;
            }
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }
}