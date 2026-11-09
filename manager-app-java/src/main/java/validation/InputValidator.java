package validation;

import exception.ValidationException;

public final class InputValidator {

    public static String validateLoginInput(String input) {
        if (input == null || input.isBlank()) {
            throw new ValidationException("Please enter a username and password.");
        }
        return input;
    }

    public static int validateExpenseId(String input) {
        Integer id = parseWholeNumber(input);
        if (id == null || id <= 0) {
            throw new ValidationException("Expense ID must be a positive whole number.");
        }
        return id;
    }

    public static int validateMenuChoice(String input, int... validChoices) {
        Integer choice = parseWholeNumber(input);
        if (choice != null) {
            for (int option : validChoices) {
                if (option == choice) {
                    return choice;
                }
            }
        }
        throw new ValidationException("Invalid choice. Enter a number from the menu.");
    }

    /** Returns the number, or null if the text isn't a plain whole number like "7" or "-1". */
    private static Integer parseWholeNumber(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim();
        if (!s.matches("-?\\d{1,9}")) { // 9 digits max so it always fits in an int
            return null;
        }
        return Integer.parseInt(s);
    }
}