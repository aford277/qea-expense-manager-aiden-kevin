import exception.AuthenticationException;
import exception.AuthorizationException;
import exception.ExpenseManagerException;
import exception.ValidationException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.io.Console;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
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
    private final Console console;
    private final AuthService authService;
    private final ApprovalService approvalService;
    private final ReportService reportService;
    private User manager;

    /** Used by tests: no console, so the password is read from the Scanner like everything else. */
    public Menu(Scanner in, AuthService authService, ApprovalService approvalService, ReportService reportService) {
        this(in, null, authService, approvalService, reportService);
    }

    public Menu(Scanner in, Console console, AuthService authService, ApprovalService approvalService, ReportService reportService) {
        this.in = in;
        this.console = console;
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
            String username = askNonBlank("Username: ", false);
            String password = askNonBlank("Password: ", true);
            
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
                AppLogger.warning(e.getMessage());
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
                AppLogger.warning(e.getMessage());
                System.out.println(e.getMessage());               // expected errors
            } catch (RuntimeException e) {
                AppLogger.error("Unexpected error", e);
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

        AsciiTable table = newTable();
        addHeader(table, "ID", "Employee", "Date", "Category", "Amount", "Description");
        for (Expense e : pending) {
            AT_Row row = table.addRow(
                text(e.id()), text(e.employeeName()), text(e.expenseDate()),
                text(e.category()), text(e.amount()), text(e.description()));
            alignRight(row, 0, 4);
            table.addRule();
        }

        CWC_LongestLine widths = new CWC_LongestLine()
                .add(2, 0)    // ID
                .add(8, 0)    // Employee
                .add(10, 0)   // Date
                .add(8, 0)    // Category
                .add(6, 0)    // Amount
                .add(11, 40); // Description (max 40, then wraps)
        table.getRenderer().setCWC(widths);
        System.out.println(table.render());
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
            System.out.println("Enter a, d, or c.");
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
                AppLogger.warning(e.getMessage());
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

        AsciiTable table = newTable();
        addHeader(table, nameHeader, "Count", "Total", "Approved");
        for (ReportRow r : rows) {
            AT_Row row = table.addRow(
                    text(r.name()), text(r.count()), text(r.totalAmount()), text(r.totalApproved()));
            alignRight(row, 1, 2, 3);
            table.addRule();
        }
        table.getRenderer().setCWC(new CWC_LongestLine());   // each column as wide as its longest value
        System.out.println(table.render());
    }
 
    /** A new table with one space of padding inside each cell. */
    private AsciiTable newTable() {
        AsciiTable table = new AsciiTable();
        table.setPaddingLeftRight(1);
        return table;
    }
 
    /** Adds the top border, the header row, and the line under it. */
    private void addHeader(AsciiTable table, String... headers) {
        table.addRule();
        AT_Row header = table.addRow((Object[]) headers);
        header.setTextAlignment(TextAlignment.CENTER);
        table.addRule();
    }
 
    /** Right-aligns the given columns (0-based) in one row. */
    private void alignRight(AT_Row row, int... columns) {
        for (int c : columns) {
            row.getCells().get(c).getContext().setTextAlignment(TextAlignment.RIGHT);
        }
    }
 
    /**
     * Turns any value into cell text. Important: asciitable treats a null cell as
     * "merge with the next column", so a null value would break the table layout.
     */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Keeps asking the same question until something other than blank/spaces is typed. */
    private String askNonBlank(String prompt, boolean hidden) {
        while (true) {
            String raw = hidden ? askHidden(prompt) : ask(prompt);
            try {
            return InputValidator.validateLoginInput(raw);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
        }
    }

    /** Reads a line without echoing it. Falls back to normal input when there is no console (tests, some IDEs). */
    private String askHidden(String prompt) {
        if (console == null) {
            return ask(prompt);
        }
        char[] typed = console.readPassword(prompt);
        return typed == null ? "" : new String(typed);
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }
}