import exception.AuthenticationException;
import exception.AuthorizationException;
import model.User;
import service.AuthService;
import java.util.Scanner;

/** All input/output lives here. No business rules — it just calls services. */
public class Menu {

    private final Scanner in;
    private final AuthService authService;

    public Menu(Scanner in, AuthService authService) {
        this.in = in;
        this.authService = authService;
    }

    /** Runs the app. Returns the exit code (0 = normal, 1 = error). */
    public int run() {
        User manager = login();
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
            String choice = ask("Choose an option: ").trim();

            switch (choice) {
                case "1" -> System.out.println("(coming soon)");
                case "2" -> System.out.println("(coming soon)");
                case "3" -> System.out.println("(coming soon)");
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Enter a number from the menu.");
            }
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }
}