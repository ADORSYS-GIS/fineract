@WorkingCapital
@WorkingCapitalBreachRescheduleActionFeature @WCCOBFeature
Feature: Working Capital Breach Reschedule Action

  Scenario: Verify that breach reschedule changes minimumPayment only
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule has the following data:
      | periodNumber | fromDate   | toDate     | numberOfDays | minPaymentAmount | outstandingAmount | nearBreach | breach |
      | 1            | 2026-01-01 | 2026-02-28 | 59           | 110.70           | 110.70            | null       | true   |
      | 2            | 2026-03-01 | 2026-04-30 | 61           | 110.70           | 110.70            | null       | true   |
      | 3            | 2026-05-01 | 2026-06-30 | 61           | 110.70           | 110.70            | null       | null   |
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType |
      | 1              | PERCENTAGE         |
    When Admin sets the business date to "15 August 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2026-01-01 | 2026-02-28 | 110.70           | 110.70            | true   |
      | 2            | 2026-03-01 | 2026-04-30 | 110.70           | 110.70            | true   |
      | 3            | 2026-05-01 | 2026-06-30 | 90               | 90                | true   |
      | 4            | 2026-07-01 | 2026-08-31 | 90               | 90                |        |

  Scenario: Verify that breach reschedule changes frequency only
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin creates WC breach reschedule action with the following parameters:
      | frequency | frequencyType |
      | 30        | DAYS          |
    When Admin sets the business date to "15 August 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2026-01-01 | 2026-02-28 | 110.70           | 110.70            | true   |
      | 2            | 2026-03-01 | 2026-04-30 | 110.70           | 110.70            | true   |
      | 3            | 2026-05-01 | 2026-06-30 | 110.70           | 110.70            | true   |
      | 4            | 2026-07-01 | 2026-07-30 | 110.70           | 110.70            | true   |
      | 5            | 2026-07-31 | 2026-08-29 | 110.70           | 110.70            |        |

  Scenario: Verify that breach reschedule changes minimumPayment and frequency
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 1              | PERCENTAGE         | 30        | DAYS          |
    When Admin sets the business date to "15 August 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2026-01-01 | 2026-02-28 | 110.70           | 110.70            | true   |
      | 2            | 2026-03-01 | 2026-04-30 | 110.70           | 110.70            | true   |
      | 3            | 2026-05-01 | 2026-06-30 | 90               | 90                | true   |
      | 4            | 2026-07-01 | 2026-07-30 | 90               | 90                | true   |
      | 5            | 2026-07-31 | 2026-08-29 | 90               | 90                |        |

  Scenario: Verify that the latest breach reschedule action wins
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 2              | PERCENTAGE         | 30        | DAYS          |
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 1              | PERCENTAGE         | 30        | DAYS          |
    When Admin sets the business date to "15 August 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 3            | 2026-05-01 | 2026-06-30 | 90               | 90                | true   |
      | 4            | 2026-07-01 | 2026-07-30 | 90               | 90                | true   |

  Scenario: Verify multiple breach reschedules on the same date are stored in history
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 2              | PERCENTAGE         | 2         | MONTHS        |
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 1              | PERCENTAGE         | 2         | MONTHS        |
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 1.5            | PERCENTAGE         | 2         | MONTHS        |
    And WC loan breach actions have the following data:
      | action     | startDate    | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | RESCHEDULE | 01 June 2026 | 2              | PERCENTAGE         | 2         | MONTHS        |
      | RESCHEDULE | 01 June 2026 | 1              | PERCENTAGE         | 2         | MONTHS        |
      | RESCHEDULE | 01 June 2026 | 1.5            | PERCENTAGE         | 2         | MONTHS        |

  Scenario: Verify breach reschedule fails when no change parameters are provided
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Admin fails to create WC breach reschedule action with no parameters with error containing "reschedule.no.change.parameters"

  Scenario: Verify breach reschedule fails with negative minimumPayment
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Admin fails to create WC breach reschedule action with minimumPayment -1 PERCENTAGE and frequency 30 DAYS with error containing "minimumPayment"

  Scenario: Verify that payment-only reschedule after frequency reschedule falls back to product breach frequency
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a new Working Capital Loan Product with breachId and overrides enabled
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    And Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "01 June 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin creates WC breach reschedule action with the following parameters:
      | frequency | frequencyType |
      | 30        | DAYS          |
    When Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType |
      | 1              | PERCENTAGE         |
    When Admin sets the business date to "15 August 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2026-01-01 | 2026-02-28 | 110.70           | 110.70            | true   |
      | 2            | 2026-03-01 | 2026-04-30 | 110.70           | 110.70            | true   |
      | 3            | 2026-05-01 | 2026-06-30 | 90               | 90                | true   |
      | 4            | 2026-07-01 | 2026-08-31 | 90               | 90                |        |

  Scenario: Verify breach reschedule updates current period after partial repayment and replays payments
    When Admin sets the business date to "01 January 2019"
    And Admin creates a client with random data
    And Admin creates a Working Capital Loan Product with custom breach config and overrides enabled:
      | breachFrequency | breachFrequencyType | breachAmountCalculationType | breachAmount | delinquencyGraceDays |
      | 90              | DAYS                | PERCENTAGE                  | 9            | 3                    |
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2019 | 01 January 2019          | 9000            | 100000             | 18                | 1000     |
    And Admin successfully approves the working capital loan on "01 January 2019" with "9000" amount and expected disbursement date on "01 January 2019"
    And Admin successfully disburse the Working Capital loan on "01 January 2019" with "9000" EUR transaction amount and "1000" discount amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "05 March 2019"
    And Customer makes repayment on "05 March 2019" with 450.0 transaction amount on Working Capital loan
    When Admin sets the business date to "10 March 2019"
    And Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType |
      | 5              | PERCENTAGE         |
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 500.00           | 50.00             |        |

  Scenario: Verify breach reschedule preserves already evaluated periods
    When Admin sets the business date to "01 January 2019"
    And Admin creates a client with random data
    And Admin creates a Working Capital Loan Product with custom breach config and overrides enabled:
      | breachFrequency | breachFrequencyType | breachAmountCalculationType | breachAmount | delinquencyGraceDays |
      | 90              | DAYS                | PERCENTAGE                  | 10           | 3                    |
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2019 | 01 January 2019          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2019" with "9000" amount and expected disbursement date on "01 January 2019"
    And Admin successfully disburse the Working Capital loan on "01 January 2019" with "9000" EUR transaction amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "05 March 2019"
    And Customer makes repayment on "05 March 2019" with 450.0 transaction amount on Working Capital loan
    When Admin sets the business date to "06 April 2019"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 900.00           | 450.00            | true   |
      | 2            | 2019-04-01 | 2019-06-29 | 900.00           | 900.00            |        |
    When Admin sets the business date to "10 April 2019"
    And Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType |
      | 5              | PERCENTAGE         |
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 900.00           | 450.00            | true   |
      | 2            | 2019-04-01 | 2019-06-29 | 450.00           | 450.00            |        |

  Scenario: Verify breach reschedule changes frequency from 90 days to 30 days for current and future periods
    When Admin sets the business date to "01 January 2019"
    And Admin creates a client with random data
    And Admin creates a Working Capital Loan Product with custom breach config and overrides enabled:
      | breachFrequency | breachFrequencyType | breachAmountCalculationType | breachAmount | delinquencyGraceDays |
      | 90              | DAYS                | PERCENTAGE                  | 9            | 3                    |
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2019 | 01 January 2019          | 9000            | 100000             | 18                | 1000     |
    And Admin successfully approves the working capital loan on "01 January 2019" with "9000" amount and expected disbursement date on "01 January 2019"
    And Admin successfully disburse the Working Capital loan on "01 January 2019" with "9000" EUR transaction amount and "1000" discount amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "10 March 2019"
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 900.00           | 900.00            |        |
    And Admin creates WC breach reschedule action with the following parameters:
      | frequency | frequencyType |
      | 30        | DAYS          |
    When Admin sets the business date to "04 April 2019"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "15 June 2019"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 900.00           | 900.00            | true   |
      | 2            | 2019-04-01 | 2019-04-30 | 900.00           | 900.00            | true   |
      | 3            | 2019-05-01 | 2019-05-30 | 900.00           | 900.00            | true   |
      | 4            | 2019-05-31 | 2019-06-29 | 900.00           | 900.00            |        |

  Scenario: Verify breach reschedule changes minimum payment and frequency together
    When Admin sets the business date to "01 January 2019"
    And Admin creates a client with random data
    And Admin creates a Working Capital Loan Product with custom breach config and overrides enabled:
      | breachFrequency | breachFrequencyType | breachAmountCalculationType | breachAmount | delinquencyGraceDays |
      | 90              | DAYS                | PERCENTAGE                  | 9            | 3                    |
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP_BREACH | 01 January 2019 | 01 January 2019          | 9000            | 100000             | 18                | 1000     |
    And Admin successfully approves the working capital loan on "01 January 2019" with "9000" amount and expected disbursement date on "01 January 2019"
    And Admin successfully disburse the Working Capital loan on "01 January 2019" with "9000" EUR transaction amount and "1000" discount amount
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "10 March 2019"
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 900.00           | 900.00            |        |
    And Admin creates WC breach reschedule action with the following parameters:
      | minimumPayment | minimumPaymentType | frequency | frequencyType |
      | 5              | PERCENTAGE         | 30        | DAYS          |
    When Admin sets the business date to "04 April 2019"
    And Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "15 June 2019"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan breach schedule periods have specific data:
      | periodNumber | fromDate   | toDate     | minPaymentAmount | outstandingAmount | breach |
      | 1            | 2019-01-01 | 2019-03-31 | 500.00           | 500.00            | true   |
      | 2            | 2019-04-01 | 2019-04-30 | 500.00           | 500.00            | true   |
      | 3            | 2019-05-01 | 2019-05-30 | 500.00           | 500.00            | true   |
      | 4            | 2019-05-31 | 2019-06-29 | 500.00           | 500.00            |        |
