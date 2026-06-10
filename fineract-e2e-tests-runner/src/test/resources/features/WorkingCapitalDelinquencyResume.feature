@WorkingCapital
@WorkingCapitalDelinquencyResumeFeature
Feature: Working Capital Delinquency Resume

  @TestRailId:C85172
  Scenario: Verify working capital loan delinquency resume - UC1: resume shortens active pause and delinquency range schedule
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause with startDate "01 January 2026" and endDate "16 January 2026"
    Then Working Capital loan delinquency action has the following data:
      | action | startDate  | endDate    |
      | PAUSE  | 2026-01-01 | 2026-01-16 |
    And Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-02-14 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency resume with startDate "10 January 2026"
    Then Working Capital loan delinquency action has the following data:
      | action | startDate  | endDate    |
      | PAUSE  | 2026-01-01 | 2026-01-16 |
      | RESUME | 2026-01-10 |            |
    And Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-02-08 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |

  @TestRailId:C85173
  Scenario: Verify working capital loan delinquency resume - UC2: resume on the day before planned pause end shortens schedule by one day
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000       | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause with startDate "01 January 2026" and endDate "16 January 2026"
    And Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-02-14 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |
    When Admin sets the business date to "15 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency resume with startDate "15 January 2026"
    Then Working Capital loan delinquency action has the following data:
      | action | startDate  | endDate    |
      | PAUSE  | 2026-01-01 | 2026-01-16 |
      | RESUME | 2026-01-15 |            |
    And Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-02-13 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |

  @TestRailId:C85174
  Scenario: Verify working capital loan delinquency resume - UC3: resume by external ID
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000       | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause by external ID with startDate "01 January 2026" and endDate "16 January 2026"
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency resume by external ID with startDate "10 January 2026"
    Then Working Capital loan delinquency action by external ID has the following data:
      | action | startDate  | endDate    |
      | PAUSE  | 2026-01-01 | 2026-01-16 |
      | RESUME | 2026-01-10 |            |

  @TestRailId:C85175
  Scenario: Verify working capital loan delinquency resume - UC4: resume without an active pause results an error (Negative)
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Initiating a Working Capital loan delinquency resume with startDate "10 January 2026" results an error with the following data:
      | httpCode | errorMessage                                                      |
      | 400      | Resume Delinquency Action can only be created during an active pause |

  @TestRailId:C85176
  Scenario: Verify working capital loan delinquency resume - UC5: backdated resume results an error (Negative)
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause with startDate "01 January 2026" and endDate "16 January 2026"
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Initiating a Working Capital loan delinquency resume with startDate "09 January 2026" results an error with the following data:
      | httpCode | errorMessage                                                                  |
      | 400      | Start date of the Resume Delinquency action must be the current business date |

  @TestRailId:C85177
  Scenario: Verify working capital loan delinquency resume - UC6: resume on pause start date results an error (Negative)
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause with startDate "01 January 2026" and endDate "16 January 2026"
    Then Initiating a Working Capital loan delinquency resume with startDate "01 January 2026" results an error with the following data:
      | httpCode | errorMessage                                           |
      | 400      | Resume date must be after the active pause start date |

  @TestRailId:C85178
  Scenario: Verify working capital loan delinquency resume - UC7: multiple resumes on the same pause results an error (Negative)
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency pause with startDate "01 January 2026" and endDate "16 January 2026"
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    And Admin initiate a Working Capital loan delinquency resume with startDate "10 January 2026"
    Then Initiating a Working Capital loan delinquency resume with startDate "10 January 2026" results an error with the following data:
      | httpCode | errorMessage                                                         |
      | 400      | Resume Delinquency Action can only be created during an active pause |

  @TestRailId:C85179
  Scenario: Verify working capital loan delinquency resume - UC8: resume recalculates delinquency immediately
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPaymentVolume | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000             | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    When Admin runs inline COB job for Working Capital Loan by loanId
    When Admin sets the business date to "15 February 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-01-30 | 270.0          | 0.0        | 270.0             | false                 | 270.0            | 16             |
      | 2            | 2026-01-31 | 2026-03-01 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |
    And Admin initiate a Working Capital loan delinquency pause with startDate "15 February 2026" and endDate "15 March 2026"
    Then Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-01-30 | 270.0          | 0.0        | 270.0             | false                 | 270.0            | 16             |
      | 2            | 2026-01-31 | 2026-03-29 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |
    When Admin sets the business date to "20 February 2026"
    And Admin initiate a Working Capital loan delinquency resume with startDate "20 February 2026"
    Then Working Capital loan delinquency range schedule has the following data:
      | periodNumber | fromDate   | toDate     | expectedAmount | paidAmount | outstandingAmount | minPaymentCriteriaMet | delinquentAmount | delinquentDays |
      | 1            | 2026-01-01 | 2026-01-30 | 270.0          | 0.0        | 270.0             | false                 | 270.0            | 21             |
      | 2            | 2026-01-31 | 2026-03-06 | 270.0          | 0.0        | 270.0             | null                  | null             | null           |
