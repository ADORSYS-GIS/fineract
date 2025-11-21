package org.apache.fineract.config.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized utility for mapping human-readable enum strings to Fineract API integer IDs.
 *
 * <p>This class consolidates all enum-to-ID mapping logic across the application, following the
 * graceful fallback pattern from WorkingDaysLoader:
 *
 * <ul>
 *   <li>1. Try parsing as integer (allows direct integer input from YAML)
 *   <li>2. Map enum name to integer ID (allows human-readable strings)
 *   <li>3. Log warning and use default on unknown values (resilient to bad data)
 * </ul>
 *
 * <p><b>Reference Implementation:</b> WorkingDaysLoader.convertRepaymentRescheduleType()
 *
 * <p><b>Audit Source:</b> AUDIT_REPORT.md - 28 enum methods across 11 loaders
 */
@Slf4j
public class FineractEnumMapper {

  // ===========================================================================================
  // CHARGE ENUMS (ChargeLoader.java - 5 methods)
  // ===========================================================================================

  /**
   * Maps ChargeAppliesTo enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>LOAN = 1
   *   <li>SAVINGS = 2
   *   <li>CLIENT = 3
   *   <li>SHARES = 4
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - LOAN)
   */
  public static Integer mapChargeAppliesTo(String value) {
    if (value == null || value.isBlank()) {
      log.warn("ChargeAppliesTo value is null/empty, defaulting to LOAN (1)");
      return 1;
    }

    // Try parsing as integer
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    // Map enum name to integer ID
    return switch (value.toUpperCase().trim()) {
      case "LOAN" -> 1;
      case "SAVINGS" -> 2;
      case "CLIENT" -> 3;
      case "SHARES" -> 4;
      default -> {
        log.warn("Unknown ChargeAppliesTo value: '{}', defaulting to LOAN (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps ChargeTimeType enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DISBURSEMENT = 1
   *   <li>SPECIFIED_DUE_DATE = 2
   *   <li>INSTALMENT_FEE = 3
   *   <li>OVERDUE_INSTALLMENT = 4
   *   <li>SAVINGS_ACTIVATION = 5
   *   <li>WITHDRAWAL_FEE = 6
   *   <li>ANNUAL_FEE = 7
   *   <li>MONTHLY_FEE = 8
   *   <li>WEEKLY_FEE = 9
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - DISBURSEMENT)
   */
  public static Integer mapChargeTimeType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("ChargeTimeType value is null/empty, defaulting to DISBURSEMENT (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    // Loan charges: 1, 2, 8, 9, 12
    // Savings charges: 2, 3, 4, 5, 6, 7, 10, 11, 16
    return switch (value.toUpperCase().trim()) {
      case "DISBURSEMENT" -> 1;
      case "SPECIFIED_DUE_DATE", "SPECIFIED DUE DATE" -> 2;
      case "INSTALMENT_FEE", "INSTALLMENT_FEE", "INSTALLMENT FEE", "INSTALMENT FEE" -> 3;
      case "OVERDUE_INSTALLMENT",
          "OVERDUE_INSTALMENT",
          "OVERDUE INSTALLMENT",
          "OVERDUE INSTALMENT" -> 4;
      case "SAVINGS_ACTIVATION", "SAVINGS ACTIVATION", "ACTIVATION" -> 5;
      case "WITHDRAWAL_FEE", "WITHDRAWAL FEE", "WITHDRAWAL" -> 6;
      case "ANNUAL_FEE", "ANNUAL FEE" -> 7;
      case "MONTHLY_FEE", "MONTHLY FEE" -> 8;
      case "WEEKLY_FEE", "WEEKLY FEE" -> 9;
      case "OVERDRAFT_FEE", "OVERDRAFT FEE" -> 10;
      case "SAVINGS_CLOSURE", "SAVINGS CLOSURE" -> 11;
      case "TRANCHE_DISBURSEMENT", "TRANCHE DISBURSEMENT" -> 12;
      case "FDA_PRE_CLOSURE", "FDA PRE CLOSURE" -> 16;
      default -> {
        log.warn("Unknown ChargeTimeType value: '{}', defaulting to DISBURSEMENT (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps ChargeCalculationType enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>FLAT = 1
   *   <li>PERCENT_OF_AMOUNT = 2
   *   <li>PERCENT_OF_AMOUNT_AND_INTEREST = 3
   *   <li>PERCENT_OF_INTEREST = 4
   *   <li>PERCENT_OF_DISBURSEMENT_AMOUNT = 5
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - FLAT)
   */
  public static Integer mapChargeCalculationType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("ChargeCalculationType value is null/empty, defaulting to FLAT (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "FLAT" -> 1;
      case "PERCENT_OF_AMOUNT", "PERCENTAGE OF AMOUNT" -> 2;
      case "PERCENT_OF_AMOUNT_AND_INTEREST", "PERCENTAGE OF AMOUNT AND INTEREST" -> 3;
      case "PERCENT_OF_INTEREST", "PERCENTAGE OF INTEREST" -> 4;
      case "PERCENT_OF_DISBURSEMENT_AMOUNT", "PERCENTAGE OF DISBURSEMENT AMOUNT" -> 5;
      default -> {
        log.warn("Unknown ChargeCalculationType value: '{}', defaulting to FLAT (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps ChargePaymentMode enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>REGULAR = 0
   *   <li>ACCOUNT_TRANSFER = 1
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 0 - REGULAR)
   */
  public static Integer mapChargePaymentMode(String value) {
    if (value == null || value.isBlank()) {
      log.warn("ChargePaymentMode value is null/empty, defaulting to REGULAR (0)");
      return 0;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "REGULAR" -> 0;
      case "ACCOUNT_TRANSFER" -> 1;
      default -> {
        log.warn("Unknown ChargePaymentMode value: '{}', defaulting to REGULAR (0)", value);
        yield 0;
      }
    };
  }

  /**
   * Maps FeeInterval enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAYS = 0
   *   <li>WEEKS = 1
   *   <li>MONTHS = 2
   *   <li>YEARS = 3
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 2 - MONTHS)
   */
  public static Integer mapFeeInterval(String value) {
    if (value == null || value.isBlank()) {
      log.warn("FeeInterval value is null/empty, defaulting to MONTHS (2)");
      return 2;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAYS", "DAY" -> 0;
      case "WEEKS", "WEEK" -> 1;
      case "MONTHS", "MONTH" -> 2;
      case "YEARS", "YEAR" -> 3;
      default -> {
        log.warn("Unknown FeeInterval value: '{}', defaulting to MONTHS (2)", value);
        yield 2;
      }
    };
  }

  // ===========================================================================================
  // CHART OF ACCOUNTS ENUMS (ChartOfAccountsLoader.java - 2 methods)
  // ===========================================================================================

  /**
   * Maps GLAccountType enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>ASSET = 1
   *   <li>LIABILITY = 2
   *   <li>EQUITY = 3
   *   <li>INCOME = 4
   *   <li>EXPENSE = 5
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - ASSET)
   */
  public static Integer mapGLAccountType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("GLAccountType value is null/empty, defaulting to ASSET (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "ASSET" -> 1;
      case "LIABILITY" -> 2;
      case "EQUITY" -> 3;
      case "INCOME" -> 4;
      case "EXPENSE" -> 5;
      default -> {
        log.warn("Unknown GLAccountType value: '{}', defaulting to ASSET (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps GLAccountUsage enum to integer ID.
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DETAIL = 1
   *   <li>HEADER = 2
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - DETAIL)
   */
  public static Integer mapGLAccountUsage(String value) {
    if (value == null || value.isBlank()) {
      log.warn("GLAccountUsage value is null/empty, defaulting to DETAIL (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DETAIL" -> 1;
      case "HEADER" -> 2;
      default -> {
        log.warn("Unknown GLAccountUsage value: '{}', defaulting to DETAIL (1)", value);
        yield 1;
      }
    };
  }

  // ===========================================================================================
  // CLIENT ENUMS (ClientLoader.java - 1 method)
  // ===========================================================================================

  /**
   * Maps Gender enum to integer ID.
   *
   * <p><b>WARNING:</b> These IDs reference system code values that may not exist in all Fineract
   * instances. Verify code values exist before using.
   *
   * <p>Common Fineract API values:
   *
   * <ul>
   *   <li>MALE = 22
   *   <li>FEMALE = 23
   *   <li>OTHER = 24
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 22 - MALE)
   */
  public static Integer mapGender(String value) {
    if (value == null || value.isBlank()) {
      log.warn("Gender value is null/empty, defaulting to MALE (22)");
      return 22;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "MALE", "M" -> 22;
      case "FEMALE", "F" -> 23;
      case "OTHER", "O" -> 24;
      default -> {
        log.warn("Unknown Gender value: '{}', defaulting to MALE (22)", value);
        yield 22;
      }
    };
  }

  // ===========================================================================================
  // FINANCIAL ACTIVITY ENUMS (FinancialActivityMappingLoader.java - 1 method)
  // ===========================================================================================

  /**
   * Maps FinancialActivity enum to integer ID.
   *
   * <p>Fineract API values (hundred-based numbering):
   *
   * <ul>
   *   <li>ASSET_FUND_SOURCE = 100
   *   <li>LIABILITY_FUND_SOURCE = 101
   *   <li>ASSET_TRANSFER = 200
   *   <li>LIABILITY_TRANSFER = 201
   *   <li>OPENING_BALANCES_TRANSFER_CONTRA = 202
   *   <li>CASH_AT_MAINVAULT = 300
   *   <li>CASH_AT_TELLER = 301
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 100 - ASSET_FUND_SOURCE)
   */
  public static Integer mapFinancialActivity(String value) {
    if (value == null || value.isBlank()) {
      log.warn("FinancialActivity value is null/empty, defaulting to ASSET_FUND_SOURCE (100)");
      return 100;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    // Normalize value: convert spaces to underscores, handle common aliases
    String normalized = value.toUpperCase().trim().replace(" ", "_");

    // Fineract actual financial activity IDs (from /financialactivityaccounts/template):
    // assetTransfer = 100 (ASSET)
    // cashAtMainVault = 101 (ASSET)
    // cashAtTeller = 102 (ASSET)
    // fundSource = 103 (ASSET)
    // liabilityTransfer = 200 (LIABILITY)
    // payableDividends = 201 (LIABILITY)
    // openingBalancesTransferContra = 300 (EQUITY)
    return switch (normalized) {
      case "ASSET_TRANSFER" -> 100;
      case "CASH_AT_MAINVAULT" -> 101;
      case "CASH_AT_TELLER" -> 102;
      case "FUND_SOURCE", "ASSET_FUND_SOURCE" -> 103;
      case "LIABILITY_TRANSFER" -> 200;
      case "PAYABLE_DIVIDENDS", "LIABILITY_FUND_SOURCE" -> 201;
      case "OPENING_BALANCES_TRANSFER_CONTRA", "OPENING_BALANCES_CONTRA" -> 300;
      default -> {
        log.warn("Unknown FinancialActivity value: '{}', defaulting to FUND_SOURCE (103)", value);
        yield 103;
      }
    };
  }

  // ===========================================================================================
  // FREQUENCY ENUMS (shared across multiple loaders - 3+ usages)
  // ===========================================================================================

  /**
   * Maps RepaymentFrequencyType enum to integer ID.
   *
   * <p>Used by: LoanAccountLoader, LoanProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAYS = 0
   *   <li>WEEKS = 1
   *   <li>MONTHS = 2
   *   <li>YEARS = 3
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 2 - MONTHS)
   */
  public static Integer mapRepaymentFrequencyType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("RepaymentFrequencyType value is null/empty, defaulting to MONTHS (2)");
      return 2;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAYS", "DAY" -> 0;
      case "WEEKS", "WEEK" -> 1;
      case "MONTHS", "MONTH" -> 2;
      case "YEARS", "YEAR" -> 3;
      default -> {
        log.warn("Unknown RepaymentFrequencyType value: '{}', defaulting to MONTHS (2)", value);
        yield 2;
      }
    };
  }

  /**
   * Maps InterestType enum to integer ID.
   *
   * <p>Used by: LoanAccountLoader, LoanProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DECLINING_BALANCE = 0
   *   <li>FLAT = 1
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 0 - DECLINING_BALANCE)
   */
  public static Integer mapInterestType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("InterestType value is null/empty, defaulting to DECLINING_BALANCE (0)");
      return 0;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DECLINING_BALANCE", "DECLINING" -> 0;
      case "FLAT" -> 1;
      default -> {
        log.warn("Unknown InterestType value: '{}', defaulting to DECLINING_BALANCE (0)", value);
        yield 0;
      }
    };
  }

  /**
   * Maps AmortizationType enum to integer ID.
   *
   * <p>Used by: LoanAccountLoader, LoanProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>EQUAL_PRINCIPAL = 0
   *   <li>EQUAL_INSTALLMENTS = 1
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - EQUAL_INSTALLMENTS)
   */
  public static Integer mapAmortizationType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("AmortizationType value is null/empty, defaulting to EQUAL_INSTALLMENTS (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "EQUAL_PRINCIPAL" -> 0;
      case "EQUAL_INSTALLMENTS", "EQUAL_INSTALMENTS" -> 1;
      default -> {
        log.warn(
            "Unknown AmortizationType value: '{}', defaulting to EQUAL_INSTALLMENTS (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps InterestCalculationPeriodType enum to integer ID.
   *
   * <p>Used by: LoanAccountLoader, LoanProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAILY = 0
   *   <li>SAME_AS_REPAYMENT_PERIOD = 1
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - SAME_AS_REPAYMENT_PERIOD)
   */
  public static Integer mapInterestCalculationPeriodType(String value) {
    if (value == null || value.isBlank()) {
      log.warn(
          "InterestCalculationPeriodType value is null/empty, defaulting to"
              + " SAME_AS_REPAYMENT_PERIOD (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAILY" -> 0;
      case "SAME_AS_REPAYMENT_PERIOD" -> 1;
      default -> {
        log.warn(
            "Unknown InterestCalculationPeriodType value: '{}', defaulting to"
                + " SAME_AS_REPAYMENT_PERIOD (1)",
            value);
        yield 1;
      }
    };
  }

  // ===========================================================================================
  // LOAN PRODUCT SPECIFIC ENUMS (LoanProductLoader.java - 2 additional methods)
  // ===========================================================================================

  /**
   * Maps InterestRateFrequencyType enum to integer ID.
   *
   * <p>Used by: LoanProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>MONTH = 2
   *   <li>YEAR = 3
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 2 - MONTH)
   */
  public static Integer mapInterestRateFrequencyType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("InterestRateFrequencyType value is null/empty, defaulting to MONTH (2)");
      return 2;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "MONTH", "MONTHS", "PER_MONTH" -> 2;
      case "YEAR", "YEARS", "PER_YEAR" -> 3;
      default -> {
        log.warn("Unknown InterestRateFrequencyType value: '{}', defaulting to MONTH (2)", value);
        yield 2;
      }
    };
  }

  /**
   * Maps AccountingRule enum to integer ID.
   *
   * <p>Used by: LoanProductLoader, SavingsProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>NONE = 1
   *   <li>CASH_BASED = 2
   *   <li>ACCRUAL_PERIODIC = 3 (Loan products only)
   *   <li>ACCRUAL_UPFRONT = 4 (Loan products only)
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 2 - CASH_BASED)
   */
  public static Integer mapAccountingRule(String value) {
    if (value == null || value.isBlank()) {
      log.warn("AccountingRule value is null/empty, defaulting to CASH_BASED (2)");
      return 2;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "NONE" -> 1;
      case "CASH_BASED", "CASH" -> 2;
      case "ACCRUAL_PERIODIC", "PERIODIC" -> 3;
      case "ACCRUAL_UPFRONT", "UPFRONT" -> 4;
      default -> {
        log.warn("Unknown AccountingRule value: '{}', defaulting to CASH_BASED (2)", value);
        yield 2;
      }
    };
  }

  // ===========================================================================================
  // SAVINGS PRODUCT ENUMS (SavingsProductLoader.java - 4 methods)
  // ===========================================================================================

  /**
   * Maps InterestCompoundingPeriodType enum to integer ID.
   *
   * <p>Used by: SavingsProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAILY = 1
   *   <li>MONTHLY = 4
   *   <li>QUARTERLY = 5
   *   <li>SEMI_ANNUAL = 6
   *   <li>ANNUAL = 7
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - DAILY)
   */
  public static Integer mapInterestCompoundingPeriodType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("InterestCompoundingPeriodType value is null/empty, defaulting to DAILY (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAILY" -> 1;
      case "MONTHLY", "MONTH" -> 4;
      case "QUARTERLY", "QUARTER" -> 5;
      case "SEMI_ANNUAL", "SEMIANNUAL", "BIANNUAL" -> 6;
      case "ANNUAL", "YEARLY", "YEAR" -> 7;
      default -> {
        log.warn(
            "Unknown InterestCompoundingPeriodType value: '{}', defaulting to DAILY (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps InterestPostingPeriodType enum to integer ID.
   *
   * <p>Used by: SavingsProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>MONTHLY = 4
   *   <li>QUARTERLY = 5
   *   <li>BIANNUAL = 6
   *   <li>ANNUAL = 7
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 4 - MONTHLY)
   */
  public static Integer mapInterestPostingPeriodType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("InterestPostingPeriodType value is null/empty, defaulting to MONTHLY (4)");
      return 4;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "MONTHLY", "MONTH" -> 4;
      case "QUARTERLY", "QUARTER" -> 5;
      case "BIANNUAL", "SEMI_ANNUAL", "SEMIANNUAL" -> 6;
      case "ANNUAL", "YEARLY", "YEAR" -> 7;
      default -> {
        log.warn("Unknown InterestPostingPeriodType value: '{}', defaulting to MONTHLY (4)", value);
        yield 4;
      }
    };
  }

  /**
   * Maps InterestCalculationType enum to integer ID.
   *
   * <p>Used by: SavingsProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAILY_BALANCE = 1
   *   <li>AVERAGE_DAILY_BALANCE = 2
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 1 - DAILY_BALANCE)
   */
  public static Integer mapInterestCalculationType(String value) {
    if (value == null || value.isBlank()) {
      log.warn("InterestCalculationType value is null/empty, defaulting to DAILY_BALANCE (1)");
      return 1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAILY_BALANCE" -> 1;
      case "AVERAGE_DAILY_BALANCE" -> 2;
      default -> {
        log.warn(
            "Unknown InterestCalculationType value: '{}', defaulting to DAILY_BALANCE (1)", value);
        yield 1;
      }
    };
  }

  /**
   * Maps InterestCalculationDaysInYearType enum to integer ID.
   *
   * <p>Used by: SavingsProductLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>DAYS_360 = 360
   *   <li>DAYS_365 = 365
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 365 - DAYS_365)
   */
  public static Integer mapInterestCalculationDaysInYearType(String value) {
    if (value == null || value.isBlank()) {
      log.warn(
          "InterestCalculationDaysInYearType value is null/empty, defaulting to DAYS_365 (365)");
      return 365;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    return switch (value.toUpperCase().trim()) {
      case "DAYS_360", "360" -> 360;
      case "DAYS_365", "365" -> 365;
      default -> {
        log.warn(
            "Unknown InterestCalculationDaysInYearType value: '{}', defaulting to DAYS_365 (365)",
            value);
        yield 365;
      }
    };
  }

  // ===========================================================================================
  // TRANSACTION TYPE ENUMS (Command string mapping - NOT integer IDs)
  // ===========================================================================================

  /**
   * Maps LoanTransactionType enum to Fineract API command string.
   *
   * <p><b>NOTE:</b> This is NOT an integer ID mapping - returns command strings for API endpoints.
   *
   * <p>Used by: LoanTransactionLoader
   *
   * <p>Fineract API commands:
   *
   * <ul>
   *   <li>REPAYMENT → "repayment"
   *   <li>WAIVER → "waiveinterest"
   *   <li>WRITEOFF → "writeoff"
   * </ul>
   *
   * @param value enum name
   * @return command string (defaults to "repayment")
   */
  public static String mapLoanTransactionTypeToCommand(String value) {
    if (value == null || value.isBlank()) {
      log.warn("LoanTransactionType value is null/empty, defaulting to 'repayment'");
      return "repayment";
    }

    return switch (value.toUpperCase().trim()) {
      case "REPAYMENT" -> "repayment";
      case "WAIVER", "WAIVE_INTEREST" -> "waiveinterest";
      case "WRITEOFF", "WRITE_OFF" -> "writeoff";
      default -> {
        log.warn("Unknown LoanTransactionType value: '{}', defaulting to 'repayment'", value);
        yield "repayment";
      }
    };
  }

  /**
   * Maps SavingsTransactionType enum to Fineract API command string.
   *
   * <p><b>NOTE:</b> This is NOT an integer ID mapping - returns command strings for API endpoints.
   *
   * <p>Used by: SavingsTransactionLoader
   *
   * <p>Fineract API commands:
   *
   * <ul>
   *   <li>DEPOSIT → "deposit"
   *   <li>WITHDRAWAL → "withdrawal"
   * </ul>
   *
   * @param value enum name
   * @return command string (defaults to "deposit")
   */
  public static String mapSavingsTransactionTypeToCommand(String value) {
    if (value == null || value.isBlank()) {
      log.warn("SavingsTransactionType value is null/empty, defaulting to 'deposit'");
      return "deposit";
    }

    return switch (value.toUpperCase().trim()) {
      case "DEPOSIT" -> "deposit";
      case "WITHDRAWAL", "WITHDRAW" -> "withdrawal";
      default -> {
        log.warn("Unknown SavingsTransactionType value: '{}', defaulting to 'deposit'", value);
        yield "deposit";
      }
    };
  }

  // ===========================================================================================
  // WORKING DAYS ENUM (WorkingDaysLoader.java - 1 method - REFERENCE IMPLEMENTATION)
  // ===========================================================================================

  /**
   * Maps RepaymentRescheduleType enum to integer ID.
   *
   * <p><b>REFERENCE IMPLEMENTATION</b> - This is the original pattern that informed all other
   * methods in this utility class.
   *
   * <p>Used by: WorkingDaysLoader
   *
   * <p>Fineract API values:
   *
   * <ul>
   *   <li>SAME_DAY = 1
   *   <li>MOVE_TO_NEXT_WORKING_DAY = 2
   *   <li>MOVE_TO_NEXT_REPAYMENT_MEETING_DAY = 3
   *   <li>MOVE_TO_PREVIOUS_WORKING_DAY = 4
   *   <li>MOVE_TO_NEXT_MEETING_DAY = 5
   * </ul>
   *
   * @param value enum name or integer string
   * @return integer ID (defaults to 2 - MOVE_TO_NEXT_WORKING_DAY)
   */
  public static Integer mapRepaymentRescheduleType(String value) {
    if (value == null || value.isBlank()) {
      log.warn(
          "RepaymentRescheduleType value is null/empty, defaulting to MOVE_TO_NEXT_WORKING_DAY"
              + " (2)");
      return 2;
    }

    // Try parsing as integer
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      // Not an integer, proceed with enum name mapping
    }

    // Map enum name to integer ID
    return switch (value.toUpperCase().trim()) {
      case "SAME_DAY" -> 1;
      case "MOVE_TO_NEXT_WORKING_DAY" -> 2;
      case "MOVE_TO_NEXT_REPAYMENT_MEETING_DAY" -> 3;
      case "MOVE_TO_PREVIOUS_WORKING_DAY" -> 4;
      case "MOVE_TO_NEXT_MEETING_DAY" -> 5;
      default -> {
        log.warn(
            "Unknown RepaymentRescheduleType value: '{}', defaulting to MOVE_TO_NEXT_WORKING_DAY"
                + " (2)",
            value);
        yield 2;
      }
    };
  }
}
