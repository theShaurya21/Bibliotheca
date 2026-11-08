package in.bibliotheca.service;

import in.bibliotheca.config.LibraryRules;
import in.bibliotheca.entity.*;
import in.bibliotheca.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {
    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final FinePaymentRepository payments;
    private final LibraryRules rules;
    private final FineCalculator fineCalculator;

    public LibraryService(BookRepository books, MemberRepository members, LoanRepository loans,
                          ReservationRepository reservations, FinePaymentRepository payments, LibraryRules rules,
                          FineCalculator fineCalculator) {
        this.books = books; this.members = members; this.loans = loans;
        this.reservations = reservations; this.payments = payments; this.rules = rules; this.fineCalculator = fineCalculator;
    }

    public Page<Book> searchBooks(String query, String category, String availability, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page), 12, Sort.by("title").ascending());
        String term = query == null ? "" : query.trim();
        String selectedCategory = category == null ? "" : category.trim();
        Boolean available = switch (availability == null ? "all" : availability) {
            case "", "all" -> null;
            case "available" -> true;
            case "unavailable" -> false;
            default -> throw new IllegalArgumentException("Select a valid availability filter.");
        };
        return books.search(term, selectedCategory, available, pageable);
    }

    public Page<Member> searchMembers(String query, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page), 12, Sort.by("fullName").ascending());
        if (query == null || query.isBlank()) return members.findAll(pageable);
        String term = query.trim();
        return members.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
    }

    @Transactional
    public Loan issue(Long memberId, Long bookId) {
        Member member = members.findByIdForUpdate(memberId).orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Book book = books.findByIdForUpdate(bookId).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        ensureEligible(member);
        expireStaleReservations(book);
        if (loans.countByMemberIdAndStatus(memberId, Loan.Status.ACTIVE) >= rules.getMaximumActiveLoans())
            throw new IllegalArgumentException("This member has reached the active loan limit.");
        List<Reservation> ready = reservations.findByBookIdAndStatusOrderByCreatedAtAsc(bookId, Reservation.Status.READY);
        Reservation ownReadyReservation = ready.isEmpty() ? null : ready.get(0);
        if (ownReadyReservation != null && !ownReadyReservation.getMember().getId().equals(memberId))
            throw new IllegalArgumentException("A copy is being held for the next member in the reservation queue.");
        if (book.getAvailableCopies() <= 0) throw new IllegalArgumentException("No copies are currently available.");
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        if (ownReadyReservation != null) ownReadyReservation.setStatus(Reservation.Status.FULFILLED);
        LocalDate today = LocalDate.now();
        return loans.save(new Loan(book, member, today, today.plusDays(rules.getLoanPeriodDays())));
    }

    @Transactional
    public BigDecimal returnLoan(Long loanId) {
        Loan loan = loans.findByIdForUpdate(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        if (loan.getStatus() != Loan.Status.ACTIVE) throw new IllegalArgumentException("This loan has already been returned.");
        LocalDate today = LocalDate.now();
        BigDecimal fine = fineCalculator.calculate(loan.getDueDate(), today);
        loan.completeReturn(today, fine);
        Book book = books.findByIdForUpdate(loan.getBook().getId()).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        expireStaleReservations(book);
        return fine;
    }

    @Transactional
    public void renew(Long loanId) {
        Loan loan = loans.findByIdForUpdate(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        books.findByIdForUpdate(loan.getBook().getId()).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (loan.getStatus() != Loan.Status.ACTIVE) throw new IllegalArgumentException("Only active loans can be renewed.");
        if (loan.getRenewalCount() >= rules.getMaximumRenewals()) throw new IllegalArgumentException("This loan has reached its renewal limit.");
        if (reservations.existsByBookIdAndStatusIn(loan.getBook().getId(), List.of(Reservation.Status.WAITING, Reservation.Status.READY)))
            throw new IllegalArgumentException("This title has a waiting reservation and cannot be renewed.");
        loan.setDueDate(loan.getDueDate().plusDays(rules.getLoanPeriodDays()));
        loan.incrementRenewalCount();
    }

    @Transactional
    public Reservation reserve(Long memberId, Long bookId) {
        Member member = members.findByIdForUpdate(memberId).orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Book book = books.findByIdForUpdate(bookId).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        ensureEligible(member);
        if (book.getAvailableCopies() > 0) throw new IllegalArgumentException("Copies are available; issue the book instead of reserving it.");
        if (reservations.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, List.of(Reservation.Status.WAITING, Reservation.Status.READY)))
            throw new IllegalArgumentException("This member already has an active reservation for the title.");
        return reservations.save(new Reservation(book, member));
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservations.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        if (reservation.getStatus() != Reservation.Status.WAITING && reservation.getStatus() != Reservation.Status.READY)
            throw new IllegalArgumentException("Only a waiting or ready reservation can be cancelled.");
        boolean wasReady = reservation.getStatus() == Reservation.Status.READY;
        reservation.setStatus(Reservation.Status.CANCELLED);
        if (wasReady) expireStaleReservations(reservation.getBook());
    }

    @Transactional
    public FinePayment recordPayment(Long loanId, BigDecimal amount, String method, String reference) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Payment must be greater than zero.");
        if (method == null || method.isBlank()) throw new IllegalArgumentException("Select a payment method.");
        if (amount.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Payment can have no more than two decimal places.");
        String selectedMethod = method.trim();
        if (!Set.of("Cash", "UPI", "Card", "Bank transfer").contains(selectedMethod))
            throw new IllegalArgumentException("Select a valid payment method.");
        String paymentReference = reference == null || reference.isBlank() ? null : reference.trim();
        if (paymentReference != null && paymentReference.length() > 100)
            throw new IllegalArgumentException("Payment reference must be 100 characters or fewer.");
        Loan loan = loans.findByIdForUpdate(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        BigDecimal paid = payments.paidForLoan(loanId);
        BigDecimal balance = loan.getFine().subtract(paid);
        if (amount.compareTo(balance) > 0) throw new IllegalArgumentException("Payment cannot exceed the outstanding balance.");
        return payments.save(new FinePayment(loan, amount.setScale(2, RoundingMode.UNNECESSARY), selectedMethod, paymentReference));
    }

    private void ensureEligible(Member member) {
        LocalDate today = LocalDate.now();
        if (!member.isActive() || member.getMembershipStart() == null || member.getMembershipStart().isAfter(today)
                || member.getMembershipExpiry() == null || member.getMembershipExpiry().isBefore(today))
            throw new IllegalArgumentException("This member's account is inactive or membership has expired.");
    }

    private void processNextReservation(Book book) {
        if (book.getAvailableCopies() <= 0) return;
        int readyCount = reservations.findByBookIdAndStatusOrderByCreatedAtAsc(book.getId(), Reservation.Status.READY).size();
        List<Reservation> waiting = reservations.findByBookIdAndStatusOrderByCreatedAtAsc(book.getId(), Reservation.Status.WAITING);
        for (Reservation reservation : waiting) {
            if (readyCount >= book.getAvailableCopies()) return;
            Member member = reservation.getMember();
            if (member.isActive() && member.getMembershipStart() != null && !member.getMembershipStart().isAfter(LocalDate.now())
                    && member.getMembershipExpiry() != null
                    && !member.getMembershipExpiry().isBefore(LocalDate.now())
                    && loans.countByMemberIdAndStatus(member.getId(), Loan.Status.ACTIVE) < rules.getMaximumActiveLoans()) {
                reservation.setStatus(Reservation.Status.READY);
                reservation.setExpiresAt(LocalDate.now().plusDays(2));
                readyCount++;
            } else {
                reservation.setStatus(Reservation.Status.EXPIRED);
            }
        }
    }

    private void expireStaleReservations(Book book) {
        List<Reservation> ready = reservations.findByBookIdAndStatusOrderByCreatedAtAsc(book.getId(), Reservation.Status.READY);
        for (Reservation reservation : ready) {
            Member member = reservation.getMember();
            boolean expired = reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(LocalDate.now());
            boolean ineligible = !member.isActive() || member.getMembershipStart() == null
                    || member.getMembershipStart().isAfter(LocalDate.now()) || member.getMembershipExpiry() == null
                    || member.getMembershipExpiry().isBefore(LocalDate.now())
                    || loans.countByMemberIdAndStatus(member.getId(), Loan.Status.ACTIVE) >= rules.getMaximumActiveLoans();
            if (expired || ineligible) reservation.setStatus(Reservation.Status.EXPIRED);
        }
        processNextReservation(book);
    }

    @Scheduled(fixedDelayString = "${app.reservations.refresh-delay-ms:60000}",
            initialDelayString = "${app.reservations.refresh-delay-ms:60000}")
    @Transactional
    public void refreshReservationQueue() {
        List<Long> bookIds = new ArrayList<>();
        reservations.findByStatusOrderByCreatedAtAsc(Reservation.Status.READY)
                .forEach(reservation -> bookIds.add(reservation.getBook().getId()));
        reservations.findByStatusOrderByCreatedAtAsc(Reservation.Status.WAITING)
                .forEach(reservation -> bookIds.add(reservation.getBook().getId()));
        for (Long bookId : bookIds.stream().distinct().toList()) {
            books.findByIdForUpdate(bookId).ifPresent(this::expireStaleReservations);
        }
    }
}
