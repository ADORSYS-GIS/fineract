@YearEndRetainedEarning
Feature: Loan Year End Retained Earning

  Background:
    # Calendar fiscal year + income/expense band + close-out target.
    When Global config "last-day-of-financial-year" value set to "31"
    And Global config "last-month-of-financial-year" value set to "12"
    And Global config "income-expense-gl-accounts" value set to "400000-899999"
    And Global config "retained-gl-account" value set to "320000"
    And Global config "retained-earning-used-by-report-name" value set to "Trial Balance Summary Report with Asset Owner"

  @TestRailId:C85187
  Scenario: Verify that year-end close zeroes income accounts into retained earnings, is idempotent and next year starts clean
    # --- Arrange: a loan recognising interest income (404000) and fee income (404007) in FY2025 ---
    When Admin sets the business date to "01 December 2025"
    And Admin creates a client with random data
    And Admin creates a fully customized loan with the following data:
      | LoanProduct                                                   | submitted on date | with Principal | ANNUAL interest rate % | interest type     | interest calculation period | amortization type  | loanTermFrequency | loanTermFrequencyType | repaymentEvery | repaymentFrequencyType | numberOfRepayments | graceOnPrincipalPayment | graceOnInterestPayment | interest free period | Payment strategy            |
      | LP2_ADV_CUSTOM_PMT_ALLOC_PROGRESSIVE_LOAN_SCHEDULE_HORIZONTAL | 01 December 2025  | 100            | 26                     | DECLINING_BALANCE | DAILY                       | EQUAL_INSTALLMENTS | 3                 | MONTHS                | 1              | MONTHS                 | 3                  | 0                       | 0                      | 0                    | ADVANCED_PAYMENT_ALLOCATION |
    And Admin successfully approves the loan on "01 December 2025" with "100" amount and expected disbursement date on "01 December 2025"
    And Admin successfully disburse the loan on "01 December 2025" with "100" EUR transaction amount
    And Admin adds "LOAN_SNOOZE_FEE" due date charge with "15 December 2025" due date and 10 EUR transaction amount
    When Admin sets the business date to "31 December 2025"
    And Admin runs inline COB job for Loan
    And Customer makes "AUTOPAY" repayment on "31 December 2025" with 40 EUR transaction amount
    # --- Assert BEFORE close: income accounts carry non-zero balances (they accumulate indefinitely) ---
    Then Trial Balance Summary Report with Asset Owner for date "31 December 2025" has a row for GL account "404000" with non-zero ending balance
    And Trial Balance Summary Report with Asset Owner for date "31 December 2025" has a row for GL account "404007" with non-zero ending balance
    # --- Act: enter the new fiscal year and run the year-end close for FY2025 ---
    When Admin sets the business date to "02 January 2026"
    And Admin runs the Retained Earning Job
    # --- Assert AFTER close: both income accounts are zeroed, net carried to retained earnings per owner ---
    Then Trial Balance Summary Report with Asset Owner for date "01 January 2026" shows GL account "404000" closed out
    And Trial Balance Summary Report with Asset Owner for date "01 January 2026" shows GL account "404007" closed out
    And Trial Balance Summary Report with Asset Owner for date "01 January 2026" has a row for GL account "320000" with non-zero ending balance
    # --- Out-of-band balance-sheet account is never touched by the close ---
    And Trial Balance Summary Report with Asset Owner for date "01 January 2026" has a row for GL account "145023" with non-zero ending balance
    # --- Persisted close-out: exactly one retained earnings record (single asset owner: self) ---
    #And The journal entry annual summary table contains 1 row for GL code "320000" with year end date "31 December 2025"
    # --- Idempotency: re-running the job for the same fiscal year does nothing
    #     Double-posting would break the zero-sum and resurface 404000 with a positive balance. ---
    When Admin runs the Retained Earning Job
    Then Trial Balance Summary Report with Asset Owner for date "01 January 2026" shows GL account "404000" closed out
    #And The journal entry annual summary table contains 1 row for GL code "320000" with year end date "31 December 2025"
    # --- New-year accounting continues normally: COB posts fresh January accruals, so 404000 reappears
    #     with only the new period's (small) balance - the closed 2025 total is NOT resurfaced.
    When Admin sets the business date to "05 January 2026"
    And Admin runs inline COB job for Loan
    Then Trial Balance Summary Report with Asset Owner for date "05 January 2026" has a row for GL account "404000" with non-zero ending balance

  @TestRailId:C85188
  Scenario: Verify year-end close handles multiple loan products and multiple asset owners
    # Two loans on DIFFERENT products; the second is sold to an external asset owner before income accrues.
    When Admin sets the business date to "01 December 2026"
    And Admin creates a client with random data
    And Admin creates a fully customized loan with the following data:
      | LoanProduct                                                   | submitted on date | with Principal | ANNUAL interest rate % | interest type     | interest calculation period | amortization type  | loanTermFrequency | loanTermFrequencyType | repaymentEvery | repaymentFrequencyType | numberOfRepayments | graceOnPrincipalPayment | graceOnInterestPayment | interest free period | Payment strategy            |
      | LP2_ADV_CUSTOM_PMT_ALLOC_PROGRESSIVE_LOAN_SCHEDULE_HORIZONTAL | 01 December 2026  | 100            | 26                     | DECLINING_BALANCE | DAILY                       | EQUAL_INSTALLMENTS | 3                 | MONTHS                | 1              | MONTHS                 | 3                  | 0                       | 0                      | 0                    | ADVANCED_PAYMENT_ALLOCATION |
    And Admin successfully approves the loan on "01 December 2026" with "100" amount and expected disbursement date on "01 December 2026"
    And Admin successfully disburse the loan on "01 December 2026" with "100" EUR transaction amount
    And Admin creates a client with random data
    And Admin creates a fully customized loan with the following data:
      | LoanProduct                             | submitted on date | with Principal | ANNUAL interest rate % | interest type     | interest calculation period | amortization type  | loanTermFrequency | loanTermFrequencyType | repaymentEvery | repaymentFrequencyType | numberOfRepayments | graceOnPrincipalPayment | graceOnInterestPayment | interest free period | Payment strategy            |
      | LP2_ADV_PYMNT_INTEREST_DAILY_EMI_360_30 | 01 December 2026  | 1000           | 12                     | DECLINING_BALANCE | DAILY                       | EQUAL_INSTALLMENTS | 6                 | MONTHS                | 1              | MONTHS                 | 6                  | 0                       | 0                      | 0                    | ADVANCED_PAYMENT_ALLOCATION |
    And Admin successfully approves the loan on "01 December 2026" with "1000" amount and expected disbursement date on "01 December 2026"
    And Admin successfully disburse the loan on "01 December 2026" with "1000" EUR transaction amount
    # --- Sell the second loan to an external asset owner; the sale settles via COB ---
    When Admin makes asset externalization request by Loan ID with unique ownerExternalId, user-generated transferExternalId and the following data:
      | Transaction type | settlementDate | purchasePriceRatio |
      | sale             | 2026-12-10     | 1                  |
    Then Asset externalization response has the correct Loan ID, transferExternalId
    When Admin sets the business date to "11 December 2026"
    And Admin runs COB job
    # --- Accrue December income on both loans ---
    When Admin sets the business date to "31 December 2026"
    And Admin runs COB job
    Then Trial Balance Summary Report with Asset Owner for date "31 December 2026" has a row for GL account "404000" with non-zero ending balance
    # --- Close FY2026 ---
    When Admin sets the business date to "02 January 2027"
    And Admin runs the Retained Earning Job
    # --- Income zeroed across BOTH products and BOTH owners; one Retained Earning record per asset owner ---
    Then Trial Balance Summary Report with Asset Owner for date "01 January 2027" shows GL account "404000" closed out
    And Trial Balance Summary Report with Asset Owner for date "01 January 2027" has a row for GL account "320000" with non-zero ending balance
    #And The journal entry annual summary table contains 2 rows for GL code "320000" with year end date "31 December 2026"

  @TestRailId:C85189
  Scenario: Verify Year-end close keeps GL codes in the trial balance summary report
    # A written-off loan posts to "Written off" account whose gl_code is "e4".
    When Admin sets the business date to "01 December 2027"
    And Admin creates a client with random data
    And Admin creates a fully customized loan with the following data:
      | LoanProduct                                                   | submitted on date | with Principal | ANNUAL interest rate % | interest type     | interest calculation period | amortization type  | loanTermFrequency | loanTermFrequencyType | repaymentEvery | repaymentFrequencyType | numberOfRepayments | graceOnPrincipalPayment | graceOnInterestPayment | interest free period | Payment strategy            |
      | LP2_ADV_CUSTOM_PMT_ALLOC_PROGRESSIVE_LOAN_SCHEDULE_HORIZONTAL | 01 December 2027  | 100            | 26                     | DECLINING_BALANCE | DAILY                       | EQUAL_INSTALLMENTS | 3                 | MONTHS                | 1              | MONTHS                 | 3                  | 0                       | 0                      | 0                    | ADVANCED_PAYMENT_ALLOCATION |
    And Admin successfully approves the loan on "01 December 2027" with "100" amount and expected disbursement date on "01 December 2027"
    And Admin successfully disburse the loan on "01 December 2027" with "100" EUR transaction amount
    When Admin sets the business date to "15 December 2027"
    And Admin runs inline COB job for Loan
    And Admin does write-off the loan on "15 December 2027"
    # --- Both a numeric income balance AND the alphanumeric expense row are in the trial balance ---
    Then Trial Balance Summary Report with Asset Owner for date "31 December 2027" has a row for GL account "404000" with non-zero ending balance
    And Trial Balance Summary Report with Asset Owner for date "31 December 2027" has a row for GL account "e4" with non-zero ending balance
    # --- Close FY2027: the job must not crash on "e4" and must still close the numeric band ---
    When Admin sets the business date to "02 January 2028"
    And Admin runs the Retained Earning Job
    Then Trial Balance Summary Report with Asset Owner for date "01 January 2028" shows GL account "404000" closed out
    And Trial Balance Summary Report with Asset Owner for date "01 January 2028" has a row for GL account "320000" with non-zero ending balance
