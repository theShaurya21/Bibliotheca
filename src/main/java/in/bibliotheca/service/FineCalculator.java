package in.bibliotheca.service;

import in.bibliotheca.config.LibraryRules;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class FineCalculator {
    private final LibraryRules rules;
    public FineCalculator(LibraryRules rules) { this.rules = rules; }

    public BigDecimal calculate(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
        return rules.getFinePerDay().multiply(BigDecimal.valueOf(daysLate));
    }
}
