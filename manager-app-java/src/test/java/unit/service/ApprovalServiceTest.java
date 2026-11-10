package unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dao.ApprovalDao;
import exception.AuthorizationException;
import exception.InvalidStateException;
import exception.NotFoundException;
import exception.ValidationException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import model.User;
import service.ApprovalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ApprovalService. The DAO is a Mockito mock, so no database is used:
 * each test decides what the "database" returns, then checks what the service did.
 */
class ApprovalServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private static final User MANAGER = new User(1, "mgr_asha", "manager", "Asha Patel");
    private static final User EMPLOYEE = new User(3, "emp_jordan", "employee", "Jordan Lee");

    private ApprovalDao dao;
    private ApprovalService service;

    @BeforeEach
    void setUp() {
        dao = mock(ApprovalDao.class);                      // a fake DAO: returns null/false/empty unless told otherwise
        service = new ApprovalService(dao, FIXED_CLOCK);    // the real service, using the fake DAO
    }

    // ---------------------------------------------------------------- FR-A4: only managers

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A4 (TA-04) - an employee can't approve, and nothing is saved")
    void employeeCannotApprove() {
        assertThrows(AuthorizationException.class, () -> service.approve(EMPLOYEE, 25, "OK"));
        verify(dao, never()).saveDecision(anyInt(), anyString(), anyInt(), any(), any());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-A4 (TA-04) - an employee can't deny, and nothing is saved")
    void employeeCannotDeny() {
        assertThrows(AuthorizationException.class, () -> service.deny(EMPLOYEE, 25, "Missing receipt"));
        verify(dao, never()).saveDecision(anyInt(), anyString(), anyInt(), any(), any());
    }

    // ---------------------------------------------------------------- FR-M2: deciding

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M2, VR-05 (TM-02) - denial with a 4-character comment saves nothing")
    void denyWithShortCommentSavesNothing() {
        assertThrows(ValidationException.class, () -> service.deny(MANAGER, 25, "abcd"));
        verify(dao, never()).saveDecision(anyInt(), anyString(), anyInt(), any(), any());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M2 - deciding a missing expense throws NotFoundException and saves nothing")
    void decidingMissingExpenseThrowsNotFound() {
        when(dao.findStatus(999)).thenReturn(null);   // no approvals row for this ID

        assertThrows(NotFoundException.class, () -> service.approve(MANAGER, 999, "OK"));
        verify(dao, never()).saveDecision(anyInt(), anyString(), anyInt(), any(), any());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M2, FR-S2 - deciding an already-decided expense throws InvalidStateException")
    void decidingDecidedExpenseThrowsInvalidState() {
        when(dao.findStatus(7)).thenReturn("approved");   // seed expense 7 is already approved

        assertThrows(InvalidStateException.class, () -> service.approve(MANAGER, 7, "OK"));
        verify(dao, never()).saveDecision(anyInt(), anyString(), anyInt(), any(), any());
    }

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M2, FR-S2 - expense changed before the save (XW-03/XW-04) throws InvalidStateException")
    void expenseChangedBeforeSaveThrowsInvalidState() {
        when(dao.findStatus(25)).thenReturn("pending");
        // The UPDATE matched no rows: the employee deleted it, or another manager decided it first
        when(dao.saveDecision(25, "approved", 1, "OK", TODAY)).thenReturn(false);

        assertThrows(InvalidStateException.class, () -> service.approve(MANAGER, 25, "OK"));
    }

    @Test
    @DisplayName("Covers: FR-M2 - approve saves status, reviewer, trimmed comment and today's date")
    void approveSavesDecision() {
        when(dao.findStatus(25)).thenReturn("pending");
        when(dao.saveDecision(25, "approved", 1, "Looks fine", TODAY)).thenReturn(true);

        service.approve(MANAGER, 25, "  Looks fine  ");

        verify(dao).saveDecision(25, "approved", 1, "Looks fine", TODAY);
    }

    @Test
    @DisplayName("Covers: FR-M2, VR-06, FR-S5 - a blank approval comment is saved as NULL")
    void blankApprovalCommentIsSavedAsNull() {
        when(dao.findStatus(25)).thenReturn("pending");
        when(dao.saveDecision(25, "approved", 1, null, TODAY)).thenReturn(true);

        service.approve(MANAGER, 25, "   ");

        verify(dao).saveDecision(25, "approved", 1, null, TODAY);
    }

    // ---------------------------------------------------------------- FR-M1: listing

    @Test
    @Tag("negative")
    @DisplayName("Covers: FR-M1 (TM-07) - no pending expenses gives an empty list")
    void noPendingExpensesGivesEmptyList() {
        when(dao.findPending()).thenReturn(List.of());

        assertTrue(service.listPending(MANAGER).isEmpty());
    }
}