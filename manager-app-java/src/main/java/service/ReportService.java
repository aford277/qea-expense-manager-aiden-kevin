package service;

import dao.ReportDao;
import exception.AuthorizationException;

import java.time.LocalDate;
import java.util.List;
import model.ReportRow;
import model.User;
import validation.DateRangeValidator;

/** Reports for managers (spec FR-M3). Date range report (a Should) can be added here later. */
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
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

    /** A non-manager calling reports gets AuthorizationException. */
    private void requireManager(User user) {
        if (user == null || !user.isManager()) {
            throw new AuthorizationException("Only managers can perform this action.");
        }
    }
}