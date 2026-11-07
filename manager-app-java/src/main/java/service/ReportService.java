package service;

import dao.ReportDao;
import exception.AuthorizationException;
import exception.ExpenseManagerException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.ReportRow;
import model.User;
import validation.DateRangeValidator;

/** Reports for managers (spec FR-M3). Date range report (a Should) can be added here later. */
public class ReportService {

    private static final Path REPORTS_FOLDER = Path.of("reports");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReportDao reportDao;
    private final Clock clock;

    public ReportService(ReportDao reportDao, Clock clock) {
        this.reportDao = reportDao;
        this.clock = clock;
    }

    /** Totals per employee. Empty list if there is no data. */
    public List<ReportRow> byEmployee(User manager) {
        requireManager(manager);
        return reportDao.byEmployee();
    }

    /** Totals per category. Empty list if there is no data. */
    public List<ReportRow> byCategory(User manager) {
        requireManager(manager);
        return reportDao.byCategory();
    }

    /** Totals per employee in date range. Empty list if there is no data. */
    public List<ReportRow> byEmployeeInRange(User manager, String startInput, String endInput) {
        requireManager(manager);
        LocalDate start = DateRangeValidator.validateDate(startInput);
        LocalDate end = DateRangeValidator.validateDate(endInput);
        DateRangeValidator.validateRange(start, end);
        return reportDao.byEmployeeInRange(start, end);
    }

    /** Write the shown report to reports/ with a header row. Returns the path of the new file. */
    public Path exportCsv(User manager, String type, List<ReportRow> rows) {
        requireManager(manager);
        String timestamp = LocalDateTime.now(clock).format(FILE_TIMESTAMP);
        Path file = REPORTS_FOLDER.resolve(type + "_" + timestamp + ".csv");
 
        try {
            Files.createDirectories(REPORTS_FOLDER);   // makes reports/ if it doesn't exist yet
            // try-with-resources closes the file even if writing fails halfway
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write("name,count,total_amount,total_approved");
                writer.newLine();
                for (ReportRow r : rows) {
                    writer.write(csvValue(r.name()) + ","
                            + r.count() + ","
                            + r.totalAmount().toPlainString() + ","
                            + r.totalApproved().toPlainString());
                    writer.newLine();
                }
            }
            return file;
        } catch (IOException e) {
            throw new ExpenseManagerException("Could not save the CSV file. The report is still shown above.", e);
        }
    }

    /** Wraps a value in quotes if it contains a comma or quote, so it can't break the CSV columns. */
    private String csvValue(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** A non-manager calling reports gets AuthorizationException. */
    private void requireManager(User user) {
        if (user == null || !user.isManager()) {
            throw new AuthorizationException("Only managers can perform this action.");
        }
    }
}