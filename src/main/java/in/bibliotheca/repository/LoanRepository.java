package in.bibliotheca.repository;

import in.bibliotheca.entity.Loan;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    long countByStatus(Loan.Status status);
    long countByStatusAndDueDateBefore(Loan.Status status, LocalDate date);
    long countByStatusAndIssueDate(Loan.Status status, LocalDate date);
    long countByIssueDate(LocalDate date);
    long countByStatusAndReturnDate(Loan.Status status, LocalDate date);
    long countByMemberIdAndStatus(Long memberId, Loan.Status status);
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findTop8ByOrderByIssueDateDesc();
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByStatusOrderByDueDateAsc(Loan.Status status);
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByMemberIdOrderByIssueDateDesc(Long memberId);
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByBookIdOrderByIssueDateDesc(Long bookId);
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByIssueDateBetweenOrderByIssueDateDesc(LocalDate from, LocalDate to);
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findAll(Sort sort);
    @EntityGraph(attributePaths = {"book", "member"})
    @Query("select l from Loan l")
    List<Loan> findAllWithBookAndMember();
    boolean existsByBookIdAndStatus(Long bookId, Loan.Status status);
    boolean existsByBookId(Long bookId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    java.util.Optional<Loan> findByIdForUpdate(@Param("id") Long id);
    @Query("select coalesce(sum(l.fine), 0) from Loan l")
    java.math.BigDecimal sumFines();
    @EntityGraph(attributePaths = {"book", "member"})
    @Query("select l from Loan l where l.status = 'ACTIVE' and l.dueDate < :today order by l.dueDate asc")
    List<Loan> findOverdue(@Param("today") LocalDate today);
}
