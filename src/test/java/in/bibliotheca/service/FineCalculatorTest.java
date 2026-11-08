package in.bibliotheca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import in.bibliotheca.config.LibraryRules;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FineCalculatorTest {
    private final FineCalculator calculator = new FineCalculator(new LibraryRules());

    @Test
    void returnOnDueDateHasNoFine() {
        LocalDate due = LocalDate.of(2026, 10, 10);
        assertEquals(new BigDecimal("0.00"), calculator.calculate(due, due));
    }

    @Test
    void returnBeforeDueDateHasNoFine() {
        assertEquals(new BigDecimal("0.00"), calculator.calculate(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 8)));
    }

    @Test
    void chargesConfiguredRateForEachOverdueDay() {
        assertEquals(new BigDecimal("6.00"), calculator.calculate(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 13)));
    }
}
