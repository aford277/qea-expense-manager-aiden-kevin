package model;
 
import java.math.BigDecimal;
 
/**
 * One expense as the Manager App shows it.
 * employeeName and category are the names, not the IDs, so the menu can print them directly.
 * amount is a BigDecimal with 2 decimals.
 */
public record Expense(
        int id,
        String employeeName,
        String expenseDate,
        String category,
        BigDecimal amount,
        String description) {
}
 