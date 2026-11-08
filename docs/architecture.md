# Bibliotheca architecture

## Request and persistence flow

The application is a single Spring Boot 3 MVC application. Thymeleaf renders HTML on the server and Spring Security secures each application route except the sign-in page and public static resources.

```text
Browser → Spring Security → MVC controller → LibraryService → Spring Data repository → MySQL
                                      ↓
                                Thymeleaf view
```

- **Controllers** (`controller/`) accept form/query parameters, bind and validate entities, call services, then render a template or redirect with a flash message.
- **Services** (`service/`) contain transactional operations and the library rules. `LibraryService.issue` locks the member and book rows before consuming a copy; return uses the same book-locking strategy while restoring it.
- **Repositories** (`repository/`) provide persistence operations and targeted queries. Spring Data JPA generates the SQL from repository method names and JPQL.
- **Entities** (`entity/`) represent books, members, loans, reservations, fine payments and librarian accounts.
- **Templates and static files** (`resources/templates/`, `resources/static/`) provide the responsive staff UI.
- **Configuration** (`config/`) supplies BCrypt/session security, business-rule properties and configurable demo data initialization.
- **Book covers** use the existing `Book.coverUrl` column. A scheduled service looks up missing covers off the startup thread, verifies Open Library images by ISBN, and uses Google Books only when title and author match. Conditional SQL updates preserve any manually selected URL; image failures use the UI fallback.

## Security

Librarian passwords are stored as BCrypt hashes. Spring Security handles the session login and logout, protects mutating form submissions with CSRF tokens, and denies unauthenticated access to the management pages. No public registration is provided. Demo-account creation is controlled by `DEMO_DATA_ENABLED` and the `DEMO_USERNAME`/`DEMO_PASSWORD` environment variables. Do not use demo credentials in a deployment.

## Circulation transaction

For issuing, the service locks the member and selected book rows. It checks active membership dates, active-loan count, any reservation hold and available copy count. Only then does it decrement availability and save a dated loan. The method is transactional: a failure does not leave only half of the operation persisted.

On return, the service rejects an already-returned loan, calculates the fine from the due date and return date, stores the finalized fine/return, restores availability, and marks the next eligible reservation ready. A scheduled refresh expires stale holds and promotes the next eligible readers when copies are available. The late-fee calculation is centralized in `FineCalculator`.

## Configuration and health

Database connection, server port, demo account and seed-data settings are environment configurable. Rules use the `app.rules` prefix. Spring Boot Actuator exposes the health endpoint at `/actuator/health`.
