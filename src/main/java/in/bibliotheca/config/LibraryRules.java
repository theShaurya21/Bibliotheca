package in.bibliotheca.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rules")
public class LibraryRules {
    private int maximumActiveLoans = 3;
    private int loanPeriodDays = 14;
    private BigDecimal finePerDay = new BigDecimal("2.00");
    private int maximumRenewals = 1;

    public int getMaximumActiveLoans() { return maximumActiveLoans; }
    public void setMaximumActiveLoans(int value) { maximumActiveLoans = value; }
    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int value) { loanPeriodDays = value; }
    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal value) { finePerDay = value; }
    public int getMaximumRenewals() { return maximumRenewals; }
    public void setMaximumRenewals(int value) { maximumRenewals = value; }
}
