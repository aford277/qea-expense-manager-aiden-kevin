package log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * All Manager App logging lives here.
 *
 * Writes to logs/manager_app.log, one line per entry,
 *   2026-11-08 12:15:03 INFO manager_app.service.ApprovalService: Expense 7 approved by mgr_asha
 *
 * Usage anywhere in the app:
 *   AppLogger.info("...");
 *   AppLogger.warning("...");
 *   AppLogger.error("...", exception);   // also writes the stack trace to the file
 *
 * Never pass a password into any of these methods.
 */
public final class AppLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + "/manager_app.log";
    private static final String APP_NAME = "manager_app";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINE_FORMAT = "%s %s %s: %s%n"; // time, level, name, message

    private static final Logger LOGGER = createLogger();

    private AppLogger() { }

    /** Normal events: logins, decisions. */
    public static void info(String message) {
        LOGGER.logp(Level.INFO, callerName(), null, message);
    }

    /** Expected problems: failed logins, validation errors, wrong-state errors. */
    public static void warning(String message) {
        LOGGER.logp(Level.WARNING, callerName(), null, message);
    }

    /** Errors without an exception object. */
    public static void error(String message) {
        LOGGER.logp(Level.SEVERE, callerName(), null, message);
    }

    /** Unexpected errors: the stack trace goes to the log file, never the screen. */
    public static void error(String message, Throwable cause) {
        LOGGER.logp(Level.SEVERE, callerName(), null, message, cause);
    }

    /** Finds the class that called AppLogger, e.g. "service.ApprovalService". */
    private static String callerName() {
        return StackWalker.getInstance().walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> !name.equals(AppLogger.class.getName()))
                .findFirst()
                .orElse("unknown"));
    }

    private static Logger createLogger() {
        Logger logger = Logger.getLogger(APP_NAME);
        // Stop Java from also printing every log line to the console.
        logger.setUseParentHandlers(false);
        try {
            Files.createDirectories(Path.of(LOG_DIR));
            FileHandler file = new FileHandler(LOG_FILE, true); // true = append, don't overwrite
            file.setFormatter(new LineFormatter());
            logger.addHandler(file);
        } catch (IOException | SecurityException e) {
            // If the file can't be opened, log calls simply do nothing.
        }
        return logger;
    }

    /** Turns one log entry into one text line. */
    private static final class LineFormatter extends Formatter {
        @Override
        public String format(LogRecord entry) {
            String time = LocalDateTime
                    .ofInstant(entry.getInstant(), ZoneId.systemDefault())
                    .format(TIME_FORMAT);
            
            String level = entry.getLevel() == Level.SEVERE ? "ERROR" : entry.getLevel().getName();
            String name = APP_NAME + "." + entry.getSourceClassName();

            StringBuilder line = new StringBuilder(
                    String.format(LINE_FORMAT, time, level, name, entry.getMessage()));
            if (entry.getThrown() != null) {
                StringWriter trace = new StringWriter();
                entry.getThrown().printStackTrace(new PrintWriter(trace));
                line.append(trace);
            }
            return line.toString();
        }
    }
}