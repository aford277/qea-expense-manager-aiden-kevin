# Test Plan

## Scope

In:

- Employee App
- Manager App
- Database
- All FR and VR requirements
- Cross-app cases

Out:

- GUI/API
- Performance
- Security
- Code coverage
- NF

## Approach by Level

| Level | Python (PyTest) | Java (JUnit) |
|---|---|---|
| Unit | • Validators and services must be tested<br>• Use mocks for input, today date, database errors | • Validators and services must be tested<br>• Use Mockito for DAOs |
| Integration | • Testing how services interact with database<br>• Use a temporary database per test | *(same as left)* |
| SQL Validation | Five queries given in project specification | *Not done in java* |
| Menu Flow | One test | One test |
| Manual | • Design cases listed in section 9.6 in the project specification<br>• Works across both apps | *(same as left)* |
| Exploratory | One 30 minute session | One 30 minute session |

## Entry and exit criteria

Entry:

- Both apps built and able to run
- Database can be created and populated from schema.sql and seed.sql
- Test cases are written for each requirement

Exit:

- Every Must requirement has a designed case and an automated test, traced in the RTM
- All automated tests pass on a clean clone
- No open Critical or High defects
- Two test cycles recorded

## Smoke subset

- Valid log in each app (FR-A1)
- Submit a valid expense (FR-E1)
- View my expenses (FR-E2)
- List pending and approve one (FR-M1, FR-M2)
- Database creates cleanly (FR-S1)

## Main risks (and mitigation)

- Tests written after development may match the code more than the specification
  - Check expected results against the requirements doc
- Cross app mismatches may cause errors
  - Will be covered by the XW cases in 9.6
- We are new to JUnit and testing in Java as a whole
  - Start with easier tests first (ie simple unit tests)
- Time; having many tasks in our training program can add up
  - Must prioritize properly
