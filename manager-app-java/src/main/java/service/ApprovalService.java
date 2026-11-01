package service;
 
import dao.ApprovalDao;
import exception.AuthorizationException;
import java.util.List;
import model.Expense;
import model.User;

public class ApprovalService {
 
    private final ApprovalDao approvalDao;
 
    public ApprovalService(ApprovalDao approvalDao) {
        this.approvalDao = approvalDao;
    }
 
    /** FR-M1: list all pending expenses, oldest first. Returns an empty list if there are none. */
    public List<Expense> listPending(User manager) {
        requireManager(manager);
        return approvalDao.findPending();
    }
 
    /** FR-A4: the service itself checks the role, not just the menu. Approve and deny will reuse this. */
    private void requireManager(User user) {
        if (user == null || !user.isManager()) {
            throw new AuthorizationException("Only managers can perform this action.");
        }
    }
}