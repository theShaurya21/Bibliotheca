# Testing and verification

## Automated tests

The test profile uses an in-memory H2 database in MySQL compatibility mode. It is isolated from `bibliotheca_db`; automated tests do not alter local MySQL data.

Run the test suite and package:

```powershell
mvn clean test
mvn package
```

Tests cover demo-data initialization and repeatability, protected page rendering and login, persisted entity relationships, fine boundaries, circulation limits, duplicate returns, reservation promotion and expiry, catalogue filters, fine payments, and dashboard inventory/loan consistency. The reports page also accepts inclusive start and end dates for borrowing activity.

## Manual MySQL verification

Automated tests do not verify a separately installed MySQL server. To exercise the MySQL configuration without modifying an existing database:

1. Create a dedicated empty development database and a least-privilege application user.
2. Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in the current terminal.
3. Set `DEMO_DATA_ENABLED=true` and provide `DEMO_USERNAME` and `DEMO_PASSWORD` for a local demonstration account.
4. Start with `mvn spring-boot:run`, sign in, and verify book/member changes remain after restarting.
5. To verify initialization is repeatable, restart against the same dedicated database and confirm that the catalogue and reader counts do not increase.
6. Never use a production or personally maintained database for this procedure. The demo initializer does not clear existing tables.

## Important manual scenarios

- Add, edit, search, filter and safely remove a book. Confirm an invalid ISBN or duplicate ISBN is rejected and copy counts remain consistent.
- Add and edit members, including membership dates; confirm a future-start or expired member cannot borrow or reserve.
- Issue, renew and return books; verify the active-loan cap, copy counts, and fine calculation at the due-date boundary.
- Add duplicate reservations, cancel a hold, wait for an expired hold to be refreshed, and confirm the next eligible reservation becomes ready.
- Record partial and full fine payments and verify an overpayment is rejected.
- Use the inventory/member/loan CSV export links and open the result in a spreadsheet application.
- Check the responsive layout at desktop, tablet and mobile widths; inspect browser developer tools for client-side errors.

The application depends on a working MySQL service for the manual runtime check. The automated test database intentionally does not prove that local MySQL credentials, schema permissions, or network connectivity are correct.
