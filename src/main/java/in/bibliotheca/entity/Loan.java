package in.bibliotheca.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_due_status", columnList = "status,dueDate"),
        @Index(name = "idx_loan_member_status", columnList = "member_id,status")
})
public class Loan {
    public enum Status { ACTIVE, RETURNED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Book book;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Member member;
    @Column(nullable = false)
    private LocalDate issueDate;
    @Column(nullable = false)
    private LocalDate dueDate;
    private LocalDate returnDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
    @Column(nullable = false)
    private int renewalCount;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fine = BigDecimal.ZERO;

    protected Loan() {}
    public Loan(Book book, Member member, LocalDate issueDate, LocalDate dueDate) {
        this.book = book; this.member = member; this.issueDate = issueDate; this.dueDate = dueDate;
    }
    public Long getId() { return id; }
    public Book getBook() { return book; }
    public Member getMember() { return member; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public Status getStatus() { return status; }
    public int getRenewalCount() { return renewalCount; }
    public void incrementRenewalCount() { renewalCount++; }
    public BigDecimal getFine() { return fine; }
    public void completeReturn(LocalDate date, BigDecimal fine) { this.returnDate = date; this.status = Status.RETURNED; this.fine = fine; }
}
