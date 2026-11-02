package service;
 
import dao.ApprovalDao;
import exception.AuthorizationException;
import exception.InvalidStateException;
import exception.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import model.Expense;
import model.User;
import validation.CommentValidator;

public class ApprovalService {
 
    private final ApprovalDao approvalDao;
    private final Clock clock;   // where "today" comes from
 
    public ApprovalService(ApprovalDao approvalDao, Clock clock) {
        this.approvalDao = approvalDao;
        this.clock = clock;
    }
 
    /** FR-M1: list all pending expenses, oldest first. Returns an empty list if there are none. */
    public List<Expense> listPending(User manager) {
        requireManager(manager);
        return approvalDao.findPending();
    }

    public void checkCanDecide(User manager, int expenseId) {
        requireManager(manager);
        String status = approvalDao.findStatus(expenseId);
        if (status == null) {
            throw new NotFoundException("Expense " + expenseId + " not found.");
        }
        if (!"pending".equals(status)) {
            throw new InvalidStateException("Expense is no longer pending.");
        }
    }

    public void approve(User manager, int expenseId, String comment) {
        requireManager(manager);
        CommentValidator.validateApproval(comment);
        decide(manager, expenseId, "approved", comment);
    }
 
    public void deny(User manager, int expenseId, String comment) {
        requireManager(manager);
        CommentValidator.validateDenial(comment);
        decide(manager, expenseId, "denied", comment);
    }
 
    /** Shared steps for approve and deny. */
    private void decide(User manager, int expenseId, String status, String comment) {
        checkCanDecide(manager, expenseId);
 
        // Store a blank comment as NULL so the Python app shows "(no comment)"
        String cleanComment = (comment == null || comment.isBlank()) ? null : comment.trim();
 
        boolean saved = approvalDao.saveDecision(
                expenseId, status, manager.id(), cleanComment, LocalDate.now(clock));
        if (!saved) {
            // It was pending a moment ago but changed before we saved 
            throw new InvalidStateException("Expense is no longer pending.");
        }
    }
 
    /** FR-A4: the service itself checks the role, not just the menu. Approve and deny will reuse this. */
    private void requireManager(User user) {
        if (user == null || !user.isManager()) {
            throw new AuthorizationException("Only managers can perform this action.");
        }
    }
}