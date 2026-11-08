package in.bibliotheca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import in.bibliotheca.entity.Book;
import in.bibliotheca.entity.FinePayment;
import in.bibliotheca.entity.Librarian;
import in.bibliotheca.entity.Loan;
import in.bibliotheca.entity.Member;
import in.bibliotheca.entity.Reservation;
import in.bibliotheca.repository.BookRepository;
import in.bibliotheca.repository.FinePaymentRepository;
import in.bibliotheca.repository.LibrarianRepository;
import in.bibliotheca.repository.LoanRepository;
import in.bibliotheca.repository.MemberRepository;
import in.bibliotheca.repository.ReservationRepository;
import in.bibliotheca.service.LibraryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class WebPageSmokeTest {
    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private FinePaymentRepository payments;
    @Autowired private LibrarianRepository librarians;
    @Autowired private LoanRepository loans;
    @Autowired private MemberRepository members;
    @Autowired private ReservationRepository reservations;
    @Autowired private LibraryService library;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "demo-librarian", roles = "LIBRARIAN")
    void protectedPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/dashboard\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/books\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/members\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/circulation\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/reports\"")));
        mvc.perform(get("/books")).andExpect(status().isOk());
        mvc.perform(get("/books/new")).andExpect(status().isOk());
        mvc.perform(get("/members")).andExpect(status().isOk());
        mvc.perform(get("/members/new")).andExpect(status().isOk());
        mvc.perform(get("/circulation")).andExpect(status().isOk());
        mvc.perform(get("/reports")).andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void managementPagesRequireAuthentication() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void librarianLoginLoadsDashboard() throws Exception {
        String username = "route-check-" + UUID.randomUUID();
        String password = "test-password";
        librarians.save(new Librarian(username, passwordEncoder.encode(password)));
        try {
            MvcResult login = mvc.perform(formLogin().user(username).password(password))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"))
                    .andReturn();
            MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
            assertNotNull(session);
            mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        } finally {
            librarians.findByUsername(username).ifPresent(librarians::delete);
        }
    }

    @Test
    @WithMockUser(username = "demo-librarian", roles = "LIBRARIAN")
    void populatedRelationshipPagesRenderWithOpenInViewDisabled() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String coverUrl = "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg?default=false";
        Book book = new Book();
        book.setTitle("Relationship test title");
        book.setAuthor("A. Reader");
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setCoverUrl(coverUrl);
        book = books.save(book);

        Book unavailableBook = new Book();
        unavailableBook.setTitle("Unavailable cover selection test");
        unavailableBook.setAuthor("A. Reader");
        unavailableBook.setTotalCopies(1);
        unavailableBook.setAvailableCopies(0);
        unavailableBook.setCoverUrl(coverUrl);
        unavailableBook = books.save(unavailableBook);

        Member member = new Member();
        member.setFullName("Relationship Test Reader");
        member.setEmail("relationship-test-" + suffix + "@example.edu");
        member.setMembershipStart(LocalDate.now().minusDays(1));
        member.setMembershipExpiry(LocalDate.now().plusMonths(6));
        member = members.save(member);

        Loan loan = library.issue(member.getId(), book.getId());
        loan.setDueDate(LocalDate.now().minusDays(2));
        loan = loans.save(loan);
        reservations.save(new Reservation(book, member));
        payments.save(new FinePayment(loan, new BigDecimal("2.00"), "Cash", "test"));

        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relationship test title")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(coverUrl)));
        mvc.perform(get("/circulation")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relationship Test Reader")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-cover=\"" + coverUrl + "\"")));
        mvc.perform(get("/reports")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relationship test title")));
        mvc.perform(get("/books/{id}", book.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relationship Test Reader")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(coverUrl)));
        mvc.perform(get("/books")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(coverUrl)));
        mvc.perform(get("/members/{id}", member.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Relationship test title")));
        assertEquals(0, books.setCoverUrlIfMissing(book.getId(), "https://example.com/manual-cover.jpg"));
        assertEquals(coverUrl, books.findById(book.getId()).orElseThrow().getCoverUrl());
    }

    @Test
    @Transactional
    @WithMockUser(username = "demo-librarian", roles = "LIBRARIAN")
    void detailPagesRenderPersistedRecords() throws Exception {
        Book book = new Book();
        book.setTitle("Template test title");
        book.setAuthor("A. Reader");
        book.setCategory("Testing");
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book = books.save(book);

        Member member = new Member();
        member.setFullName("Template Test Reader");
        member.setEmail("template-test@example.edu");
        member.setMembershipStart(LocalDate.now().minusDays(1));
        member.setMembershipExpiry(LocalDate.now().plusMonths(6));
        member = members.save(member);

        mvc.perform(get("/books/{id}", book.getId())).andExpect(status().isOk());
        mvc.perform(get("/members/{id}", member.getId())).andExpect(status().isOk());
        var loan = library.issue(member.getId(), book.getId());
        loan.setDueDate(LocalDate.now().minusDays(2));
        library.returnLoan(loan.getId());
        mvc.perform(get("/circulation")).andExpect(status().isOk());
    }

    @Test
    @Transactional
    @WithMockUser(username = "demo-librarian", roles = "LIBRARIAN")
    void invalidBookEditPreservesRecordIdAndShowsIsbnError() throws Exception {
        Book book = new Book();
        book.setTitle("Existing title");
        book.setAuthor("A. Reader");
        book = books.save(book);

        mvc.perform(post("/books/save").with(csrf())
                        .param("id", book.getId().toString())
                        .param("title", "Updated title")
                        .param("author", "A. Reader")
                        .param("isbn", "not-an-isbn")
                        .param("totalCopies", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"id\" value=\"" + book.getId() + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10- or 13-digit ISBN")));
        assertEquals("Existing title", books.findById(book.getId()).orElseThrow().getTitle());
    }
}
