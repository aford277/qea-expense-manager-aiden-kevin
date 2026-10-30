package model;
 
/**
 * One row from the `users` table (no password).
 * A "record" auto-creates the constructor and getters: id(), username(), role(), fullName().
 */
public record User(int id, String username, String role, String fullName) {
 
    public boolean isManager() {
        return "manager".equals(role);
    }
}
 