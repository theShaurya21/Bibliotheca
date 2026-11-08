package in.bibliotheca.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fine_payments", indexes = @Index(name = "idx_payment_loan", columnList = "loan_id"))
public class FinePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Loan loan;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 30) private String method;
    @Column(length = 100) private String reference;
    @Column(nullable = false, updatable = false) private LocalDateTime paidAt = LocalDateTime.now();
    protected FinePayment() {}
    public FinePayment(Loan loan, BigDecimal amount, String method, String reference) {
        this.loan = loan; this.amount = amount; this.method = method; this.reference = reference;
    }
    public Long getId() { return id; }
    public Loan getLoan() { return loan; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getReference() { return reference; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
