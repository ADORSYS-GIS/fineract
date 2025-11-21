package org.apache.fineract.config.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * Loan Product definition.
 *
 * <p>Defines loan product terms, interest rates, charges, and accounting rules.
 */
@Data
public class LoanProduct {
  private String name;
  private String shortName;
  private String description;
  private String fundSourceName; // Reference to fund source
  private String currencyCode;
  private Integer digitsAfterDecimal;

  // Principal limits
  private BigDecimal principal;
  private BigDecimal minPrincipal;
  private BigDecimal maxPrincipal;

  // Loan term limits
  private Integer numberOfRepayments;
  private Integer minNumberOfRepayments;
  private Integer maxNumberOfRepayments;
  private String repaymentFrequencyType; // DAYS, WEEKS, MONTHS, YEARS
  private Integer repaymentEvery;

  // Interest configuration
  private BigDecimal interestRatePerPeriod;
  private BigDecimal minInterestRatePerPeriod;
  private BigDecimal maxInterestRatePerPeriod;
  private String interestRateFrequencyType; // MONTH, YEAR
  private String interestType; // FLAT, DECLINING_BALANCE
  private String interestCalculationPeriodType; // DAILY, SAME_AS_REPAYMENT_PERIOD
  private String amortizationType; // EQUAL_INSTALLMENTS, EQUAL_PRINCIPAL

  // Charges
  private List<String> chargeNames = new ArrayList<>();

  // Accounting
  private String accountingRule; // NONE, CASH_BASED, ACCRUAL_PERIODIC, ACCRUAL_UPFRONT
  private String fundSourceAccountCode;
  private String loanPortfolioAccountCode;
  private String interestOnLoansAccountCode;
  private String incomeFromFeesAccountCode;
  private String incomeFromPenaltiesAccountCode;
  private String writeOffAccountCode;

  // Settings
  private Boolean includeInBorrowerCycle;
  private Boolean useBorrowerCycle;
  private Boolean multiDisburseLoan;
  private Integer maxTrancheCount;
  private BigDecimal outstandingLoanBalance;

  // Additional business logic fields
  private Boolean allowPartialPeriodInterestCalculation;

  @JsonAlias("minimumGapBetweenInstallments")
  private Integer minimumDaysBetweenDisbursalAndFirstRepayment;

  // Additional accounting fields
  private Long receivableInterestAccountId;
  private Long receivableFeeAccountId;
  private Long receivablePenaltyAccountId;
  private Long transfersInSuspenseAccountId;
  private Long goodwillCreditAccountId;
  private Long incomeFromRecoveryAccountId;
  private Long overpaymentLiabilityAccountId;
}
