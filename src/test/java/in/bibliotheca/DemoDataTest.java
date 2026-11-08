package in.bibliotheca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import in.bibliotheca.entity.Loan;
import in.bibliotheca.entity.Reservation;
import in.bibliotheca.repository.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.demo-data=true",
        "app.demo-username=seed-test-librarian",
        "app.demo-password=temporary-test-password"
})
class DemoDataTest {
    @Autowired private BookRepository books;
    @Autowired private MemberRepository members;
    @Autowired private LoanRepository loans;
    @Autowired private ReservationRepository reservations;
    @Autowired private LibrarianRepository librarians;
    @Autowired private CommandLineRunner initializer;

    @Test
    void initializesRepeatableLibraryDemonstrationData() {
        assertEquals(30, books.count());
        assertEquals(10, members.count());
        assertTrue(loans.countByStatusAndDueDateBefore(Loan.Status.ACTIVE, LocalDate.now()) >= 2);
        assertEquals(loans.countByStatus(Loan.Status.ACTIVE), books.sumTotalCopies() - books.sumAvailableCopies());
        assertEquals(1, reservations.countByStatus(Reservation.Status.WAITING));
        String passwordHash = librarians.findByUsername("seed-test-librarian").orElseThrow().getPassword();
        assertTrue(passwordHash.startsWith("$2"));

        assertDoesNotThrow(() -> initializer.run());
        assertEquals(30, books.count());
        assertEquals(10, members.count());
        assertEquals(passwordHash, librarians.findByUsername("seed-test-librarian").orElseThrow().getPassword());
    }
}
