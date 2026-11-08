package in.bibliotheca.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members", indexes = @Index(name = "idx_member_name", columnList = "fullName"))
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 140)
    @Column(nullable = false, length = 140)
    private String fullName;
    @NotBlank @Email @Size(max = 180)
    @Column(nullable = false, unique = true, length = 180)
    private String email;
    @Size(max = 30)
    private String phone;
    @Size(max = 40)
    private String membershipType = "Student";
    @NotNull
    private LocalDate membershipStart = LocalDate.now();
    @NotNull
    private LocalDate membershipExpiry = LocalDate.now().plusYears(1);
    @Column(nullable = false)
    private boolean active = true;
    @Size(max = 100)
    private String department;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Member() {}
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim().toLowerCase(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }
    public LocalDate getMembershipStart() { return membershipStart; }
    public void setMembershipStart(LocalDate membershipStart) { this.membershipStart = membershipStart; }
    public LocalDate getMembershipExpiry() { return membershipExpiry; }
    public void setMembershipExpiry(LocalDate membershipExpiry) { this.membershipExpiry = membershipExpiry; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    @AssertTrue(message = "Membership expiry must be on or after the start date.")
    public boolean isMembershipPeriodValid() {
        return membershipStart == null || membershipExpiry == null || !membershipExpiry.isBefore(membershipStart);
    }
}
