package in.bibliotheca.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "books", indexes = @Index(name = "idx_book_title", columnList = "title"))
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, length = 20)
    @Size(max = 20)
    @Pattern(regexp = "(?i)(?:\\d{9}[\\dX]|\\d{13})", message = "Enter a 10- or 13-digit ISBN.")
    private String isbn;
    @NotBlank @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String title;
    @NotBlank @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String author;
    @Size(max = 140)
    private String publisher;
    @Size(max = 80)
    private String category;
    @Min(1000) @Max(2100)
    private Integer publicationYear;
    @Column(length = 1500)
    private String description;
    @Size(max = 60)
    private String language = "English";
    @Size(max = 140)
    private String shelfLocation;
    @NotNull @Min(0)
    private Integer totalCopies = 1;
    @NotNull @Min(0)
    private Integer availableCopies = 1;
    @Size(max = 500)
    private String coverUrl;
    @Size(max = 500)
    private String previewUrl;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version;

    public Book() {}
    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) {
        this.isbn = isbn == null || isbn.isBlank() ? null : isbn.trim().replaceAll("[\\s-]", "");
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
