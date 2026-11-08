package in.bibliotheca.controller;

import in.bibliotheca.config.LibraryRules;
import in.bibliotheca.entity.Loan;
import in.bibliotheca.entity.Reservation;
import in.bibliotheca.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final FinePaymentRepository payments;
    private final LibraryRules rules;
    public ReportsController(BookRepository books, MemberRepository members, LoanRepository loans,
                             ReservationRepository reservations, FinePaymentRepository payments, LibraryRules rules) {
        this.books = books; this.members = members; this.loans = loans;
        this.reservations = reservations; this.payments = payments; this.rules = rules;
    }

    @GetMapping
    public String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("The start date must be on or before the end date.");
        model.addAttribute("books", books.findAll());
        model.addAttribute("members", members.findAll());
        model.addAttribute("activeMembers", members.countCurrentlyEligible(LocalDate.now()));
        model.addAttribute("activeLoans", loans.findByStatusOrderByDueDateAsc(Loan.Status.ACTIVE));
        model.addAttribute("periodLoans", loans.findByIssueDateBetweenOrderByIssueDateDesc(start, end));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("overdue", loans.findOverdue(LocalDate.now()));
        model.addAttribute("totalFines", loans.sumFines());
        model.addAttribute("collectedFines", payments.sumPayments());
        model.addAttribute("waitingReservations", reservations.countByStatus(Reservation.Status.WAITING));
        model.addAttribute("readyReservations", reservations.countByStatus(Reservation.Status.READY));
        model.addAttribute("fineRate", rules.getFinePerDay());
        return "reports";
    }

    @GetMapping("/export")
    public void export(@RequestParam(defaultValue = "inventory") String type, HttpServletResponse response) throws IOException {
        String filename = switch (type) {
            case "members" -> "bibliotheca-members.csv";
            case "loans" -> "bibliotheca-loans.csv";
            default -> "bibliotheca-inventory.csv";
        };
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        var writer = response.getWriter();
        if ("members".equals(type)) {
            writer.println("Name,Email,Phone,Membership,Expiry,Active");
            for (var member : members.findAll())
                writer.println(csv(member.getFullName()) + "," + csv(member.getEmail()) + "," + csv(member.getPhone()) + ","
                        + csv(member.getMembershipType()) + "," + member.getMembershipExpiry() + "," + member.isActive());
        } else if ("loans".equals(type)) {
            writer.println("Member,Book,Issue date,Due date,Return date,Status,Fine");
            for (var loan : loans.findAllWithBookAndMember())
                writer.println(csv(loan.getMember().getFullName()) + "," + csv(loan.getBook().getTitle()) + ","
                        + loan.getIssueDate() + "," + loan.getDueDate() + "," + loan.getReturnDate() + ","
                        + loan.getStatus() + "," + loan.getFine());
        } else {
            writer.println("Title,Author,ISBN,Category,Shelf,Total copies,Available copies");
            for (var book : books.findAll())
                writer.println(csv(book.getTitle()) + "," + csv(book.getAuthor()) + "," + csv(book.getIsbn()) + ","
                        + csv(book.getCategory()) + "," + csv(book.getShelfLocation()) + ","
                        + book.getTotalCopies() + "," + book.getAvailableCopies());
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null) return "";
        String safe = value.replace("\"", "\"\"");
        if (safe.matches("^[=+@\\-].*")) safe = "'" + safe;
        return "\"" + safe + "\"";
    }
}
