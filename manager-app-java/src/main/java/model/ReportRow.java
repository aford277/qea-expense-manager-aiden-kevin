package model;
 
import java.math.BigDecimal;
 
/**
 * One line of a report: name, count, total amount, and total approved.
 * "name" is the employee's full name or the category name, depending on the report.
 */
public record ReportRow(
        String name,
        int count,
        BigDecimal totalAmount,
        BigDecimal totalApproved) {
}