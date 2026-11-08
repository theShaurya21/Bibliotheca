# Librarian user guide

## Sign in

Start the application after configuring its MySQL connection and demo librarian credentials. Open `http://localhost:8080` and sign in with `DEMO_USERNAME` and `DEMO_PASSWORD`. The application stores password hashes; do not commit real passwords or use demo credentials in a deployment.

## Catalogue

Open **Catalogue** to browse the paginated collection. Search by title, author, ISBN or publisher; optionally filter by category and whether copies are available. Use **Add a book** to enter a title, author, optional ISBN, category and total copies. Editing a title preserves the number of copies currently on loan. A book with circulation history cannot be deleted.

Book covers are looked up in the background for existing records with no cover URL. The lookup checks Open Library by ISBN first, then uses an exact title-and-author match from Google Books. Only verified matches are saved, and an existing manually selected cover is never replaced. Catalogue/detail pages, dashboard shelves, and issue/reservation selectors show the cover or a built-in fallback. Outbound access to the cover services is needed for new lookups; unavailable covers can be added manually in the book record and will remain unchanged.

## Members

Open **Members** to search and browse readers. Register or edit member contact and membership dates. Deactivating a member retains their loan/payment history and prevents new issues or reservations.

## Circulation

Use **Issue a title** to select a member and available book. The default loan period is 14 days and each member may have up to three active loans. Use **Return** to restore the copy and finalize any late fine. One renewal is allowed unless the title has a waiting/ready reservation.

When no copy is available, use **Reserve a title**. The first eligible reader in the waiting queue receives a two-day hold when a copy becomes available. The queue is refreshed periodically, and an expired or ineligible hold is passed over. Fine payments are recorded against a finalized loan; partial payments are supported, and payments above the remaining balance are rejected.

## Reports and sign out

The **Reports** page shows current inventory, membership, circulation and overdue information. Its export links download inventory, member and loan CSV files. Use the profile control in the sidebar to sign out; Spring Security invalidates the session and protects form submissions with CSRF tokens.

For configurable rules and setup commands, see the project [README](../README.md). For guided manual scenarios, see [testing and verification](./testing.md).
