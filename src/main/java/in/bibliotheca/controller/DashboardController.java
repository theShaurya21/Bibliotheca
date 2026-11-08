package in.bibliotheca.controller;

import in.bibliotheca.entity.Loan;
import in.bibliotheca.entity.Reservation;
import in.bibliotheca.repository.*;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final FinePaymentRepository payments;

    public DashboardController(BookRepository books, MemberRepository members, LoanRepository loans,
                               ReservationRepository reservations, FinePaymentRepository payments) {
        this.books = books; this.members = members; this.loans = loans; this.reservations = reservations; this.payments = payments;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("bookTitles", books.count());
        model.addAttribute("physicalCopies", books.sumTotalCopies());
        model.addAttribute("availableCopies", books.sumAvailableCopies());
        model.addAttribute("memberCount", members.count());
        model.addAttribute("activeLoans", loans.countByStatus(Loan.Status.ACTIVE));
        model.addAttribute("overdueLoans", loans.countByStatusAndDueDateBefore(Loan.Status.ACTIVE, LocalDate.now()));
        model.addAttribute("issuedToday", loans.countByIssueDate(LocalDate.now()));
        model.addAttribute("returnedToday", loans.countByStatusAndReturnDate(Loan.Status.RETURNED, LocalDate.now()));
        model.addAttribute("potentialFines", loans.sumFines());
        model.addAttribute("unpaidReservations", reservations.countByStatus(Reservation.Status.WAITING));
        model.addAttribute("recentLoans", loans.findTop8ByOrderByIssueDateDesc());
        model.addAttribute("overdueList", loans.findOverdue(LocalDate.now()));
        model.addAttribute("recentBooks", books.findTop8ByOrderByCreatedAtDesc());
        model.addAttribute("popularBooks", books.findMostBorrowed(PageRequest.of(0, 6)));
        model.addAttribute("payments", payments.findTop20ByOrderByPaidAtDesc());
        return "dashboard";
    }
}
