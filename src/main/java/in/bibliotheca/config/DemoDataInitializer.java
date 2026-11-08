package in.bibliotheca.config;

import in.bibliotheca.entity.*;
import in.bibliotheca.repository.*;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {
    @Bean
    CommandLineRunner initializeDemoData(BookRepository books, MemberRepository members, LoanRepository loans,
                                         ReservationRepository reservations,
                                         LibrarianRepository librarians, PasswordEncoder encoder,
                                         @Value("${app.demo-data:true}") boolean enabled,
                                         @Value("${app.demo-username:librarian}") String username,
                                         @Value("${app.demo-password:}") String password) {
        return args -> {
            if (!enabled) return;
            if (librarians.findByUsername(username).isEmpty()) {
                if (password == null || password.isBlank())
                    throw new IllegalStateException("Set DEMO_PASSWORD to create the demo librarian account.");
                librarians.save(new Librarian(username, encoder.encode(password)));
            }
            if (books.count() > 0 || members.count() > 0) return;

            String[][] catalogue = {
                    {"The God of Small Things","Arundhati Roy","Literary Fiction"},
                    {"The White Tiger","Aravind Adiga","Literary Fiction"},
                    {"Train to Pakistan","Khushwant Singh","Historical Fiction"},
                    {"A Suitable Boy","Vikram Seth","Literary Fiction"},
                    {"The Namesake","Jhumpa Lahiri","Literary Fiction"},
                    {"The Inheritance of Loss","Kiran Desai","Literary Fiction"},
                    {"Midnight's Children","Salman Rushdie","Historical Fiction"},
                    {"The Palace of Illusions","Chitra Banerjee Divakaruni","Mythology"},
                    {"The Guide","R. K. Narayan","Classic"},
                    {"Malgudi Days","R. K. Narayan","Short Stories"},
                    {"The Discovery of India","Jawaharlal Nehru","History"},
                    {"India After Gandhi","Ramachandra Guha","History"},
                    {"Wings of Fire","A. P. J. Abdul Kalam","Biography"},
                    {"The Argumentative Indian","Amartya Sen","Economics"},
                    {"The Great Indian Novel","Shashi Tharoor","Satire"},
                    {"Gitanjali","Rabindranath Tagore","Poetry"},
                    {"The Room on the Roof","Ruskin Bond","Young Adult"},
                    {"A Fine Balance","Rohinton Mistry","Literary Fiction"},
                    {"The Hungry Tide","Amitav Ghosh","Literary Fiction"},
                    {"The Shadow Lines","Amitav Ghosh","Literary Fiction"},
                    {"The Blue Umbrella","Ruskin Bond","Children"},
                    {"The Jungle Book","Rudyard Kipling","Children"},
                    {"Pride and Prejudice","Jane Austen","Classic"},
                    {"To Kill a Mockingbird","Harper Lee","Classic"},
                    {"The Great Gatsby","F. Scott Fitzgerald","Classic"},
                    {"Sapiens","Yuval Noah Harari","Science"},
                    {"A Brief History of Time","Stephen Hawking","Science"},
                    {"The Design of Everyday Things","Don Norman","Design"},
                    {"Clean Code","Robert C. Martin","Technology"},
                    {"The Art of War","Sun Tzu","Philosophy"}
            };
            for (int i = 0; i < catalogue.length; i++) {
                Book book = new Book();
                book.setIsbn(demoIsbn(i + 1));
                book.setTitle(catalogue[i][0]);
                book.setAuthor(catalogue[i][1]);
                book.setCategory(catalogue[i][2]);
                book.setPublisher("Bibliotheca Press");
                book.setPublicationYear(2000 + i % 24);
                book.setLanguage("English");
                book.setShelfLocation("Block A → Floor " + (1 + i % 3) + " → Shelf A-" + String.format("%02d", i + 1));
                book.setTotalCopies(i % 5 == 0 ? 3 : 2);
                book.setAvailableCopies(book.getTotalCopies());
                books.save(book);
            }
            String[] names = {"Aarav Sharma","Ananya Iyer","Ishaan Mehta","Diya Nair","Kabir Das","Meera Patel","Rohan Gupta","Sara Khan","Vivaan Rao","Tara Sen"};
            for (int i = 0; i < names.length; i++) {
                Member member = new Member();
                member.setFullName(names[i]);
                member.setEmail("reader" + (i + 1) + "@example.edu");
                member.setPhone("+91 90000 000" + String.format("%02d", i));
                member.setDepartment(i % 2 == 0 ? "Computer Science" : "Literature");
                member.setMembershipStart(LocalDate.now().minusMonths(8));
                member.setMembershipExpiry(LocalDate.now().plusMonths(4));
                members.save(member);
            }
            Book overdueOne = books.findAll().get(0);
            Book overdueTwo = books.findAll().get(1);
            overdueOne.setAvailableCopies(overdueOne.getAvailableCopies() - 1);
            overdueTwo.setAvailableCopies(overdueTwo.getAvailableCopies() - 1);
            loans.save(new Loan(overdueOne, members.findAll().get(0), LocalDate.now().minusDays(25), LocalDate.now().minusDays(11)));
            loans.save(new Loan(overdueTwo, members.findAll().get(1), LocalDate.now().minusDays(20), LocalDate.now().minusDays(6)));
            books.save(overdueOne);
            books.save(overdueTwo);
            Book active = books.findAll().get(2);
            active.setAvailableCopies(active.getAvailableCopies() - 1);
            loans.save(new Loan(active, members.findAll().get(2), LocalDate.now().minusDays(4), LocalDate.now().plusDays(10)));
            books.save(active);

            Book reservedTitle = books.findAll().get(5);
            for (int i = 0; i < reservedTitle.getTotalCopies(); i++) {
                reservedTitle.setAvailableCopies(reservedTitle.getAvailableCopies() - 1);
                loans.save(new Loan(reservedTitle, members.findAll().get(i + 3), LocalDate.now().minusDays(3), LocalDate.now().plusDays(11)));
            }
            if (reservedTitle.getAvailableCopies() == 0) {
                reservations.save(new Reservation(reservedTitle, members.findAll().get(7)));
            }
            books.save(reservedTitle);
        };
    }

    private static String demoIsbn(int sequence) {
        String firstTwelve = "9780000" + String.format("%05d", sequence);
        int sum = 0;
        for (int i = 0; i < firstTwelve.length(); i++)
            sum += Character.digit(firstTwelve.charAt(i), 10) * (i % 2 == 0 ? 1 : 3);
        return firstTwelve + (10 - sum % 10) % 10;
    }
}
