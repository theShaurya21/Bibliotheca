# Database design

Hibernate maps the JPA entities to MySQL tables and creates/updates tables using the configured non-destructive `update` strategy. The first release intentionally favors a clear local demonstration; production schema changes should use reviewed versioned migrations.

## Main relationships

```text
Book 1 ───── * Loan * ───── 1 Member
Book 1 ───── * Reservation * ───── 1 Member
Loan 1 ───── * FinePayment
Librarian (independent staff credentials)
```

### Tables and constraints

- **books** — ISBN (unique when supplied; normalized and checked for 10- or 13-digit format), title, author, publisher, genre, publication details, shelf, total and available physical copies, cover/preview links and creation timestamp. `@Version` protects concurrent edits.
- **members** — unique normalized email, membership dates, type, contact fields and active flag. Deactivation retains history.
- **loans** — foreign keys to book and member; issue/due/return dates, active/returned status, renewal count and finalized fine. Indexes support member/status and due-date/status lookups. Historical loans remain.
- **reservations** — book/member foreign keys, queue creation time, state and optional hold expiry. A service-level check prevents duplicate active reservations.
- **fine_payments** — loan foreign key, positive amount, method, optional reference and payment timestamp. Payments are append-only transaction records.
- **librarians** — unique username and BCrypt password hash.

`LocalDate` is used for library calendar dates. Entity timestamps use local server timestamps, and Hibernate JDBC time zone is configured as UTC. Money uses `BigDecimal` with two decimal places. Availability is adjusted inside transactional service methods and guarded by row locking on the book during circulation.

## Business invariants

- An ISBN and a member email cannot duplicate another record.
- Available copies are reduced only when a valid issue is saved and restored on a valid return.
- A book cannot be deleted while any loan history exists; the relational FK also protects historical references.
- Fine days are `max(0, returnDate - dueDate)`, multiplied by the configured daily amount.
- Payments must be positive and cannot exceed the unpaid balance for the loan.
- A READY reservation restricts a returning copy to the selected eligible member.
- The catalogue search applies category and available/unavailable filters in the database query; dashboard copy totals are also aggregated in the database.
