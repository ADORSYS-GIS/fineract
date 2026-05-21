@WorkingCapital
@WorkingCapitalLoanChargesFeature
Feature: WorkingCapitalLoanChargesFeature

  @TestRailId:C80954
  Scenario: Verify Working Capital Charge product - UC1: charge can be created modified and deleted
    When Admin creates working capital loan charge
    When Admin updates working capital loan charge
    When Admin deletes working capital loan charge

  @TestRailId:C80955
  Scenario: Verify Working Capital Charge product - UC2: template API returns filtered options
    Then Admin retrieves the charge template for Working Capital Loan
    Then The charge template chargeTimeTypeOptions contains only Specified due date
    Then The charge template chargeCalculationTypeOptions contains only Flat
    Then The charge template chargePaymentModeOptions contains only Regular

  @TestRailId:C80956
  Scenario: Verify Working Capital Charge product - UC3: template API with Specified due date returns Flat calculation type only
    Then Admin retrieves the charge template for Working Capital Loan with charge time type "SPECIFIED_DUE_DATE"
    Then The charge template chargeCalculationTypeOptions contains only Flat

  @TestRailId:C80957
  Scenario: Verify Working Capital Charge product - UC4: charge can be created as penalty
    When Admin creates working capital loan charge as penalty
    Then Admin retrieves working capital loan charge and verifies it is a penalty
    When Admin deletes working capital loan charge

  @TestRailId:C80958
  Scenario: Verify Working Capital Charge product - UC5: creating charge without payment mode will result defaults to payment mode Regular
    When Admin creates working capital loan charge without payment mode
    Then Admin retrieves working capital loan charge and verifies payment mode is Regular
    When Admin deletes working capital loan charge

  @TestRailId:C80959
  Scenario: Verify Working Capital Charge product - UC6: invalid chargeTimeType Disbursement fails (Negative)
    Then Creating working capital loan charge with "DISBURSEMENT" chargeTimeType and "FLAT" chargeCalculationType results an error with the following data:
      | httpCode | errorMessage                                                                                        |
      | 400      | The parameter `chargeTimeType` must be one of [ 2 ] .                                               |

  @TestRailId:C80960
  Scenario: Verify Working Capital Charge product - UC7: invalid chargeCalculationType Percentage Amount fails (Negative)
    Then Creating working capital loan charge with "SPECIFIED_DUE_DATE" chargeTimeType and "PERCENTAGE_AMOUNT" chargeCalculationType results an error with the following data:
      | httpCode | errorMessage                                                                                                  |
      | 400      | The parameter `chargeCalculationType` must be one of [ 1 ] .                                                  |

  @TestRailId:C80961
  Scenario: Verify Working Capital Charge product - UC8: invalid chargeTimeType Instalment Fee fails (Negative)
    Then Creating working capital loan charge with "INSTALLMENT_FEE" chargeTimeType and "FLAT" chargeCalculationType results an error with the following data:
      | httpCode | errorMessage                                                                                        |
      | 400      | The parameter `chargeTimeType` must be one of [ 2 ] .                                               |

  @TestRailId:TODO__1
  Scenario: Verify Working Capital Charge product can be added to disbursed loan
    When Admin creates working capital loan charge without payment mode
    Then Admin retrieves working capital loan charge and verifies payment mode is Regular
    When Admin sets the business date to "01 January 2026"
    And Admin creates a client with random data
    And Admin creates a working capital loan with the following data:
      | LoanProduct | submittedOnDate | expectedDisbursementDate | principalAmount | totalPayment | periodPaymentRate | discount |
      | WCLP        | 01 January 2026 | 01 January 2026          | 9000            | 100000       | 18                | 0        |
    And Admin successfully approves the working capital loan on "01 January 2026" with "9000" amount and expected disbursement date on "01 January 2026"
    Then Working capital loan approval was successful
    And Working capital loan account has the correct data:
      | product.name | submittedOnDate | expectedDisbursementDate | status   | principal | approvedPrincipal | totalPayment | periodPaymentRate | discountApproved |
      | WCLP         | 2026-01-01      | 2026-01-01               | Approved | 9000.0    | 9000.0            | 100000.0     | 18.0              | null             |
    When Admin successfully disburse the Working Capital loan on "01 January 2026" with "9000" EUR transaction amount
    Then Working Capital loan status will be "ACTIVE"
    And Verify Working Capital loan disbursement was successful
    And Working capital loan account has the correct data:
      | product.name | submittedOnDate | expectedDisbursementDate | status | principal | approvedPrincipal | totalPayment | periodPaymentRate | discount |
      | WCLP         | 2026-01-01      | 2026-01-01               | Active | 9000.0    | 9000.0            | 100000.0     | 18.0              | null     |
    When Admin sets the business date to "10 January 2026"
    And Admin runs inline COB job for Working Capital Loan by loanId
    Then Admin retrieves working capital loan charge template by loan id
    Then Admin add working capital loan charge by loan id and charge id with amount 35.0 and due date "12-01-2026"
    Then Admin add working capital loan charge by loan id and charge id with amount 45.0 and due date "21-01-2026"
    When Admin fails to delete working capital loan charge with status 403 message "This charge cannot be deleted, it is already used in"
    Then Working Capital Loan has the created charges
