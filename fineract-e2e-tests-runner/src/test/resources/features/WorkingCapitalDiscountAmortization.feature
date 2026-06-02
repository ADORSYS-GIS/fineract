@WorkingCapital
@WorkingCapitalDiscountFeeAmortizationFeature
Feature: WorkingCapitalDiscountFeeAmortization

  @TestRailId:C80968
  Scenario: Verify Discount Fee Amortization transaction on Working Capital Loan account triggers on COB by repayment - UC1
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0     | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 5, 2026 --- #
    When Admin sets the business date to "05 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "05 January 2026" with 150 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment    | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "08 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 150.0              | 100000.0           | 28.7           | 971.3            | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 28.7              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "05 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 28.7   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 28.7  |        |

  @TestRailId:C80969
  Scenario: Verify NO Discount Fee Amortization transaction on Working Capital Loan account triggers on COB without repayment - UC2
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "08 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 0.0                | 100000.0           | 0.0            | 1000.0           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |

  @TestRailId:C80970
  Scenario: Verify NO Discount Fee Amortization transaction on Working Capital Loan account triggers on COB if NO discount added - UC3
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 5, 2026 --- #
    When Admin sets the business date to "05 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "05 January 2026" with 150 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment    | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "08 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 9000.0    | 150.0              | 100000.0           | 0.0            | 0.0              | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment    | 150.0             | 150.0            | 0.0               | 0.0                   | false    |

  @TestRailId:C80971
  Scenario: Verify NO duplicated Discount Fee Amortization transaction on Working Capital Loan account triggers on COB run again without new repayments - UC4
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 5, 2026 --- #
    When Admin sets the business date to "05 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "05 January 2026" with 150 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment    | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "08 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 150.0              | 100000.0           | 28.7           | 971.3            | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 28.7              |                  |                   |                       | false    |
# --- 2nd run WC COB shouldn't generate any more/new 'Discount Fee Amortization' transaction
    When Admin sets the business date to "10 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 150.0              | 100000.0           | 28.7           | 971.3            | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 28.7              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "05 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 28.7   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 28.7  |        |

  @TestRailId:C80972
  Scenario: Verify Discount Fee Amortization transaction on Working Capital Loan account triggers on COB run by each repayment  - UC5
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- update discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 5, 2026 --- #
    When Admin sets the business date to "05 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "05 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "08 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 50.0               | 100000.0           | 9.61           | 990.39           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 9.61              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "05 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 9.61   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 9.61  |        |
# -- make repayment on Jan, 10, 2026 --- #
    When Admin sets the business date to "10 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "10 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 9.61              |                  |                   |                       | false    |
      | 10 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "11 January 2026"
    When Admin runs inline COB job for Working Capital Loan
# --- realized income should be equal to discount amount --- #
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 100.0              | 100000.0           | 19.18          | 980.82           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 05 January 2026 | Discount Fee Amortization | 9.61              |                  |                   |                       | false    |
      | 10 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 10 January 2026 | Discount Fee Amortization | 9.57              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "10 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 9.57   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 9.57  |        |

  @TestRailId:C80973
  Scenario: Verify Discount Fee Amortization transaction on Working Capital Loan account triggers on COB run by repayment at next day transaction date - UC6
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 1, 2026 --- #
    And Customer makes repayment on "01 January 2026" with 150 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Repayment    | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "03 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 150.0              | 100000.0           | 28.7           | 971.3            | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Repayment                 | 150.0             | 150.0            | 0.0               | 0.0                   | false    |
      | 02 January 2026 | Discount Fee Amortization | 28.7              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "02 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 28.7   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 28.7  |        |

  @TestRailId:C8974
  Scenario: Verify Discount Fee Amortization transaction on Working Capital Loan account triggers on COB run by repayment with amount less then discount amount - UC7
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 3, 2026 --- #
    When Admin sets the business date to "03 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "03 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "04 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 50.0               | 100000.0           | 9.61           | 990.39           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Discount Fee Amortization | 9.61              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "03 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 9.61   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 9.61  |        |

  @TestRailId:C80975
  Scenario: Verify Discount Fee Amortization transaction on Working Capital Loan account triggers on COB run by a few repayments at the same date - UC8
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Working capital loan creation was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status                         | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Submitted and pending approval | 9000.0            | 0.0               | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status   | proposedPrincipal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Approved | 9000.0            | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0           | 18.0              | null             | null             | null     |
# --- add discount after disbursement on the same disbursement date --- #
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Working capital loan account has the correct data:
      | product.name             | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPaymentVolume | periodPaymentRate | discountProposed | discountApproved | discount |
      | WCLP_ADVANCED_ACCOUNTING | 2026-01-01      | 2026-01-01               | Active | 10000.0   | 9000.0            | 100000.0           | 18.0              | null             | null             | 1000.0   |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
# -- make repayment on Jan, 3, 2026 --- #
    When Admin sets the business date to "03 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Customer makes repayment on "03 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
# -- make one more repayment on Jan, 3, 2026 --- #
    And Customer makes repayment on "03 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
    When Admin sets the business date to "04 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 100.0              | 100000.0           | 19.18          | 980.82           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 03 January 2026 | Discount Fee Amortization | 19.18             |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "03 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 19.18  |
      | LIABILITY | 240005       | Deferred Interest Revenue | 19.18 |        |

  Scenario: Verify Discount Fee Amortization when repayment is made on the disbursement date
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                |          |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Admin successfully add discount with "1000" amount on Working Capital loan account
    And Customer makes repayment on "01 January 2026" with 50 transaction amount on Working Capital loan
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Repayment    | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
    And Admin retrieves the projected amortization schedule
    Then The retrieved amortization schedule has the following summary fields:
      | discountFeeAmount | netDisbursementAmount | totalPaymentVolume | periodPaymentRate | npvDayCount | expectedPaymentAmount | originalPaymentNumber |
      | 1000.00           | 9000.00               | 100000.00          | 18                | 360         | 50.00                 | 200                   |
    And The retrieved amortization schedule has payments with the following details:
      | paymentNo | date       | expectedPaymentAmount | expectedBalance | actualBalance | expectedAmortizationAmount | actualPaymentAmount | actualAmortizationAmount | expectedDiscountFeeBalance | actualDiscountFeeBalance |
      | 0         | 2026-01-01 | -9000.00              | 9000.00         | 9000.00       |                            |                     |                          | 1000.00                    | 1000.00                  |
      | 1         | 2026-01-01 | 50.00                 | 8959.61         | 8959.61       | 9.61                       | 50.00               | 9.61                     | 990.39                     | 990.39                   |
      | 2         | 2026-01-02 | 50.00                 | 8919.18         |               | 9.57                       |                     |                          | 980.82                     |                          |
      | 3         | 2026-01-03 | 50.00                 | 8878.70         |               | 9.52                       |                     |                          | 971.30                     |                          |
      | 4         | 2026-01-04 | 50.00                 | 8838.18         |               | 9.48                       |                     |                          | 961.82                     |                          |
      | 5         | 2026-01-05 | 50.00                 | 8797.62         |               | 9.44                       |                     |                          | 952.38                     |                          |
      | 6         | 2026-01-06 | 50.00                 | 8757.01         |               | 9.39                       |                     |                          | 942.99                     |                          |
      | 7         | 2026-01-07 | 50.00                 | 8716.36         |               | 9.35                       |                     |                          | 933.64                     |                          |
      | 8         | 2026-01-08 | 50.00                 | 8675.67         |               | 9.31                       |                     |                          | 924.33                     |                          |
      | 9         | 2026-01-09 | 50.00                 | 8634.94         |               | 9.26                       |                     |                          | 915.07                     |                          |
      | 10        | 2026-01-10 | 50.00                 | 8594.16         |               | 9.22                       |                     |                          | 905.85                     |                          |
      | 11        | 2026-01-11 | 50.00                 | 8553.33         |               | 9.18                       |                     |                          | 896.67                     |                          |
      | 12        | 2026-01-12 | 50.00                 | 8512.47         |               | 9.13                       |                     |                          | 887.54                     |                          |
      | 13        | 2026-01-13 | 50.00                 | 8471.56         |               | 9.09                       |                     |                          | 878.45                     |                          |
      | 14        | 2026-01-14 | 50.00                 | 8430.60         |               | 9.05                       |                     |                          | 869.40                     |                          |
      | 15        | 2026-01-15 | 50.00                 | 8389.61         |               | 9.00                       |                     |                          | 860.40                     |                          |
      | 16        | 2026-01-16 | 50.00                 | 8348.56         |               | 8.96                       |                     |                          | 851.44                     |                          |
      | 17        | 2026-01-17 | 50.00                 | 8307.48         |               | 8.91                       |                     |                          | 842.53                     |                          |
      | 18        | 2026-01-18 | 50.00                 | 8266.35         |               | 8.87                       |                     |                          | 833.66                     |                          |
      | 19        | 2026-01-19 | 50.00                 | 8225.18         |               | 8.83                       |                     |                          | 824.83                     |                          |
      | 20        | 2026-01-20 | 50.00                 | 8183.96         |               | 8.78                       |                     |                          | 816.05                     |                          |
      | 21        | 2026-01-21 | 50.00                 | 8142.70         |               | 8.74                       |                     |                          | 807.31                     |                          |
      | 22        | 2026-01-22 | 50.00                 | 8101.39         |               | 8.69                       |                     |                          | 798.62                     |                          |
      | 23        | 2026-01-23 | 50.00                 | 8060.04         |               | 8.65                       |                     |                          | 789.97                     |                          |
      | 24        | 2026-01-24 | 50.00                 | 8018.65         |               | 8.61                       |                     |                          | 781.36                     |                          |
      | 25        | 2026-01-25 | 50.00                 | 7977.21         |               | 8.56                       |                     |                          | 772.80                     |                          |
      | 26        | 2026-01-26 | 50.00                 | 7935.73         |               | 8.52                       |                     |                          | 764.28                     |                          |
      | 27        | 2026-01-27 | 50.00                 | 7894.21         |               | 8.47                       |                     |                          | 755.81                     |                          |
      | 28        | 2026-01-28 | 50.00                 | 7852.63         |               | 8.43                       |                     |                          | 747.38                     |                          |
      | 29        | 2026-01-29 | 50.00                 | 7811.02         |               | 8.39                       |                     |                          | 738.99                     |                          |
      | 30        | 2026-01-30 | 50.00                 | 7769.36         |               | 8.34                       |                     |                          | 730.65                     |                          |
      | 31        | 2026-01-31 | 50.00                 | 7727.66         |               | 8.30                       |                     |                          | 722.35                     |                          |
      | 32        | 2026-02-01 | 50.00                 | 7685.91         |               | 8.25                       |                     |                          | 714.10                     |                          |
      | 33        | 2026-02-02 | 50.00                 | 7644.12         |               | 8.21                       |                     |                          | 705.89                     |                          |
      | 34        | 2026-02-03 | 50.00                 | 7602.28         |               | 8.16                       |                     |                          | 697.73                     |                          |
      | 35        | 2026-02-04 | 50.00                 | 7560.40         |               | 8.12                       |                     |                          | 689.61                     |                          |
      | 36        | 2026-02-05 | 50.00                 | 7518.47         |               | 8.07                       |                     |                          | 681.54                     |                          |
      | 37        | 2026-02-06 | 50.00                 | 7476.50         |               | 8.03                       |                     |                          | 673.51                     |                          |
      | 38        | 2026-02-07 | 50.00                 | 7434.48         |               | 7.98                       |                     |                          | 665.53                     |                          |
      | 39        | 2026-02-08 | 50.00                 | 7392.42         |               | 7.94                       |                     |                          | 657.59                     |                          |
      | 40        | 2026-02-09 | 50.00                 | 7350.31         |               | 7.89                       |                     |                          | 649.70                     |                          |
      | 41        | 2026-02-10 | 50.00                 | 7308.16         |               | 7.85                       |                     |                          | 641.85                     |                          |
      | 42        | 2026-02-11 | 50.00                 | 7265.97         |               | 7.80                       |                     |                          | 634.05                     |                          |
      | 43        | 2026-02-12 | 50.00                 | 7223.72         |               | 7.76                       |                     |                          | 626.29                     |                          |
      | 44        | 2026-02-13 | 50.00                 | 7181.44         |               | 7.71                       |                     |                          | 618.58                     |                          |
      | 45        | 2026-02-14 | 50.00                 | 7139.11         |               | 7.67                       |                     |                          | 610.91                     |                          |
      | 46        | 2026-02-15 | 50.00                 | 7096.73         |               | 7.62                       |                     |                          | 603.29                     |                          |
      | 47        | 2026-02-16 | 50.00                 | 7054.31         |               | 7.58                       |                     |                          | 595.71                     |                          |
      | 48        | 2026-02-17 | 50.00                 | 7011.84         |               | 7.53                       |                     |                          | 588.18                     |                          |
      | 49        | 2026-02-18 | 50.00                 | 6969.33         |               | 7.49                       |                     |                          | 580.69                     |                          |
      | 50        | 2026-02-19 | 50.00                 | 6926.77         |               | 7.44                       |                     |                          | 573.25                     |                          |
      | 51        | 2026-02-20 | 50.00                 | 6884.17         |               | 7.40                       |                     |                          | 565.85                     |                          |
      | 52        | 2026-02-21 | 50.00                 | 6841.52         |               | 7.35                       |                     |                          | 558.50                     |                          |
      | 53        | 2026-02-22 | 50.00                 | 6798.82         |               | 7.31                       |                     |                          | 551.19                     |                          |
      | 54        | 2026-02-23 | 50.00                 | 6756.08         |               | 7.26                       |                     |                          | 543.93                     |                          |
      | 55        | 2026-02-24 | 50.00                 | 6713.30         |               | 7.21                       |                     |                          | 536.72                     |                          |
      | 56        | 2026-02-25 | 50.00                 | 6670.47         |               | 7.17                       |                     |                          | 529.55                     |                          |
      | 57        | 2026-02-26 | 50.00                 | 6627.59         |               | 7.12                       |                     |                          | 522.43                     |                          |
      | 58        | 2026-02-27 | 50.00                 | 6584.67         |               | 7.08                       |                     |                          | 515.35                     |                          |
      | 59        | 2026-02-28 | 50.00                 | 6541.70         |               | 7.03                       |                     |                          | 508.32                     |                          |
      | 60        | 2026-03-01 | 50.00                 | 6498.68         |               | 6.99                       |                     |                          | 501.33                     |                          |
      | 61        | 2026-03-02 | 50.00                 | 6455.62         |               | 6.94                       |                     |                          | 494.39                     |                          |
      | 62        | 2026-03-03 | 50.00                 | 6412.51         |               | 6.89                       |                     |                          | 487.50                     |                          |
      | 63        | 2026-03-04 | 50.00                 | 6369.36         |               | 6.85                       |                     |                          | 480.65                     |                          |
      | 64        | 2026-03-05 | 50.00                 | 6326.16         |               | 6.80                       |                     |                          | 473.85                     |                          |
      | 65        | 2026-03-06 | 50.00                 | 6282.92         |               | 6.76                       |                     |                          | 467.09                     |                          |
      | 66        | 2026-03-07 | 50.00                 | 6239.63         |               | 6.71                       |                     |                          | 460.38                     |                          |
      | 67        | 2026-03-08 | 50.00                 | 6196.29         |               | 6.66                       |                     |                          | 453.72                     |                          |
      | 68        | 2026-03-09 | 50.00                 | 6152.91         |               | 6.62                       |                     |                          | 447.10                     |                          |
      | 69        | 2026-03-10 | 50.00                 | 6109.48         |               | 6.57                       |                     |                          | 440.53                     |                          |
      | 70        | 2026-03-11 | 50.00                 | 6066.00         |               | 6.52                       |                     |                          | 434.01                     |                          |
      | 71        | 2026-03-12 | 50.00                 | 6022.48         |               | 6.48                       |                     |                          | 427.53                     |                          |
      | 72        | 2026-03-13 | 50.00                 | 5978.91         |               | 6.43                       |                     |                          | 421.10                     |                          |
      | 73        | 2026-03-14 | 50.00                 | 5935.29         |               | 6.38                       |                     |                          | 414.72                     |                          |
      | 74        | 2026-03-15 | 50.00                 | 5891.63         |               | 6.34                       |                     |                          | 408.38                     |                          |
      | 75        | 2026-03-16 | 50.00                 | 5847.92         |               | 6.29                       |                     |                          | 402.09                     |                          |
      | 76        | 2026-03-17 | 50.00                 | 5804.17         |               | 6.24                       |                     |                          | 395.85                     |                          |
      | 77        | 2026-03-18 | 50.00                 | 5760.36         |               | 6.20                       |                     |                          | 389.65                     |                          |
      | 78        | 2026-03-19 | 50.00                 | 5716.52         |               | 6.15                       |                     |                          | 383.50                     |                          |
      | 79        | 2026-03-20 | 50.00                 | 5672.62         |               | 6.10                       |                     |                          | 377.40                     |                          |
      | 80        | 2026-03-21 | 50.00                 | 5628.68         |               | 6.06                       |                     |                          | 371.34                     |                          |
      | 81        | 2026-03-22 | 50.00                 | 5584.69         |               | 6.01                       |                     |                          | 365.33                     |                          |
      | 82        | 2026-03-23 | 50.00                 | 5540.65         |               | 5.96                       |                     |                          | 359.37                     |                          |
      | 83        | 2026-03-24 | 50.00                 | 5496.57         |               | 5.92                       |                     |                          | 353.45                     |                          |
      | 84        | 2026-03-25 | 50.00                 | 5452.44         |               | 5.87                       |                     |                          | 347.58                     |                          |
      | 85        | 2026-03-26 | 50.00                 | 5408.26         |               | 5.82                       |                     |                          | 341.76                     |                          |
      | 86        | 2026-03-27 | 50.00                 | 5364.03         |               | 5.78                       |                     |                          | 335.98                     |                          |
      | 87        | 2026-03-28 | 50.00                 | 5319.76         |               | 5.73                       |                     |                          | 330.25                     |                          |
      | 88        | 2026-03-29 | 50.00                 | 5275.44         |               | 5.68                       |                     |                          | 324.57                     |                          |
      | 89        | 2026-03-30 | 50.00                 | 5231.08         |               | 5.63                       |                     |                          | 318.94                     |                          |
      | 90        | 2026-03-31 | 50.00                 | 5186.66         |               | 5.59                       |                     |                          | 313.35                     |                          |
      | 91        | 2026-04-01 | 50.00                 | 5142.20         |               | 5.54                       |                     |                          | 307.81                     |                          |
      | 92        | 2026-04-02 | 50.00                 | 5097.69         |               | 5.49                       |                     |                          | 302.32                     |                          |
      | 93        | 2026-04-03 | 50.00                 | 5053.13         |               | 5.44                       |                     |                          | 296.88                     |                          |
      | 94        | 2026-04-04 | 50.00                 | 5008.53         |               | 5.40                       |                     |                          | 291.48                     |                          |
      | 95        | 2026-04-05 | 50.00                 | 4963.88         |               | 5.35                       |                     |                          | 286.13                     |                          |
      | 96        | 2026-04-06 | 50.00                 | 4919.18         |               | 5.30                       |                     |                          | 280.83                     |                          |
      | 97        | 2026-04-07 | 50.00                 | 4874.43         |               | 5.25                       |                     |                          | 275.58                     |                          |
      | 98        | 2026-04-08 | 50.00                 | 4829.64         |               | 5.20                       |                     |                          | 270.38                     |                          |
      | 99        | 2026-04-09 | 50.00                 | 4784.79         |               | 5.16                       |                     |                          | 265.22                     |                          |
      | 100       | 2026-04-10 | 50.00                 | 4739.90         |               | 5.11                       |                     |                          | 260.11                     |                          |
      | 101       | 2026-04-11 | 50.00                 | 4694.96         |               | 5.06                       |                     |                          | 255.05                     |                          |
      | 102       | 2026-04-12 | 50.00                 | 4649.98         |               | 5.01                       |                     |                          | 250.04                     |                          |
      | 103       | 2026-04-13 | 50.00                 | 4604.94         |               | 4.97                       |                     |                          | 245.07                     |                          |
      | 104       | 2026-04-14 | 50.00                 | 4559.86         |               | 4.92                       |                     |                          | 240.15                     |                          |
      | 105       | 2026-04-15 | 50.00                 | 4514.73         |               | 4.87                       |                     |                          | 235.28                     |                          |
      | 106       | 2026-04-16 | 50.00                 | 4469.55         |               | 4.82                       |                     |                          | 230.46                     |                          |
      | 107       | 2026-04-17 | 50.00                 | 4424.32         |               | 4.77                       |                     |                          | 225.69                     |                          |
      | 108       | 2026-04-18 | 50.00                 | 4379.05         |               | 4.72                       |                     |                          | 220.97                     |                          |
      | 109       | 2026-04-19 | 50.00                 | 4333.72         |               | 4.68                       |                     |                          | 216.29                     |                          |
      | 110       | 2026-04-20 | 50.00                 | 4288.35         |               | 4.63                       |                     |                          | 211.66                     |                          |
      | 111       | 2026-04-21 | 50.00                 | 4242.93         |               | 4.58                       |                     |                          | 207.08                     |                          |
      | 112       | 2026-04-22 | 50.00                 | 4197.46         |               | 4.53                       |                     |                          | 202.55                     |                          |
      | 113       | 2026-04-23 | 50.00                 | 4151.94         |               | 4.48                       |                     |                          | 198.07                     |                          |
      | 114       | 2026-04-24 | 50.00                 | 4106.38         |               | 4.43                       |                     |                          | 193.64                     |                          |
      | 115       | 2026-04-25 | 50.00                 | 4060.76         |               | 4.38                       |                     |                          | 189.26                     |                          |
      | 116       | 2026-04-26 | 50.00                 | 4015.10         |               | 4.34                       |                     |                          | 184.92                     |                          |
      | 117       | 2026-04-27 | 50.00                 | 3969.38         |               | 4.29                       |                     |                          | 180.63                     |                          |
      | 118       | 2026-04-28 | 50.00                 | 3923.62         |               | 4.24                       |                     |                          | 176.39                     |                          |
      | 119       | 2026-04-29 | 50.00                 | 3877.81         |               | 4.19                       |                     |                          | 172.20                     |                          |
      | 120       | 2026-04-30 | 50.00                 | 3831.95         |               | 4.14                       |                     |                          | 168.06                     |                          |
      | 121       | 2026-05-01 | 50.00                 | 3786.04         |               | 4.09                       |                     |                          | 163.97                     |                          |
      | 122       | 2026-05-02 | 50.00                 | 3740.09         |               | 4.04                       |                     |                          | 159.93                     |                          |
      | 123       | 2026-05-03 | 50.00                 | 3694.08         |               | 3.99                       |                     |                          | 155.94                     |                          |
      | 124       | 2026-05-04 | 50.00                 | 3648.03         |               | 3.94                       |                     |                          | 152.00                     |                          |
      | 125       | 2026-05-05 | 50.00                 | 3601.92         |               | 3.90                       |                     |                          | 148.10                     |                          |
      | 126       | 2026-05-06 | 50.00                 | 3555.77         |               | 3.85                       |                     |                          | 144.25                     |                          |
      | 127       | 2026-05-07 | 50.00                 | 3509.56         |               | 3.80                       |                     |                          | 140.45                     |                          |
      | 128       | 2026-05-08 | 50.00                 | 3463.31         |               | 3.75                       |                     |                          | 136.70                     |                          |
      | 129       | 2026-05-09 | 50.00                 | 3417.01         |               | 3.70                       |                     |                          | 133.00                     |                          |
      | 130       | 2026-05-10 | 50.00                 | 3370.66         |               | 3.65                       |                     |                          | 129.35                     |                          |
      | 131       | 2026-05-11 | 50.00                 | 3324.26         |               | 3.60                       |                     |                          | 125.75                     |                          |
      | 132       | 2026-05-12 | 50.00                 | 3277.81         |               | 3.55                       |                     |                          | 122.20                     |                          |
      | 133       | 2026-05-13 | 50.00                 | 3231.31         |               | 3.50                       |                     |                          | 118.70                     |                          |
      | 134       | 2026-05-14 | 50.00                 | 3184.76         |               | 3.45                       |                     |                          | 115.25                     |                          |
      | 135       | 2026-05-15 | 50.00                 | 3138.16         |               | 3.40                       |                     |                          | 111.85                     |                          |
      | 136       | 2026-05-16 | 50.00                 | 3091.51         |               | 3.35                       |                     |                          | 108.50                     |                          |
      | 137       | 2026-05-17 | 50.00                 | 3044.81         |               | 3.30                       |                     |                          | 105.20                     |                          |
      | 138       | 2026-05-18 | 50.00                 | 2998.06         |               | 3.25                       |                     |                          | 101.95                     |                          |
      | 139       | 2026-05-19 | 50.00                 | 2951.26         |               | 3.20                       |                     |                          | 98.75                      |                          |
      | 140       | 2026-05-20 | 50.00                 | 2904.42         |               | 3.15                       |                     |                          | 95.60                      |                          |
      | 141       | 2026-05-21 | 50.00                 | 2857.52         |               | 3.10                       |                     |                          | 92.50                      |                          |
      | 142       | 2026-05-22 | 50.00                 | 2810.57         |               | 3.05                       |                     |                          | 89.45                      |                          |
      | 143       | 2026-05-23 | 50.00                 | 2763.57         |               | 3.00                       |                     |                          | 86.45                      |                          |
      | 144       | 2026-05-24 | 50.00                 | 2716.52         |               | 2.95                       |                     |                          | 83.50                      |                          |
      | 145       | 2026-05-25 | 50.00                 | 2669.42         |               | 2.90                       |                     |                          | 80.60                      |                          |
      | 146       | 2026-05-26 | 50.00                 | 2622.27         |               | 2.85                       |                     |                          | 77.75                      |                          |
      | 147       | 2026-05-27 | 50.00                 | 2575.07         |               | 2.80                       |                     |                          | 74.95                      |                          |
      | 148       | 2026-05-28 | 50.00                 | 2527.82         |               | 2.75                       |                     |                          | 72.20                      |                          |
      | 149       | 2026-05-29 | 50.00                 | 2480.52         |               | 2.70                       |                     |                          | 69.50                      |                          |
      | 150       | 2026-05-30 | 50.00                 | 2433.17         |               | 2.65                       |                     |                          | 66.85                      |                          |
      | 151       | 2026-05-31 | 50.00                 | 2385.77         |               | 2.60                       |                     |                          | 64.25                      |                          |
      | 152       | 2026-06-01 | 50.00                 | 2338.31         |               | 2.55                       |                     |                          | 61.70                      |                          |
      | 153       | 2026-06-02 | 50.00                 | 2290.81         |               | 2.50                       |                     |                          | 59.20                      |                          |
      | 154       | 2026-06-03 | 50.00                 | 2243.26         |               | 2.45                       |                     |                          | 56.75                      |                          |
      | 155       | 2026-06-04 | 50.00                 | 2195.65         |               | 2.40                       |                     |                          | 54.35                      |                          |
      | 156       | 2026-06-05 | 50.00                 | 2148.00         |               | 2.34                       |                     |                          | 52.01                      |                          |
      | 157       | 2026-06-06 | 50.00                 | 2100.29         |               | 2.29                       |                     |                          | 49.72                      |                          |
      | 158       | 2026-06-07 | 50.00                 | 2052.53         |               | 2.24                       |                     |                          | 47.48                      |                          |
      | 159       | 2026-06-08 | 50.00                 | 2004.73         |               | 2.19                       |                     |                          | 45.29                      |                          |
      | 160       | 2026-06-09 | 50.00                 | 1956.87         |               | 2.14                       |                     |                          | 43.15                      |                          |
      | 161       | 2026-06-10 | 50.00                 | 1908.96         |               | 2.09                       |                     |                          | 41.06                      |                          |
      | 162       | 2026-06-11 | 50.00                 | 1860.99         |               | 2.04                       |                     |                          | 39.02                      |                          |
      | 163       | 2026-06-12 | 50.00                 | 1812.98         |               | 1.99                       |                     |                          | 37.03                      |                          |
      | 164       | 2026-06-13 | 50.00                 | 1764.92         |               | 1.94                       |                     |                          | 35.09                      |                          |
      | 165       | 2026-06-14 | 50.00                 | 1716.80         |               | 1.88                       |                     |                          | 33.21                      |                          |
      | 166       | 2026-06-15 | 50.00                 | 1668.64         |               | 1.83                       |                     |                          | 31.38                      |                          |
      | 167       | 2026-06-16 | 50.00                 | 1620.42         |               | 1.78                       |                     |                          | 29.60                      |                          |
      | 168       | 2026-06-17 | 50.00                 | 1572.15         |               | 1.73                       |                     |                          | 27.87                      |                          |
      | 169       | 2026-06-18 | 50.00                 | 1523.83         |               | 1.68                       |                     |                          | 26.19                      |                          |
      | 170       | 2026-06-19 | 50.00                 | 1475.45         |               | 1.63                       |                     |                          | 24.56                      |                          |
      | 171       | 2026-06-20 | 50.00                 | 1427.03         |               | 1.58                       |                     |                          | 22.98                      |                          |
      | 172       | 2026-06-21 | 50.00                 | 1378.55         |               | 1.52                       |                     |                          | 21.46                      |                          |
      | 173       | 2026-06-22 | 50.00                 | 1330.02         |               | 1.47                       |                     |                          | 19.99                      |                          |
      | 174       | 2026-06-23 | 50.00                 | 1281.45         |               | 1.42                       |                     |                          | 18.57                      |                          |
      | 175       | 2026-06-24 | 50.00                 | 1232.81         |               | 1.37                       |                     |                          | 17.20                      |                          |
      | 176       | 2026-06-25 | 50.00                 | 1184.13         |               | 1.32                       |                     |                          | 15.88                      |                          |
      | 177       | 2026-06-26 | 50.00                 | 1135.39         |               | 1.26                       |                     |                          | 14.62                      |                          |
      | 178       | 2026-06-27 | 50.00                 | 1086.61         |               | 1.21                       |                     |                          | 13.41                      |                          |
      | 179       | 2026-06-28 | 50.00                 | 1037.77         |               | 1.16                       |                     |                          | 12.25                      |                          |
      | 180       | 2026-06-29 | 50.00                 | 988.88          |               | 1.11                       |                     |                          | 11.14                      |                          |
      | 181       | 2026-06-30 | 50.00                 | 939.93          |               | 1.06                       |                     |                          | 10.08                      |                          |
      | 182       | 2026-07-01 | 50.00                 | 890.93          |               | 1.00                       |                     |                          | 9.08                       |                          |
      | 183       | 2026-07-02 | 50.00                 | 841.89          |               | 0.95                       |                     |                          | 8.13                       |                          |
      | 184       | 2026-07-03 | 50.00                 | 792.79          |               | 0.90                       |                     |                          | 7.23                       |                          |
      | 185       | 2026-07-04 | 50.00                 | 743.63          |               | 0.85                       |                     |                          | 6.38                       |                          |
      | 186       | 2026-07-05 | 50.00                 | 694.43          |               | 0.79                       |                     |                          | 5.59                       |                          |
      | 187       | 2026-07-06 | 50.00                 | 645.17          |               | 0.74                       |                     |                          | 4.85                       |                          |
      | 188       | 2026-07-07 | 50.00                 | 595.86          |               | 0.69                       |                     |                          | 4.16                       |                          |
      | 189       | 2026-07-08 | 50.00                 | 546.49          |               | 0.64                       |                     |                          | 3.52                       |                          |
      | 190       | 2026-07-09 | 50.00                 | 497.08          |               | 0.58                       |                     |                          | 2.94                       |                          |
      | 191       | 2026-07-10 | 50.00                 | 447.61          |               | 0.53                       |                     |                          | 2.41                       |                          |
      | 192       | 2026-07-11 | 50.00                 | 398.08          |               | 0.48                       |                     |                          | 1.93                       |                          |
      | 193       | 2026-07-12 | 50.00                 | 348.51          |               | 0.43                       |                     |                          | 1.50                       |                          |
      | 194       | 2026-07-13 | 50.00                 | 298.88          |               | 0.37                       |                     |                          | 1.13                       |                          |
      | 195       | 2026-07-14 | 50.00                 | 249.20          |               | 0.32                       |                     |                          | 0.81                       |                          |
      | 196       | 2026-07-15 | 50.00                 | 199.47          |               | 0.27                       |                     |                          | 0.54                       |                          |
      | 197       | 2026-07-16 | 50.00                 | 149.68          |               | 0.21                       |                     |                          | 0.33                       |                          |
      | 198       | 2026-07-17 | 50.00                 | 99.84           |               | 0.16                       |                     |                          | 0.17                       |                          |
      | 199       | 2026-07-18 | 50.00                 | 49.95           |               | 0.11                       |                     |                          | 0.06                       |                          |
      | 200       | 2026-07-19 | 50.00                 | 0.00            |               | 0.05                       |                     |                          | 0.01                       |                          |
    When Admin sets the business date to "03 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Working capital loan account has the correct data:
      | principal | totalPaidPrincipal | totalPaymentVolume | realizedIncome | unrealizedIncome | overpaymentAmount |
      | 10000.0   | 50.0               | 100000.0           | 9.61           | 990.39           | 0.0               |
    And Working Capital Loan has transactions:
      | transactionDate | type                      | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement              | 9000.0            | 9000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Discount Fee              | 1000.0            | 1000.0           | 0.0               | 0.0                   | false    |
      | 01 January 2026 | Repayment                 | 50.0              | 50.0             | 0.0               | 0.0                   | false    |
      | 02 January 2026 | Discount Fee Amortization | 9.61              |                  |                   |                       | false    |
    Then Working Capital Loan Transactions tab has a "DISCOUNT_FEE_AMORTIZATION" transaction with date "02 January 2026" which has the following Journal entries:
      | Type      | Account code | Account name              | Debit | Credit |
      | INCOME    | 404000       | Interest Income           |       | 9.61   |
      | LIABILITY | 240005       | Deferred Interest Revenue | 9.61  |        |

  Scenario: Discount amortization schedule with repayment on disbursement date and a later rate change starts the new rate on the rate change date
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 900             | 100000             | 18                |          |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "900" amount and expected disbursement date on "01 January 2026"
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "900" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Admin successfully add discount with "100" amount on Working Capital loan account
    And Customer makes repayment on "01 January 2026" with 50 transaction amount on Working Capital loan
    When Admin sets the business date to "06 January 2026"
    When Admin runs inline COB job for Working Capital Loan
    And Admin update Working Capital period payment rate with "17" value
    And Admin retrieves the projected amortization schedule
    Then The retrieved amortization schedule has the following summary fields:
      | discountFeeAmount | netDisbursementAmount | totalPaymentVolume | periodPaymentRate | npvDayCount | expectedPaymentAmount | originalPaymentNumber |
      | 100.00            | 900.00                | 100000.00          | 18                | 360         | 50.00                 | 20                    |
    And The retrieved amortization schedule has payments with the following details:
      | paymentNo | date       | expectedPaymentAmount | expectedBalance | actualBalance | expectedAmortizationAmount | actualPaymentAmount | actualAmortizationAmount | expectedDiscountFeeBalance | actualDiscountFeeBalance |
      | 0         | 2026-01-01 | -900.00               | 900.00          | 900.00        |                            |                     |                          | 100.00                     | 100.00                   |
      | 1         | 2026-01-01 | 50.00                 | 859.23          | 859.23        | 9.23                       | 50.00               | 9.23                     | 90.77                      | 90.77                    |
      | 2         | 2026-01-02 | 50.00                 | 818.03          | 859.23        | 8.81                       | 0.00                | 0.00                     | 81.96                      | 90.77                    |
      | 3         | 2026-01-03 | 50.00                 | 776.42          | 859.23        | 8.39                       | 0.00                | 0.00                     | 73.57                      | 90.77                    |
      | 4         | 2026-01-04 | 50.00                 | 734.38          | 859.23        | 7.96                       | 0.00                | 0.00                     | 65.61                      | 90.77                    |
      | 5         | 2026-01-05 | 50.00                 | 691.91          | 859.23        | 7.53                       | 0.00                | 0.00                     | 58.08                      | 90.77                    |
      | 6         | 2026-01-06 | 47.22                 | 640.52          |               | -4.17                      |                     |                          | 62.25                      |                          |
      | 7         | 2026-01-07 | 47.22                 | 589.45          |               | -3.86                      |                     |                          | 66.11                      |                          |
      | 8         | 2026-01-08 | 47.22                 | 538.68          |               | -3.55                      |                     |                          | 69.66                      |                          |
      | 9         | 2026-01-09 | 47.22                 | 488.22          |               | -3.24                      |                     |                          | 72.90                      |                          |
      | 10        | 2026-01-10 | 47.22                 | 438.06          |               | -2.94                      |                     |                          | 75.84                      |                          |
      | 11        | 2026-01-11 | 47.22                 | 388.20          |               | -2.64                      |                     |                          | 78.48                      |                          |
      | 12        | 2026-01-12 | 47.22                 | 338.65          |               | -2.34                      |                     |                          | 80.82                      |                          |
      | 13        | 2026-01-13 | 47.22                 | 289.39          |               | -2.04                      |                     |                          | 82.86                      |                          |
      | 14        | 2026-01-14 | 47.22                 | 240.42          |               | -1.74                      |                     |                          | 84.60                      |                          |
      | 15        | 2026-01-15 | 47.22                 | 191.76          |               | -1.45                      |                     |                          | 86.05                      |                          |
      | 16        | 2026-01-16 | 47.22                 | 143.38          |               | -1.15                      |                     |                          | 87.20                      |                          |
      | 17        | 2026-01-17 | 47.22                 | 95.30           |               | -0.86                      |                     |                          | 88.06                      |                          |
      | 18        | 2026-01-18 | 47.22                 | 47.51           |               | -0.57                      |                     |                          | 88.63                      |                          |
      | 19        | 2026-01-19 | 47.22                 | 0.00            |               | -0.29                      |                     |                          | 88.92                      |                          |
      | 20        | 2026-01-20 | 47.22                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |
      | 21        | 2026-01-21 | 47.22                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |
      | 22        | 2026-01-22 | 47.22                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |
      | 23        | 2026-01-23 | 47.22                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |
      | 24        | 2026-01-24 | 47.22                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |
      | 25        | 2026-01-25 | 11.12                 | 0.00            |               | 0.00                       |                     |                          | 0.00                       |                          |

  Scenario: Discount amortization schedule with a repayment on the disbursement date and a later repayment
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct              | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_ADVANCED_ACCOUNTING | 01 January 2026 | 01 January 2026          | 900             | 100000             | 18                |          |
    Then Admin successfully approves the working capital loan on "01 January 2026" with "900" amount and expected disbursement date on "01 January 2026"
    Then Admin successfully disburse the Working Capital loan on "01 January 2026" with "900" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    Then Admin successfully add discount with "100" amount on Working Capital loan account
    And Customer makes repayment on "01 January 2026" with 50 transaction amount on Working Capital loan
    When Admin sets the business date to "02 January 2026"
    And Customer makes repayment on "02 January 2026" with 50 transaction amount on Working Capital loan
    And Admin retrieves the projected amortization schedule
    Then The retrieved amortization schedule has the following summary fields:
      | discountFeeAmount | netDisbursementAmount | totalPaymentVolume | periodPaymentRate | npvDayCount | expectedPaymentAmount | originalPaymentNumber |
      | 100.00            | 900.00                | 100000.00          | 18                | 360         | 50.00                 | 20                    |
    And The retrieved amortization schedule has payments with the following details:
      | paymentNo | date       | expectedPaymentAmount | expectedBalance | actualBalance | expectedAmortizationAmount | actualPaymentAmount | actualAmortizationAmount | expectedDiscountFeeBalance | actualDiscountFeeBalance |
      | 0         | 2026-01-01 | -900.00               | 900.00          | 900.00        |                            |                     |                          | 100.00                     | 100.00                   |
      | 1         | 2026-01-01 | 50.00                 | 859.23          | 859.23        | 9.23                       | 50.00               | 9.23                     | 90.77                      | 90.77                    |
      | 2         | 2026-01-02 | 50.00                 | 818.03          | 818.03        | 8.81                       | 50.00               | 8.81                     | 81.96                      | 81.96                    |
      | 3         | 2026-01-03 | 50.00                 | 776.42          |               | 8.39                       |                     |                          | 73.57                      |                          |
      | 4         | 2026-01-04 | 50.00                 | 734.38          |               | 7.96                       |                     |                          | 65.61                      |                          |
      | 5         | 2026-01-05 | 50.00                 | 691.91          |               | 7.53                       |                     |                          | 58.08                      |                          |
      | 6         | 2026-01-06 | 50.00                 | 649.00          |               | 7.09                       |                     |                          | 50.99                      |                          |
      | 7         | 2026-01-07 | 50.00                 | 605.65          |               | 6.65                       |                     |                          | 44.34                      |                          |
      | 8         | 2026-01-08 | 50.00                 | 561.86          |               | 6.21                       |                     |                          | 38.13                      |                          |
      | 9         | 2026-01-09 | 50.00                 | 517.62          |               | 5.76                       |                     |                          | 32.37                      |                          |
      | 10        | 2026-01-10 | 50.00                 | 472.93          |               | 5.31                       |                     |                          | 27.06                      |                          |
      | 11        | 2026-01-11 | 50.00                 | 427.78          |               | 4.85                       |                     |                          | 22.21                      |                          |
      | 12        | 2026-01-12 | 50.00                 | 382.16          |               | 4.39                       |                     |                          | 17.82                      |                          |
      | 13        | 2026-01-13 | 50.00                 | 336.08          |               | 3.92                       |                     |                          | 13.90                      |                          |
      | 14        | 2026-01-14 | 50.00                 | 289.52          |               | 3.45                       |                     |                          | 10.45                      |                          |
      | 15        | 2026-01-15 | 50.00                 | 242.49          |               | 2.97                       |                     |                          | 7.48                       |                          |
      | 16        | 2026-01-16 | 50.00                 | 194.98          |               | 2.49                       |                     |                          | 4.99                       |                          |
      | 17        | 2026-01-17 | 50.00                 | 146.98          |               | 2.00                       |                     |                          | 2.99                       |                          |
      | 18        | 2026-01-18 | 50.00                 | 98.48           |               | 1.51                       |                     |                          | 1.48                       |                          |
      | 19        | 2026-01-19 | 50.00                 | 49.49           |               | 1.01                       |                     |                          | 0.47                       |                          |
      | 20        | 2026-01-20 | 50.00                 | 0.00            |               | 0.51                       |                     |                          | -0.04                      |                          |
