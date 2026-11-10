// No package: Menu lives in Java's default package, and only classes in the
// default package can use it. So this file goes directly in src/test/java/.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dao.ApprovalDao;
import dao.ReportDao;
import dao.UserDao;
import integration.support.DatabaseTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import service.ApprovalService;
import service.AuthService;
import service.ReportService;

/**
 * Menu-flow integration tests: the real Menu with scripted keyboard input,
 * real services, and a real temporary database (from DatabaseTest).
 *
 * TS-03 reads logs/manager_app.log, which AppLogger writes relative to where
 * `mvn test` runs (manager-app-java/). The log file is appended to, never cleared,
 * so each test only looks at the lines written during that test.
 */
class ManagerMenuLoggingTest extends DatabaseTest {

    private static final Path LOG_FILE = Path.of("logs", "manager_app.log");

    /** "2026-11-10 13:14:05 LEVEL logger: message" - time, level and message (FR-S3). */
    private static final String LINE_START = "(?m)^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} ";

    private final PrintStream realOut = System.out;
    private ByteArrayOutputStream screen;
    private long logSizeBefore;

    @BeforeEach
    void captureScreenAndLogPosition() throws IOException {
        screen = new ByteArrayOutputStream();
        System.setOut(new PrintStream(screen, true, StandardCharsets.UTF_8));
        logSizeBefore = Files.exists(LOG_FILE) ? Files.size(LOG_FILE) : 0;
    }

    @AfterEach
    void restoreScreen() {
        System.setOut(realOut);
    }

    /** Runs the Manager App menu with the given lines typed in. Returns the exit code. */
    private int runMenu(String... typedLines) {
        String input = String.join("\n", typedLines) + "\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        Menu menu = new Menu(scanner,
                new AuthService(new UserDao(conn)),
                new ApprovalService(new ApprovalDao(conn), SEED_CLOCK),
                new ReportService(new ReportDao(conn), SEED_CLOCK));
        return menu.run();
    }

    /** Everything written to the log file since this test started. */
    private String newLogText() throws IOException {
        byte[] all = Files.readAllBytes(LOG_FILE);
        int start = (int) Math.min(logSizeBefore, all.length);
        return new String(all, start, all.length - start, StandardCharsets.UTF_8);
    }

    private static boolean hasLine(String text, String levelAndMessageRegex) {
        return Pattern.compile(LINE_START + levelAndMessageRegex).matcher(text).find();
    }

    // TS-03: FR-S3

    @Test
    @Tag("smoke")
    @DisplayName("Covers: FR-S3 (TS-03) - a successful login is logged with time and level; the password is not")
    void successfulLoginIsLoggedWithoutPassword() throws IOException {
        int exitCode = runMenu("mgr_asha", "asha123", "0");   // log in, then 0 = Exit

        assertEquals(0, exitCode);
        String log = newLogText();
        assertTrue(hasLine(log, "INFO \\S+: Login successful: mgr_asha"),
                "expected an INFO login line in the log, got:\n" + log);
        assertTrue(!log.contains("asha123"), "the password must never be written to the log");
        assertTrue(!screen.toString(StandardCharsets.UTF_8).contains("asha123"),
                "the password must never be printed");
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-S3 (TS-03) - a FAILED login is logged; the wrong password appears nowhere in the log")
    void failedLoginIsLoggedWithoutPassword() throws IOException {
        // A unique wrong password, so we can search the WHOLE log file for it
        String wrongPassword = "Wr0ng-" + UUID.randomUUID();

        runMenu("mgr_asha", wrongPassword, "mgr_asha", "asha123", "0");   // fail once, then log in and exit

        assertTrue(hasLine(newLogText(), "WARNING \\S+: Failed login attempt for username: mgr_asha"),
                "expected a WARNING line for the failed login");
        String wholeLog = Files.readString(LOG_FILE, StandardCharsets.UTF_8);
        assertTrue(!wholeLog.contains(wrongPassword), "the wrong password must never be written to the log");
    }

    // TA-03: FR-A3 lockout
    // The lockout logic lives in Menu, so it is tested here through the same scripted-input setup.

    @Test
    @Tag("negative")
    @Tag("boundary")
    @DisplayName("Covers: FR-A3 (TA-03) - 3 failed logins in a row print the message and exit with code 1")
    void threeFailedLoginsExitWithCode1() {
        int exitCode = runMenu(
                "mgr_asha", "wrong1",
                "mgr_asha", "wrong1",    // same wrong password again
                "mgr_asha", "wrong2");   // a different wrong password

        assertEquals(1, exitCode);
        String output = screen.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid username or password."));
        assertTrue(output.contains("Too many failed attempts."));
        assertTrue(!output.contains("Welcome,"), "nobody should be logged in");
    }
}