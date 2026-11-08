package in.bibliotheca.service;

import static org.junit.jupiter.api.Assertions.*;

import in.bibliotheca.entity.*;
import in.bibliotheca.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class LibraryWorkflowTest {
    @Autowired private LibraryService library;
    @Autowired private BookRepository books;
    @Autowired private MemberRepository members;
    @Autowired private LoanRepository loans;
    @Autowired private ReservationRepository reservations;
    @Autowired private FinePaymentRepository payments;

    @Test
    @Transactional
    void returnedCopyMovesToFirstEligibleReservation() {
        Member first = member("first-reader@example.edu");
        Member waiting = member("waiting-reader@example.edu");
        Book book = book(1);
        Loan loan = library.issue(first.getId(), book.getId());
        Reservation reservation = library.reserve(waiting.getId(), book.getId());

        assertEquals(Reservation.Status.WAITING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), library.returnLoan(loan.getId()));
        assertEquals(Reservation.Status.READY, reservation.getStatus());

        Loan nextLoan = library.issue(waiting.getId(), book.getId());
        assertEquals(waiting.getId(), nextLoan.getMember().getId());
        assertEquals(Reservation.Status.FULFILLED, reservation.getStatus());
        assertEquals(0, books.findById(book.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    @Transactional
    void activeLoanLimitIsEnforced() {
        Member reader = member("limit-reader@example.edu");
        Book one = book(1);
        Book two = book(1);
        Book three = book(1);
        Book four = book(1);
        library.issue(reader.getId(), one.getId());
        library.issue(reader.getId(), two.getId());
        library.issue(reader.getId(), three.getId());
        assertThrows(IllegalArgumentException.class, () -> library.issue(reader.getId(), four.getId()));
        assertEquals(3, loans.countByMemberIdAndStatus(reader.getId(), Loan.Status.ACTIVE));
    }

    @Test
    @Transactional
    void returnedLoanCannotBeReturnedTwice() {
        Member reader = member("duplicate-return@example.edu");
        Book book = book(1);
        Loan loan = library.issue(reader.getId(), book.getId());
        library.returnLoan(loan.getId());
        assertThrows(IllegalArgumentException.class, () -> library.returnLoan(loan.getId()));
        assertEquals(1, books.findById(book.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    @Transactional
    void futureMembershipCannotIssueOrReserve() {
        Member reader = member("future-member@example.edu");
        reader.setMembershipStart(LocalDate.now().plusDays(1));
        members.save(reader);
        Book book = book(1);

        assertThrows(IllegalArgumentException.class, () -> library.issue(reader.getId(), book.getId()));
        assertThrows(IllegalArgumentException.class, () -> library.reserve(reader.getId(), book.getId()));
        assertEquals(1, books.findById(book.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    @Transactional
    void catalogueFiltersCombineSearchCategoryAndAvailability() {
        Book available = book(2);
        available.setTitle("Filterable novel");
        available.setCategory("Mystery");
        books.save(available);
        Book unavailable = book(1);
        unavailable.setTitle("Filterable archive");
        unavailable.setCategory("Mystery");
        unavailable.setAvailableCopies(0);
        books.save(unavailable);
        Book otherCategory = book(1);
        otherCategory.setTitle("Filterable science");
        otherCategory.setCategory("Science");
        books.save(otherCategory);

        var matches = library.searchBooks("filterable", "Mystery", "available", 0);

        assertEquals(1, matches.getTotalElements());
        assertEquals("Filterable novel", matches.getContent().get(0).getTitle());
        assertEquals(1, library.searchBooks("", "Mystery", "unavailable", 0).getTotalElements());
    }

    @Test
    @Transactional
    void expiredReservationPromotesNextWaitingMember() {
        Member borrower = member("expiry-borrower@example.edu");
        Member first = member("expiry-first@example.edu");
        Member second = member("expiry-second@example.edu");
        Book book = book(1);
        Loan loan = library.issue(borrower.getId(), book.getId());
        Reservation firstReservation = library.reserve(first.getId(), book.getId());
        Reservation secondReservation = library.reserve(second.getId(), book.getId());

        library.returnLoan(loan.getId());
        firstReservation.setExpiresAt(LocalDate.now().minusDays(1));
        reservations.save(firstReservation);
        library.refreshReservationQueue();

        assertEquals(Reservation.Status.EXPIRED, firstReservation.getStatus());
        assertEquals(Reservation.Status.READY, secondReservation.getStatus());
        assertEquals(LocalDate.now().plusDays(2), secondReservation.getExpiresAt());
    }

    @Test
    @Transactional
    void finePaymentsCannotExceedFinalizedBalance() {
        Member reader = member("payment-reader@example.edu");
        Book book = book(1);
        Loan loan = library.issue(reader.getId(), book.getId());
        loan.setDueDate(LocalDate.now().minusDays(2));
        loans.save(loan);
        library.returnLoan(loan.getId());

        library.recordPayment(loan.getId(), new BigDecimal("2.00"), "Cash", null);

        assertEquals(new BigDecimal("2.00"), payments.paidForLoan(loan.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> library.recordPayment(loan.getId(), new BigDecimal("2.01"), "Cash", null));
        assertThrows(IllegalArgumentException.class,
                () -> library.recordPayment(loan.getId(), new BigDecimal("0.001"), "Cash", null));
        assertThrows(IllegalArgumentException.class,
                () -> library.recordPayment(loan.getId(), new BigDecimal("1.00"), "Crypto", null));
    }

    @Test
    @Transactional
    void issueActivityQueryHonorsDateRange() {
        Member reader = member("range-reader@example.edu");
        Book book = book(1);
        library.issue(reader.getId(), book.getId());

        assertEquals(1, loans.findByIssueDateBetweenOrderByIssueDateDesc(LocalDate.now(), LocalDate.now()).size());
        assertEquals(0, loans.findByIssueDateBetweenOrderByIssueDateDesc(
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)).size());
    }

    private Member member(String email) {
        Member member = new Member();
        member.setFullName("Workflow Reader");
        member.setEmail(email);
        member.setMembershipStart(LocalDate.now().minusDays(1));
        member.setMembershipExpiry(LocalDate.now().plusYears(1));
        return members.save(member);
    }

    private Book book(int copies) {
        Book book = new Book();
        book.setTitle("Workflow book");
        book.setAuthor("Test Author");
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        return books.save(book);
    }
}
