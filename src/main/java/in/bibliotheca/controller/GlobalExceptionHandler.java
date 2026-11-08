package in.bibliotheca.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String duplicateData(Model model) {
        model.addAttribute("message", "That value conflicts with an existing record. Check that ISBN and email addresses are unique.");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String invalidRequest(IllegalArgumentException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error";
    }
}
