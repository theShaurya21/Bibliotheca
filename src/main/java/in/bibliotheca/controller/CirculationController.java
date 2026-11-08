package in.bibliotheca.controller;

import in.bibliotheca.config.LibraryRules;
import in.bibliotheca.entity.*;
import in.bibliotheca.repository.*;
import in.bibliotheca.service.LibraryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/circulation")
public class CirculationController {
    private final LoanRepository loans;
    private final BookRepository books;
    private final MemberRepository members;
    private final ReservationRepository reservations;
    private final FinePaymentRepository payments;
    private final LibraryService library;
    private final LibraryRules rules;

    public CirculationController(LoanRepository loans, BookRepository books, MemberRepository members,
                                 ReservationRepository reservations, FinePaymentRepository payments, LibraryService library,
                                 LibraryRules rules) {
        this.loans = loans; this.books = books; this.members = members; this.reservations = reservations;
        this.payments = payments; this.library = library; this.rules = rules;
    }

    @GetMapping
    public String loans(@RequestParam(defaultValue = "all") String status, Model model) {
        var records = loans.findAll(Sorts.byDueDate());
        if ("active".equals(status)) records = records.stream().filter(l -> l.getStatus() == Loan.Status.ACTIVE).toList();
        if ("overdue".equals(status)) records = loans.findOverdue(LocalDate.now());
        if ("returned".equals(status)) records = records.stream().filter(l -> l.getStatus() == Loan.Status.RETURNED).toList();
        Map<Long, BigDecimal> paidByLoan = new HashMap<>();
        Map<Long, BigDecimal> outstandingByLoan = new HashMap<>();
        for (Loan loan : records) {
            BigDecimal paid = payments.paidForLoan(loan.getId());
            paidByLoan.put(loan.getId(), paid);
            outstandingByLoan.put(loan.getId(), loan.getFine().subtract(paid).max(BigDecimal.ZERO));
        }
        model.addAttribute("loans", records);
        model.addAttribute("paidByLoan", paidByLoan);
        model.addAttribute("outstandingByLoan", outstandingByLoan);
        model.addAttribute("status", status);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("reservations", reservations.findAllWithBookAndMember().stream()
                .filter(r -> r.getStatus() == Reservation.Status.WAITING || r.getStatus() == Reservation.Status.READY).toList());
        model.addAttribute("payments", payments.findTop20ByOrderByPaidAtDesc());
        model.addAttribute("books", books.findAll());
        model.addAttribute("members", members.findAll());
        model.addAttribute("fineRate", rules.getFinePerDay());
        return "circulation";
    }

    @PostMapping("/issue")
    public String issue(@RequestParam Long memberId, @RequestParam Long bookId, RedirectAttributes redirect) {
        try {
            Loan loan = library.issue(memberId, bookId);
            redirect.addFlashAttribute("success", "Issued \"" + loan.getBook().getTitle() + "\". Due " + loan.getDueDate() + ".");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            BigDecimal fine = library.returnLoan(id);
            redirect.addFlashAttribute("success", "Return processed. Finalized fine: ₹" + fine + ".");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes redirect) {
        try { library.renew(id); redirect.addFlashAttribute("success", "Loan renewed."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    @PostMapping("/reserve")
    public String reserve(@RequestParam Long memberId, @RequestParam Long bookId, RedirectAttributes redirect) {
        try { library.reserve(memberId, bookId); redirect.addFlashAttribute("success", "Added to the reservation queue."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, RedirectAttributes redirect) {
        try { library.cancelReservation(id); redirect.addFlashAttribute("success", "Reservation cancelled."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/payment")
    public String payment(@PathVariable Long id, @RequestParam BigDecimal amount, @RequestParam String method,
                          @RequestParam(required = false) String reference, RedirectAttributes redirect) {
        try { library.recordPayment(id, amount, method, reference); redirect.addFlashAttribute("success", "Fine payment recorded."); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/circulation";
    }

    private static class Sorts {
        static org.springframework.data.domain.Sort byDueDate() {
            return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "dueDate");
        }
    }
}
