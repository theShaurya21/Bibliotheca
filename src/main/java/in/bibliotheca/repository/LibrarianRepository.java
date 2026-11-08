package in.bibliotheca.repository;

import in.bibliotheca.entity.Librarian;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {
    Optional<Librarian> findByUsername(String username);
}
