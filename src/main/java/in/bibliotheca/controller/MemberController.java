package in.bibliotheca.controller;

import in.bibliotheca.entity.Member;
import in.bibliotheca.repository.*;
import in.bibliotheca.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MemberController {
    private final MemberRepository members;
    private final LoanRepository loans;
    private final FinePaymentRepository payments;
    private final LibraryService library;
    public MemberController(MemberRepository members, LoanRepository loans, FinePaymentRepository payments, LibraryService library) {
        this.members = members; this.loans = loans; this.payments = payments; this.library = library;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("members", library.searchMembers(q, page));
        model.addAttribute("q", q);
        return "members/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("member", new Member());
        model.addAttribute("memberId", null);
        return "members/form";
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        Member member = members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found."));
        model.addAttribute("member", member);
        model.addAttribute("history", loans.findByMemberIdOrderByIssueDateDesc(id));
        model.addAttribute("activeLoanCount", loans.countByMemberIdAndStatus(id, in.bibliotheca.entity.Loan.Status.ACTIVE));
        model.addAttribute("finePayments", payments.findByMemberId(id));
        return "members/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("member", members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found.")));
        model.addAttribute("memberId", id);
        return "members/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Member form, BindingResult errors, @RequestParam(required = false) Long id,
                       Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) { model.addAttribute("memberId", id); return "members/form"; }
        Member target = id == null ? new Member() : members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found."));
        target.setFullName(form.getFullName()); target.setEmail(form.getEmail()); target.setPhone(form.getPhone());
        target.setMembershipType(form.getMembershipType()); target.setMembershipStart(form.getMembershipStart());
        target.setMembershipExpiry(form.getMembershipExpiry()); target.setDepartment(form.getDepartment());
        if (id != null) target.setActive(form.isActive());
        members.save(target);
        redirect.addFlashAttribute("success", "Member saved successfully.");
        return "redirect:/members";
    }

    @PostMapping("/{id}/status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirect) {
        Member member = members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found."));
        member.setActive(!member.isActive());
        members.save(member);
        redirect.addFlashAttribute("success", "Member status updated.");
        return "redirect:/members/" + id;
    }
}
