# Test Effectiveness Check

## Bug 1 — Manager App (Java)

| Item | Details |
|------|---------|
| File changed | manager-app-java\src\main\java\validation\CommentValidator.java |
| Requirement / rule affected | VR-05 |
| Suite run | mvn test |

**Change made**

```java
// Before
    public static final int MIN_DENIAL_LENGTH = 5;

// After (bug)
    public static final int MIN_DENIAL_LENGTH = 0;
```

**Result**

- Caught by an automated test? Yes
- Test(s) that failed: 
CommentValidatorTest.denialCommentMinimumLength
ApprovalServiceTest.denyWithShortCommentSavesNothing
- Failure message: 
expected: <false> but was: <true>
Unexpected exception type thrown, expected: <exception.ValidationException> but was: <exception.NotFoundException>
- Tests run / failed: 96 ran, 5 failed

---

## Bug 2 — Employee App (Python)

| Item | Details |
|------|---------|
| File changed | employee-app-python\employee_app\validators.py |
| Requirement / rule affected | VR-01 |
| Suite run | pytest |

**Change made**

```python
# Before
    if val <= 0:

# After (bug)
    if val < 0:
```

**Result**

- Caught by an automated test? Yes
- Test(s) that failed: 
tests/unit/test_validators.py::test_amount_invalid[0]
- Failure message: Failed: DID NOT RAISE ValidationError
- Tests run / failed: 86 ran, 1 failed

---

## Summary

Both suites caught each bug, showing that our validation testing is functional. The changes have been reverted and are no longer in the program.