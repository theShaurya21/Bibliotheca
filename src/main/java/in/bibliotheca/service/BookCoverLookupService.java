package in.bibliotheca.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.bibliotheca.entity.Book;
import in.bibliotheca.repository.BookRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BookCoverLookupService {
    private static final Logger log = LoggerFactory.getLogger(BookCoverLookupService.class);
    private static final String OPEN_LIBRARY_COVER_URL = "https://covers.openlibrary.org/b/isbn/%s-L.jpg?default=false";
    private static final Map<String, String> VERIFIED_ISBNS = Map.ofEntries(
            Map.entry("atomic habits|james clear", "9780735211292"),
            Map.entry("the alchemist|paulo coelho", "9780062315007"),
            Map.entry("clean code|robert c martin", "9780132350884"),
            Map.entry("harry potter and the philosopher s stone|j k rowling", "9780747532699"),
            Map.entry("rich dad poor dad|robert kiyosaki", "9781612680194"),
            Map.entry("the psychology of money|morgan housel", "9780857197689"),
            Map.entry("think and grow rich|napoleon hill", "9781585424337"),
            Map.entry("the 48 laws of power|robert greene", "9780140280197"),
            Map.entry("ikigai|hector garcia and francesc miralles", "9780143130727"),
            Map.entry("deep work|cal newport", "9781455586691")
    );

    private final BookRepository books;
    private final ObjectMapper objectMapper;
    private final RestClient http;
    private final Map<String, CachedLookup> lookupCache = new ConcurrentHashMap<>();

    @Autowired
    public BookCoverLookupService(BookRepository books, ObjectMapper objectMapper) {
        this(books, objectMapper, createHttpClient());
    }

    BookCoverLookupService(BookRepository books, ObjectMapper objectMapper, RestClient http) {
        this.books = books;
        this.objectMapper = objectMapper;
        this.http = http;
    }

    private static RestClient createHttpClient() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Bibliotheca/1.0 book-cover lookup")
                .build();
    }

    @Scheduled(initialDelayString = "${app.covers.initial-delay-ms:30000}",
            fixedDelayString = "${app.covers.refresh-delay-ms:86400000}")
    public void fillMissingCoverUrls() {
        List<Book> candidates = new ArrayList<>(books.findBooksWithoutCover());
        candidates.sort((first, second) -> Boolean.compare(
                !VERIFIED_ISBNS.containsKey(titleAuthorKey(first.getTitle(), first.getAuthor())),
                !VERIFIED_ISBNS.containsKey(titleAuthorKey(second.getTitle(), second.getAuthor()))));
        int updated = 0;
        for (Book book : candidates) {
            String key = cacheKey(book);
            CachedLookup cached = lookupCache.get(key);
            if (cached == null || (cached.coverUrl().isEmpty()
                    && cached.attemptedAt().isBefore(Instant.now().minus(Duration.ofHours(6))))) {
                cached = new CachedLookup(lookupCover(book), Instant.now());
                lookupCache.put(key, cached);
            }
            Optional<String> cover = cached.coverUrl();
            if (cover.isPresent()) updated += books.setCoverUrlIfMissing(book.getId(), cover.get());
        }
        if (updated > 0) log.info("Added verified covers for {} existing catalogue record(s).", updated);
    }

    private Optional<String> lookupCover(Book book) {
        String titleAuthor = titleAuthorKey(book.getTitle(), book.getAuthor());
        String verifiedIsbn = VERIFIED_ISBNS.get(titleAuthor);
        if (verifiedIsbn != null) return Optional.of(OPEN_LIBRARY_COVER_URL.formatted(verifiedIsbn));

        if (book.getIsbn() != null && !book.getIsbn().isBlank()) {
            Optional<String> byIsbn = openLibraryCover(book.getIsbn());
            if (byIsbn.isPresent()) return byIsbn;
        }

        return googleBooksCover(book);
    }

    private Optional<String> openLibraryCover(String isbn) {
        String normalizedIsbn = isbn.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        if (normalizedIsbn.isBlank()) return Optional.empty();
        String url = OPEN_LIBRARY_COVER_URL.formatted(normalizedIsbn);
        try {
            var response = http.head().uri(url).retrieve().toBodilessEntity();
            MediaType contentType = response.getHeaders().getContentType();
            return contentType != null && "image".equalsIgnoreCase(contentType.getType())
                    ? Optional.of(url) : Optional.empty();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404)
                log.warn("Open Library cover lookup failed for ISBN {} (HTTP {}).", normalizedIsbn, exception.getStatusCode().value());
            return Optional.empty();
        } catch (RestClientException exception) {
            log.warn("Open Library cover lookup failed for ISBN {}: {}", normalizedIsbn, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> googleBooksCover(Book book) {
        URI uri = UriComponentsBuilder.fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", "intitle:" + book.getTitle() + " inauthor:" + book.getAuthor())
                .queryParam("maxResults", 8)
                .build().encode().toUri();
        try {
            String json = http.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            if (json == null || json.isBlank()) return Optional.empty();
            JsonNode items = objectMapper.readTree(json).path("items");
            for (JsonNode item : items) {
                JsonNode volume = item.path("volumeInfo");
                if (!sameBook(volume, book)) continue;
                String imageUrl = volume.path("imageLinks").path("thumbnail").asText("");
                if (imageUrl.isBlank()) imageUrl = volume.path("imageLinks").path("smallThumbnail").asText("");
                if (isGoogleImageUrl(imageUrl)) return Optional.of(imageUrl.replaceFirst("^http:", "https:"));
            }
        } catch (JsonProcessingException exception) {
            log.warn("Could not parse Google Books results for \"{}\" by {}: {}", book.getTitle(), book.getAuthor(), exception.getOriginalMessage());
        } catch (RestClientResponseException exception) {
            log.warn("Google Books cover lookup failed for \"{}\" by {} (HTTP {}).",
                    book.getTitle(), book.getAuthor(), exception.getStatusCode().value());
        } catch (RestClientException exception) {
            log.warn("Google Books cover lookup failed for \"{}\" by {}: {}",
                    book.getTitle(), book.getAuthor(), exception.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    private boolean sameBook(JsonNode volume, Book book) {
        if (!normalize(volume.path("title").asText()).equals(normalize(book.getTitle()))) return false;
        List<String> expectedAuthors = authorParts(book.getAuthor());
        List<String> actualAuthors = new ArrayList<>();
        for (JsonNode author : volume.path("authors")) actualAuthors.add(normalize(author.asText()));
        return !expectedAuthors.isEmpty() && expectedAuthors.stream().allMatch(expected ->
                actualAuthors.stream().anyMatch(actual -> actual.equals(expected)));
    }

    private boolean isGoogleImageUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return host != null && (host.equalsIgnoreCase("books.google.com")
                    || host.equalsIgnoreCase("books.googleusercontent.com"))
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private List<String> authorParts(String author) {
        if (author == null || author.isBlank()) return List.of();
        String normalized = normalize(author);
        if (normalized.startsWith("and ")) normalized = normalized.substring(4);
        String[] parts = normalized.split("\\s+and\\s+|\\s*&\\s*|\\s*;\\s*");
        return java.util.Arrays.stream(parts).filter(part -> !part.isBlank()).toList();
    }

    private String cacheKey(Book book) {
        if (book.getIsbn() != null && !book.getIsbn().isBlank())
            return "isbn:" + book.getIsbn().replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        return "title-author:" + titleAuthorKey(book.getTitle(), book.getAuthor());
    }

    private String titleAuthorKey(String title, String author) {
        return normalize(title) + "|" + normalize(author == null ? "" : author.replace("&", " and "));
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private record CachedLookup(Optional<String> coverUrl, Instant attemptedAt) {}
}
