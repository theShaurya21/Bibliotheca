package in.bibliotheca.repository;

import in.bibliotheca.entity.Book;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findTop8ByOrderByCreatedAtDesc();
    @Query(value = """
            select b from Book b
            where (:query = '' or
                lower(b.title) like lower(concat('%', :query, '%')) or
                lower(b.author) like lower(concat('%', :query, '%')) or
                lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%')) or
                lower(coalesce(b.publisher, '')) like lower(concat('%', :query, '%')))
              and (:category = '' or b.category = :category)
              and (:available is null or (:available = true and b.availableCopies > 0)
                   or (:available = false and b.availableCopies = 0))
            """,
            countQuery = """
            select count(b) from Book b
            where (:query = '' or
                lower(b.title) like lower(concat('%', :query, '%')) or
                lower(b.author) like lower(concat('%', :query, '%')) or
                lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%')) or
                lower(coalesce(b.publisher, '')) like lower(concat('%', :query, '%')))
              and (:category = '' or b.category = :category)
              and (:available is null or (:available = true and b.availableCopies > 0)
                   or (:available = false and b.availableCopies = 0))
            """)
    Page<Book> search(@Param("query") String query, @Param("category") String category,
                      @Param("available") Boolean available, Pageable pageable);
    @Query("select distinct b.category from Book b where b.category is not null and b.category <> '' order by b.category")
    List<String> findDistinctCategories();
    @Query("select coalesce(sum(b.totalCopies), 0) from Book b")
    long sumTotalCopies();
    @Query("select coalesce(sum(b.availableCopies), 0) from Book b")
    long sumAvailableCopies();
    @Query("select b from Book b where b.coverUrl is null or trim(b.coverUrl) = ''")
    List<Book> findBooksWithoutCover();
    @Modifying
    @Transactional
    @Query("update Book b set b.coverUrl = :coverUrl where b.id = :id and (b.coverUrl is null or trim(b.coverUrl) = '')")
    int setCoverUrlIfMissing(@Param("id") Long id, @Param("coverUrl") String coverUrl);
    @Query("select l.book from Loan l group by l.book order by count(l) desc")
    List<Book> findMostBorrowed(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);
}
