package in.bibliotheca.controller;

import in.bibliotheca.entity.Book;
import in.bibliotheca.repository.BookRepository;
import in.bibliotheca.repository.LoanRepository;
import in.bibliotheca.service.LibraryService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookController {
    private final BookRepository books;
    private final LoanRepository loans;
    private final LibraryService library;

    public BookController(BookRepository books, LoanRepository loans, LibraryService library) {
        this.books = books; this.loans = loans; this.library = library;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "all") String availability,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Book> results = library.searchBooks(q, category, availability, page);
        model.addAttribute("books", results);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("availability", availability);
        model.addAttribute("categories", books.findDistinctCategories());
        return "books/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("bookId", null);
        return "books/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Book book = books.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        model.addAttribute("book", book);
        model.addAttribute("history", loans.findByBookIdOrderByIssueDateDesc(id));
        return "books/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", books.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found.")));
        model.addAttribute("bookId", id);
        return "books/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Book form, BindingResult errors, @RequestParam(required = false) Long id,
                       Model model, RedirectAttributes redirect) {
        validateExternalUrl(form.getCoverUrl(), "coverUrl", errors);
        validateExternalUrl(form.getPreviewUrl(), "previewUrl", errors);
        if (errors.hasErrors()) {
            model.addAttribute("bookId", id);
            return "books/form";
        }
        Book target = id == null ? new Book() : books.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        int borrowed = id == null ? 0 : target.getTotalCopies() - target.getAvailableCopies();
        target.setIsbn(form.getIsbn()); target.setTitle(form.getTitle()); target.setAuthor(form.getAuthor());
        target.setPublisher(form.getPublisher()); target.setCategory(form.getCategory()); target.setPublicationYear(form.getPublicationYear());
        target.setDescription(form.getDescription()); target.setLanguage(form.getLanguage()); target.setShelfLocation(form.getShelfLocation());
        target.setCoverUrl(form.getCoverUrl()); target.setPreviewUrl(form.getPreviewUrl());
        if (id == null) {
            target.setTotalCopies(form.getTotalCopies());
            target.setAvailableCopies(form.getTotalCopies());
        } else {
            if (form.getTotalCopies() < borrowed) {
                model.addAttribute("book", form);
                model.addAttribute("bookId", id);
                model.addAttribute("copyError", "Total copies cannot be less than the number currently on loan (" + borrowed + ").");
                return "books/form";
            }
            target.setTotalCopies(form.getTotalCopies());
            target.setAvailableCopies(form.getTotalCopies() - borrowed);
        }
        books.save(target);
        redirect.addFlashAttribute("success", "Book saved successfully.");
        return "redirect:/books";
    }

    private void validateExternalUrl(String value, String field, BindingResult errors) {
        if (value == null || value.isBlank()) return;
        try {
            String scheme = URI.create(value).getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
                errors.rejectValue(field, "url.scheme", "Use an http or https link.");
        } catch (IllegalArgumentException ex) {
            errors.rejectValue(field, "url.invalid", "Enter a valid http or https link.");
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        Book book = books.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (loans.existsByBookId(id)) {
            redirect.addFlashAttribute("error", "A book with circulation history cannot be deleted; preserve the historical record.");
        } else {
            books.delete(book);
            redirect.addFlashAttribute("success", "Book removed from the catalogue.");
        }
        return "redirect:/books";
    }
}
