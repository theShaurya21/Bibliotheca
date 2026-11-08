package in.bibliotheca.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bibliotheca.entity.Book;
import in.bibliotheca.repository.BookRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class BookCoverLookupServiceTest {
    private static final String CLEAN_CODE_COVER =
            "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg?default=false";

    @Mock private BookRepository books;
    private MockRestServiceServer server;
    private BookCoverLookupService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new BookCoverLookupService(books, new ObjectMapper(), builder.build());
    }

    @Test
    void usesVerifiedTitleAuthorIsbnInsteadOfPlaceholderRecordIsbn() {
        Book book = new Book();
        ReflectionTestUtils.setField(book, "id", 29L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setIsbn("9780000000000");
        when(books.findBooksWithoutCover()).thenReturn(List.of(book));
        service.fillMissingCoverUrls();

        verify(books).setCoverUrlIfMissing(29L, CLEAN_CODE_COVER);
        server.verify();
    }

    @Test
    void refusesGoogleBooksResultsForAnUnmatchedTitle() {
        Book book = new Book();
        ReflectionTestUtils.setField(book, "id", 31L);
        book.setTitle("My Book");
        book.setAuthor("Author Name");
        when(books.findBooksWithoutCover()).thenReturn(List.of(book));
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"items":[{"volumeInfo":{"title":"Different Book","authors":["Author Name"],
                        "imageLinks":{"thumbnail":"http://books.google.com/unrelated.jpg"}}}]}
                        """, MediaType.APPLICATION_JSON));

        service.fillMissingCoverUrls();

        verify(books, never()).setCoverUrlIfMissing(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        server.verify();
    }

    @Test
    void doesNotPerformNetworkLookupsWhenNoCoverlessRecordsExist() {
        when(books.findBooksWithoutCover()).thenReturn(List.of());

        service.fillMissingCoverUrls();

        server.verify();
    }
}
