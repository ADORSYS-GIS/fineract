package org.apache.fineract.config.model.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * Savings Product definition.
 *
 * <p>Defines savings account product terms, interest rates, and accounting rules.
 */
@Data
public class SavingsProduct {
  private String name;
  private String shortName;
  private String description;
  private String currencyCode;
  private Integer digitsAfterDecimal;

  // Interest configuration
  private BigDecimal nominalAnnualInterestRate;
  private String interestCompoundingPeriodType; // DAILY, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
  private String interestPostingPeriodType; // MONTHLY, QUARTERLY, BIANNUAL, ANNUAL
  private String interestCalculationType; // DAILY_BALANCE, AVERAGE_DAILY_BALANCE
  private String interestCalculationDaysInYearType; // DAYS_360, DAYS_365

  // Balance requirements
  private BigDecimal minRequiredOpeningBalance;
  private BigDecimal minBalanceForInterestCalculation;
  private Boolean enforceMinRequiredBalance;
  private BigDecimal minRequiredBalance;

  // Withdrawal settings
  private Boolean withdrawalFeeForTransfers;
  private Boolean allowOverdraft;
  private BigDecimal overdraftLimit;

  // Charges
  private List<String> chargeNames = new ArrayList<>();

  // Lockin period
  private Integer lockinPeriodFrequency;
  private String lockinPeriodFrequencyType; // DAYS, WEEKS, MONTHS, YEARS

  // Dormancy tracking
  @JsonAlias("allowDormancyTracking")
  private Boolean isDormancyTrackingActive;

  private Long daysToInactive;
  private Long daysToDormancy;

  // Accounting
  private String accountingRule; // NONE, CASH_BASED
  private String savingsReferenceAccountCode;
  private String savingsControlAccountCode;
  private String transfersInSuspenseAccountCode;
  private String interestOnSavingsAccountCode;
  private String incomeFromFeesAccountCode;
  private String incomeFromPenaltiesAccountCode;
  private String overdraftPortfolioControlAccountCode;
  private String incomeFromInterestAccountCode;
  private Long escheatLiabilityAccountId;
}
