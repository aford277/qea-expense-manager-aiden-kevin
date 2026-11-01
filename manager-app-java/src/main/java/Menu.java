import exception.AuthenticationException;
import exception.AuthorizationException;
import exception.ExpenseManagerException;
import exception.ValidationException;
import java.util.List;
import java.util.Scanner;
import model.Expense;
import model.User;
import service.AuthService;
import service.ApprovalService;
import validation.InputValidator;

/** All input/output lives here. No business rules — it just calls services. */
public class Menu {

    private final Scanner in;
    private final AuthService authService;
    private final ApprovalService approvalService;
    private User manager;

    public Menu(Scanner in, AuthService authService, ApprovalService approvalService) {
        this.in = in;
        this.authService = authService;
        this.approvalService = approvalService;
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
            String username = ask("Username: ");
            String password = ask("Password: ");
            if (username.isBlank() || password.isBlank()) {
                continue; // empty input re-prompts, doesn't count as a failure
            }
            try {
                return authService.login(username, password);
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
                failed++;
                if (failed == 3) {
                    System.out.println("Too many failed attempts.");
                    return null;
                }
            } catch (AuthorizationException e) {
                System.out.println(e.getMessage());
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
                    case 3 -> System.out.println("(coming soon)");
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

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }
}