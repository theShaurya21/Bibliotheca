package in.bibliotheca.repository;

import in.bibliotheca.entity.FinePayment;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long> {
    @Query("select coalesce(sum(p.amount), 0) from FinePayment p")
    BigDecimal sumPayments();
    @Query("select coalesce(sum(p.amount), 0) from FinePayment p where p.loan.id = :loanId")
    BigDecimal paidForLoan(@Param("loanId") Long loanId);
    @EntityGraph(attributePaths = {"loan.book", "loan.member"})
    @Query("select p from FinePayment p where p.loan.member.id = :memberId order by p.paidAt desc")
    List<FinePayment> findByMemberId(@Param("memberId") Long memberId);
    @EntityGraph(attributePaths = {"loan.book", "loan.member"})
    List<FinePayment> findTop20ByOrderByPaidAtDesc();
}
