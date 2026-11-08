package in.bibliotheca.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = @Index(name = "idx_reservation_book_status", columnList = "book_id,status"))
public class Reservation {
    public enum Status { WAITING, READY, CANCELLED, FULFILLED, EXPIRED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Book book;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Member member;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.WAITING;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDate expiresAt;
    protected Reservation() {}
    public Reservation(Book book, Member member) { this.book = book; this.member = member; }
    public Long getId() { return id; }
    public Book getBook() { return book; }
    public Member getMember() { return member; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
