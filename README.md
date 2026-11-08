# Bibliotheca

Bibliotheca is a Spring Boot library desk for managing a real catalogue, member directory, circulation, reservations, assessed late fines and librarian-recorded fine payments. It uses server-rendered Thymeleaf pages, Spring Security and MySQL; the displayed counts come from persisted records.

## What is included

- BCrypt-protected librarian sign-in and CSRF-protected session forms.
- Book and member catalogue management, search, validation and safe deactivation.
- Book availability updates and pessimistic locking during issuing/returning.
- Configurable loan length, active-loan cap, renewal limit and late-fee rate.
- Returns with finalized fines; reservation queue with an eligibility-aware two-day hold.
- Fine payment history with balance and overpayment checks.
- Dashboard statistics, overdue list, inventory/member/loan CSV export.
- Smart shelf locations and member borrowing-history profiles.
- Background lookup and display of verified book covers, with a fallback and protection for manually selected URLs.
- Responsive editorial UI with accessibility focus states and reduced-motion support.

See [architecture](docs/architecture.md), [database design](docs/database-design.md), [testing](docs/testing.md), the [librarian user guide](docs/user-guide.md) and [viva preparation](docs/viva-questions.md).

## Requirements

- JDK 23 or newer (the Maven compiler target is Java 23).
- Maven 3.9+.
- MySQL 8.

## MySQL setup

Start MySQL and create the database and application user (change the password to your own):

```sql
CREATE DATABASE bibliotheca_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bibliotheca'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT ALL PRIVILEGES ON bibliotheca_db.* TO 'bibliotheca'@'localhost';
```

In PowerShell, configure connection and demo librarian credentials for your current terminal:

```powershell
$env:DATABASE_URL = "jdbc:mysql://localhost:3306/bibliotheca_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
$env:DATABASE_USERNAME = "bibliotheca"
$env:DATABASE_PASSWORD = "replace-with-a-strong-password"
$env:DEMO_USERNAME = "librarian"
$env:DEMO_PASSWORD = "set-a-unique-local-password"
$env:DEMO_DATA_ENABLED = "true"
```

Then start the application from the project directory:

```powershell
mvn spring-boot:run
```

Open <http://localhost:8080>. Sign in with the `DEMO_USERNAME` and `DEMO_PASSWORD` values set above. The initial local demo loader creates 30 titles and 10 sample readers once, only when both the book and member tables are empty. It creates the librarian account with BCrypt. Set `DEMO_DATA_ENABLED=false` in deployments and provision a librarian account through a controlled administrative process before disabling demo initialization; there is intentionally no public librarian registration.

To use a different HTTP port, set `SERVER_PORT`, or to use a JDBC URL other than the default, set `DATABASE_URL`. See [.env.example](.env.example). Environment variables are used directly so local secrets do not need to be written into tracked files.

## Main workflows

1. Sign in as a librarian.
2. Add books in **Catalogue**, including availability and a physical shelf reference.
3. Register readers in **Members** and set a valid membership expiry.
4. Issue a title from **Circulation**. The service validates member eligibility and active-loan limits and updates the book copy count in a database transaction.
5. Return the item to finalize its late fine and reallocate a copy to the first eligible waiting reservation.
6. Record any fine payment from the returned-loan row. The amount is checked against the remaining balance.
7. Review overdue items and download CSV snapshots from **Reports**.

Default business rules are configured in `application.properties`: three active loans, a 14-day loan period, ₹2 per overdue day and one renewal. The fee is zero when returned on the due date. A title in a waiting/ready reservation queue cannot be renewed.

## Testing

Run the test suite:

```powershell
mvn test
```

Tests use an isolated in-memory H2 database configuration and do not connect to a real library database. Catalogue search supports title, author, ISBN and publisher terms, with category and availability filters. Reservation holds expire automatically and the queue is refreshed by the application. Build a runnable package with:

```powershell
mvn package
java -jar target/bibliotheca-1.0.0.jar
```

## Architecture

This is a Spring MVC/Thymeleaf application. Controllers handle web requests, `LibraryService` enforces circulation rules and transactions, repositories use Spring Data JPA, and entities map to the MySQL tables. Spring Security protects the UI with a session login. For details, see the architecture and database documents.

## Known limitations

- The interface currently covers dashboard, books, members, circulation, payments, reservations and basic reports; dedicated settings, audit-trail and waiver workflows are not implemented.
- Export currently provides inventory, member and loan CSVs. Date-filtered exports, fine-payment report filters, monthly/category charts, and advanced popularity analytics are not implemented.
- Member account access is not exposed; members are managed by authenticated librarians.
- Demo seeding supplies example catalogue/community data but intentionally does not populate paid fine transactions or a full historical activity stream.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for this first local version and preserves data, but a production deployment should move schema evolution to reviewed versioned migrations.
- Demo librarian initialization is for local demonstration only. Choose a unique password and turn demo seeding off for deployment.

## Next steps

Versioned schema migrations, an audit log, waiver workflow, date-filtered CSV exports, category/monthly analytics, and a dedicated settings page are natural extensions.
