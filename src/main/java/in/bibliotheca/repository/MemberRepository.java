package in.bibliotheca.repository;

import in.bibliotheca.entity.Member;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Page<Member> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
    @Query("select count(m) from Member m where m.active = true and m.membershipStart <= :today and m.membershipExpiry >= :today")
    long countCurrentlyEligible(@Param("today") LocalDate today);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    java.util.Optional<Member> findByIdForUpdate(@Param("id") Long id);
}
