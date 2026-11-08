# Viva preparation

## Explain the project in one minute

Bibliotheca is a library-management web application built with Spring Boot, Thymeleaf, Spring Security, Spring Data JPA and MySQL. A librarian can manage books and members, issue and return copies, renew loans, maintain a reservation queue, calculate late fines, record fine payments and download simple reports. Book availability and circulation actions are persisted in the database.

## Common questions

**Why Spring Boot?**  
It provides sensible defaults and auto-configuration for a web server, MVC, database access and security. That lets the application focus on library rules instead of low-level setup.

**What does each layer do?**  
The controller receives a web request, the service applies the rules, the repository reads/writes data, and the entity maps a Java object to a database table. A Thymeleaf template displays the result.

**How does JPA communicate with MySQL?**  
Spring Data JPA uses Hibernate as its JPA implementation. Hibernate maps entity fields and relationships to SQL statements and executes them through the MySQL JDBC driver.

**How are librarian passwords protected?**  
Spring Security uses BCrypt through `PasswordEncoder`. The database stores a one-way hash, not the original password. Login checks a candidate password against the stored hash.

**What happens when a book is issued?**  
The service loads the member, locks the book row, checks membership, loan limit, reservations and available copies, decrements availability and saves a loan with issue and due dates.

**Why are transactions important?**  
Issuing or returning changes related rows. A transaction makes those changes succeed or fail together, so a loan cannot be created without its copy count being updated.

**How is a return fine calculated?**  
The application counts calendar days after the due date (never a negative number), then multiplies by the configured daily amount. Returning on the due date has no late fine.

**How does the reservation queue work?**  
Reservations are ordered by creation time. At return, the service selects the first active, currently eligible member and marks their reservation READY with a two-day expiry. A scheduled refresh expires stale holds and promotes waiting members when copies are available. The held copy cannot be issued to another member.

**What makes this more than a basic catalogue?**  
It has real persistent book/member/loan records, concurrency-aware copy handling, rule-checked renewal, a reservation queue, final fine records and payments, shelf finding, secured librarian access and dashboard/report views.

## Dependencies

- `spring-boot-starter-web`: MVC and embedded web server.
- `spring-boot-starter-thymeleaf`: server-side HTML rendering.
- `spring-boot-starter-data-jpa`: entity persistence and repositories.
- `spring-boot-starter-security`: login, password hashing and CSRF.
- `spring-boot-starter-validation`: form validation constraints.
- MySQL Connector/J: MySQL JDBC connectivity.
- Actuator: application health endpoint.
- Spring Boot Test, H2 and Spring Security Test: isolated automated testing.

## Current limitations to discuss honestly

The release is a working core demonstrator, not the complete wishlist: audit events, fine waivers, configurable settings UI, dated analytical charts and CSV filters are future work. Schema evolution currently uses Hibernate update; production should adopt versioned migrations. Only librarian staff accounts can sign in.
