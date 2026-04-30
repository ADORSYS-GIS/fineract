@WorkingCapitalLoanGoodwillCreditFeature
Feature: Working Capital Loan Goodwill Credit

  @TestRailId:Cxxxx1
  Scenario: Verify working capital loan goodwill credit - UC1: simple goodwill credit
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPayment | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000       | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name | submittedOnDate | expectedDisbursementDate | status   | principal | approvedPrincipal | totalPayment | periodPaymentRate | discount |
      | WCLP         | 2026-01-01      | 2026-01-01               | Approved | 9000.0    | 9000.0            | 100000.0     | 18.0              | 0.0      |
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    And Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPayment | periodPaymentRate | discount |
      | WCLP         | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0     | 18.0              | 0.0      |
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    And Customer makes "goodwillcredit" on "10 January 2026" with 270.0 transaction amount on Working Capital loan
    Then Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-01-30 | 270.0          | 270.0      | 0.0               | true                  | 0.0              | 0              |
    Then Working Capital loan amortization schedule has 4 periods, with the following data for periods:
      | paymentNo | paymentDate      | count | paymentsLeft | expectedPaymentAmount | forecastPaymentAmount | actualPaymentAmount | discountFactor        | npvValue | balance | expectedAmortizationAmount | netAmortizationAmount | actualAmortizationAmount | incomeModification | deferredBalance |
      | 0         | 01 January 2026  | 3     | 0            | -9000.00              |                       |                     | 1                     | -9000.00 | 9000.00 |                            |                       |                          |                    | 0.00            |
      | 1         | 02 January 2026  | 2     | 0            | 5000.00               | 5000.00               | 270.00              | 1                     | 270.00   | 4658.91 | 0.00                       | 0.00                  | 35.58                    | 35.58              | 0.00            |
      | 2         | 03 January 2026  | 1     | 1            | 5000.00               | 5000.00               |                     | 0.9317821063276353179 | 4658.91  | 0.00    | 0.00                       | 0.00                  |                          | 0.00               | 0.00            |
      | 3         | 04 January 2026  | 0     | 2            |                       | 4730.00               |                     | 0.8682178936723646887 | 4106.67  |         |                            | 0.00                  |                          |                    | 0.00            |
    And Working Capital Loan has transactions:
      | transactionDate | type         | transactionAmount | principalPortion | feeChargesPortion | penaltyChargesPortion | reversed |
      | 01 January 2026 | Disbursement | 9000.0             | 9000.0            | 0.0               | 0.0                   | false    |
      | 10 January 2026 | Goodwill Credit | 270.0              | 270.0             | 0.0               | 0.0                   | false    |
