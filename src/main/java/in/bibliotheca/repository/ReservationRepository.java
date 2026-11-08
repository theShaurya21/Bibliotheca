package in.bibliotheca.repository;

import in.bibliotheca.entity.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, List<Reservation.Status> statuses);
    boolean existsByBookIdAndStatusIn(Long bookId, List<Reservation.Status> statuses);
    List<Reservation> findByBookIdAndStatusOrderByCreatedAtAsc(Long bookId, Reservation.Status status);
    List<Reservation> findByStatusOrderByCreatedAtAsc(Reservation.Status status);
    @EntityGraph(attributePaths = {"book", "member"})
    @Query("select r from Reservation r order by r.createdAt asc")
    List<Reservation> findAllWithBookAndMember();
    long countByStatus(Reservation.Status status);
}
