# Fineract Config CLI - YAML Schema Reference

**Complete YAML structure and examples**

**Version**: 1.0
**Last Updated**: 2025-01-20

---

## Table of Contents

- [Overview](#overview)
- [Root Structure](#root-structure)
- [Complete Example](#complete-example)
- [Phase-by-Phase Examples](#phase-by-phase-examples)
- [Advanced Features](#advanced-features)
- [Schema Validation](#schema-validation)

---

## Overview

This document provides the complete YAML schema for Fineract configuration files.

### File Format

- **Format**: YAML 1.2
- **Encoding**: UTF-8
- **Extension**: `.yml` or `.yaml`
- **Multiple Files**: Supported via glob patterns (`/config/*.yml`)

### Basic Structure

```yaml
# Optional: Tenant identifier (default: 'default')
tenant: default

# Phase 1: System Configuration
systemConfig:
  currency: {...}
  workingDays: [...]
  globalConfig: [...]
  codes: [...]
  accountNumbering: [...]
  notifications: {...}
  notificationTemplates: [...]
  dataTables: [...]

# Phase 2: Security & Organization
roles: [...]
offices: [...]
staff: [...]
tellers: [...]

# Phase 3: Accounting & Workflow
chartOfAccounts: [...]
financialActivityMappings: [...]
tellerAccountingRules: [...]
makerCheckerConfig: [...]
schedulerJobs: [...]
loanProvisioningCriteria: [...]

# Phase 4: Financial Products
floatingRates: [...]
taxGroups: [...]
charges: [...]
fundSources: [...]
paymentTypes: [...]
holidayCalendar: [...]
loanProducts: [...]
delinquencyBuckets: [...]
savingsProducts: [...]
collateralTypes: [...]

# Phase 5: Client Accounts
clients: [...]
savingsAccounts: [...]
loanAccounts: [...]
loanCollateral: [...]
loanGuarantors: [...]

# Phase 6: Transactions (Optional)
savingsDeposits: [...]
savingsWithdrawals: [...]
loanRepayments: [...]
interBranchTransfers: [...]
```

---

## Root Structure

### Tenant

```yaml
# Optional: Specify tenant (default: 'default')
tenant: default
```

### Comments

```yaml
# This is a comment
# Comments are ignored during parsing

systemConfig:
  # Inline comments are supported
  currency:
    code: USD  # ISO 4217 code
```

---

## Complete Example

### Minimal Configuration

```yaml
tenant: default

# Minimum required configuration
systemConfig:
  currency:
    code: USD
    name: US Dollar
    decimalPlaces: 2

  workingDays:
    days:
      - MONDAY
      - TUESDAY
      - WEDNESDAY
      - THURSDAY
      - FRIDAY

offices:
  - name: Main Office
    externalId: MAIN001
    openingDate: "2024-01-01"
```

### Production Configuration (Cameroon MFI Example)

```yaml
tenant: default

#==============================================================================
# PHASE 1: SYSTEM CONFIGURATION
#==============================================================================

systemConfig:
  # Currency
  currency:
    code: XAF
    name: Central African CFA Franc
    decimalPlaces: 0

  # Working Days
  workingDays:
    days:
      - MONDAY
      - TUESDAY
      - WEDNESDAY
      - THURSDAY
      - FRIDAY
    repaymentReschedulingRule: MOVE_TO_NEXT_WORKING_DAY

  # Global Configuration
  globalConfig:
    - name: maker-checker
      enabled: true

    - name: reschedule-future-repayments
      enabled: true

    - name: allow-transactions-on-holiday
      enabled: false

    - name: penalty-wait-period
      value: 2

    - name: days-in-year-type
      value: "ACTUAL"

    - name: amazon-S3_bucket_name
      value: ${env:S3_BUCKET}

    - name: amazon-S3_access_key
      value: ${env:AWS_ACCESS_KEY}

    - name: amazon-S3_secret_key
      value: ${env:AWS_SECRET_KEY}

  # Codes & Code Values
  codes:
    - name: Gender
      systemDefined: false
      values:
        - name: Male
          position: 1
          isActive: true

        - name: Female
          position: 2
          isActive: true

        - name: Other
          position: 3
          isActive: true

    - name: Client Type
      values:
        - name: Individual
          position: 1

        - name: Business
          position: 2

    - name: Marital Status
      values:
        - name: Single
        - name: Married
        - name: Divorced
        - name: Widowed

    - name: Loan Purpose
      values:
        - name: Business Expansion
        - name: Working Capital
        - name: Equipment Purchase
        - name: Agriculture
        - name: Education
        - name: Housing

  # Account Number Preferences
  accountNumbering:
    - entityType: CLIENT
      prefixType: OFFICE
      length: 6

    - entityType: LOAN
      prefixType: PRODUCT
      length: 6

    - entityType: SAVINGS
      prefixType: PRODUCT
      length: 8

  # Notification Templates
  notificationTemplates:
    - name: Client Activation
      type: SMS
      message: "Dear {{client.firstname}}, welcome to {{office.name}}! Your client ID is {{client.accountNo}}."

    - name: Loan Approval
      type: EMAIL
      subject: "Loan Approved - {{loan.accountNo}}"
      message: |
        Dear {{client.name}},

        Congratulations! Your loan application has been approved.

        Loan Details:
        - Account Number: {{loan.accountNo}}
        - Principal: {{loan.principal}} {{loan.currency}}
        - Interest Rate: {{loan.interestRate}}%
        - Term: {{loan.numberOfRepayments}} months

        Please visit {{office.name}} to complete the disbursement process.

        Thank you,
        {{office.name}} Team

  # Data Tables (Custom Fields)
  dataTables:
    - entityType: CLIENT
      tableName: client_additional_info
      columns:
        - name: id_type
          type: DROPDOWN
          code: ID Type
          mandatory: true

        - name: id_number
          type: STRING
          length: 50
          mandatory: true

        - name: id_expiry_date
          type: DATE

        - name: next_of_kin_name
          type: STRING
          length: 100

        - name: next_of_kin_phone
          type: STRING
          length: 20

        - name: employer_name
          type: STRING
          length: 100

    - entityType: LOAN
      tableName: loan_additional_info
      columns:
        - name: loan_purpose_detail
          type: TEXT

        - name: collateral_description
          type: TEXT

        - name: collateral_value
          type: DECIMAL

#==============================================================================
# PHASE 2: SECURITY & ORGANIZATION
#==============================================================================

roles:
  - name: Branch Manager
    description: Full branch management capabilities
    permissions:
      - ALL_FUNCTIONS_READ_CLIENT
      - CREATE_CLIENT
      - UPDATE_CLIENT
      - ACTIVATE_CLIENT
      - ALL_FUNCTIONS_LOAN
      - CREATE_LOAN
      - APPROVE_LOAN
      - DISBURSE_LOAN
      - REPAYMENT_LOAN
      - ALL_FUNCTIONS_READ_SAVINGS
      - CREATE_SAVINGS
      - DEPOSIT_SAVINGS
      - WITHDRAWAL_SAVINGS
      - READ_OFFICE
      - READ_STAFF
      - CREATE_STAFF
      - READ_REPORT
      - READ_ACCOUNTING

  - name: Loan Officer
    description: Client and loan management
    permissions:
      - READ_CLIENT
      - CREATE_CLIENT
      - UPDATE_CLIENT
      - READ_LOAN
      - CREATE_LOAN
      - UPDATE_LOAN
      - REPAYMENT_LOAN
      - READ_SAVINGS
      - READ_REPORT

  - name: Cashier
    description: Teller operations
    permissions:
      - READ_CLIENT
      - READ_LOAN
      - REPAYMENT_LOAN
      - ALL_FUNCTIONS_READ_SAVINGS
      - DEPOSIT_SAVINGS
      - WITHDRAWAL_SAVINGS
      - ALL_FUNCTIONS_TELLER

offices:
  - name: Head Office
    externalId: HO001
    openingDate: "2024-01-01"
    address: "Avenue Kennedy"
    city: Yaounde
    region: Centre
    country: Cameroon
    phone: "+237222123456"
    email: headoffice@example.com

  - name: Douala Branch
    externalId: DLA001
    parentOffice: $office.Head Office
    openingDate: "2024-02-01"
    address: "Boulevard de la Liberté"
    city: Douala
    region: Littoral
    country: Cameroon
    phone: "+237233123456"
    email: douala@example.com

  - name: Bafoussam Branch
    externalId: BAF001
    parentOffice: $office.Head Office
    openingDate: "2024-03-01"
    city: Bafoussam
    region: West
    country: Cameroon

  - name: Bamenda Branch
    externalId: BAM001
    parentOffice: $office.Head Office
    openingDate: "2024-04-01"
    city: Bamenda
    region: Northwest
    country: Cameroon

staff:
  # Branch Managers
  - firstname: Jean
    lastname: Dupont
    office: $office.Head Office
    role: $role.Branch Manager
    username: jean.dupont
    email: jean.dupont@example.com
    mobile: "+237677123456"
    joiningDate: "2024-01-15"
    externalId: STF001
    createUser: true
    password: ${env:DEFAULT_PASSWORD}

  - firstname: Marie
    lastname: Mbarga
    office: $office.Douala Branch
    role: $role.Branch Manager
    username: marie.mbarga
    email: marie.mbarga@example.com
    externalId: STF002
    createUser: true

  # Loan Officers
  - firstname: Paul
    lastname: Nkembe
    office: $office.Head Office
    role: $role.Loan Officer
    isLoanOfficer: true
    username: paul.nkembe
    email: paul.nkembe@example.com
    externalId: STF003
    createUser: true

  - firstname: Grace
    lastname: Fonkou
    office: $office.Douala Branch
    role: $role.Loan Officer
    isLoanOfficer: true
    username: grace.fonkou
    email: grace.fonkou@example.com
    externalId: STF004
    createUser: true

  # Cashiers
  - firstname: David
    lastname: Tabi
    office: $office.Head Office
    role: $role.Cashier
    isTeller: true
    username: david.tabi
    externalId: STF005
    createUser: true

  - firstname: Sylvie
    lastname: Kamga
    office: $office.Douala Branch
    role: $role.Cashier
    isTeller: true
    username: sylvie.kamga
    externalId: STF006
    createUser: true

tellers:
  - office: $office.Head Office
    name: Teller 1 - Head Office
    description: Main counter
    startDate: "2024-01-01"
    status: ACTIVE

  - office: $office.Douala Branch
    name: Teller 1 - Douala
    description: Main counter
    startDate: "2024-02-01"
    status: ACTIVE

#==============================================================================
# PHASE 3: ACCOUNTING & WORKFLOW
#==============================================================================

chartOfAccounts:
  # ASSETS
  - name: Banks
    glCode: "41"
    type: ASSET
    usage: DETAIL
    manualEntriesAllowed: true

  - name: Cash on Hand
    glCode: "42"
    type: ASSET
    usage: DETAIL
    tag: TELLER

  - name: Loans - Microcredit
    glCode: "51"
    type: ASSET
    usage: DETAIL

  - name: Interest Receivable
    glCode: "53"
    type: ASSET
    usage: DETAIL

  - name: Provision for Loan Losses
    glCode: "55"
    type: ASSET
    usage: DETAIL

  - name: Due from Other Branches
    glCode: "122"
    type: ASSET
    usage: DETAIL

  # LIABILITIES
  - name: Voluntary Savings
    glCode: "61"
    type: LIABILITY
    usage: DETAIL

  - name: Fixed Deposits
    glCode: "62"
    type: LIABILITY
    usage: DETAIL

  - name: Interest Payable
    glCode: "64"
    type: LIABILITY
    usage: DETAIL

  - name: Due to Other Branches
    glCode: "131"
    type: LIABILITY
    usage: DETAIL

  - name: Tax Payable - WHT
    glCode: "141"
    type: LIABILITY
    usage: DETAIL

  # EQUITY
  - name: Capital
    glCode: "30"
    type: EQUITY
    usage: DETAIL

  # INCOME
  - name: Interest Income - Loans
    glCode: "81"
    type: INCOME
    usage: DETAIL

  - name: Fee Income
    glCode: "82"
    type: INCOME
    usage: DETAIL

  - name: Penalty Income
    glCode: "84"
    type: INCOME
    usage: DETAIL

  - name: Recovery Income
    glCode: "86"
    type: INCOME
    usage: DETAIL

  # EXPENSES
  - name: Operating Expenses
    glCode: "91"
    type: EXPENSE
    usage: DETAIL

  - name: Provision Expense
    glCode: "94"
    type: EXPENSE
    usage: DETAIL

  - name: Losses Written Off
    glCode: "95"
    type: EXPENSE
    usage: DETAIL

  - name: Cash Shortage
    glCode: "98"
    type: EXPENSE
    usage: DETAIL

financialActivityMappings:
  - activity: ASSET_TRANSFER
    account: $glAccount.122

  - activity: LIABILITY_TRANSFER
    account: $glAccount.131

  - activity: CASH_AT_MAINVAULT
    account: $glAccount.42

  - activity: CASH_AT_TELLER
    account: $glAccount.42

  - activity: OPENING_BALANCES_CONTRA_ACCOUNT
    account: $glAccount.30

  - activity: FUND_SOURCE
    account: $glAccount.41

schedulerJobs:
  - name: Post Interest For Savings
    cronExpression: "0 0 0 * * ?"
    active: true

  - name: Apply Charges To Overdue Loans
    cronExpression: "0 0 3 * * ?"
    active: true

  - name: Update Loan Arrears Aging
    cronExpression: "0 0 4 * * ?"
    active: true

#==============================================================================
# PHASE 4: FINANCIAL PRODUCTS
#==============================================================================

charges:
  - name: Loan Processing Fee
    chargeType: LOAN
    amount: 5000
    currency: XAF
    chargeCalculationType: FLAT
    chargeTimeType: DISBURSEMENT
    active: true

  - name: Late Payment Penalty
    chargeType: LOAN
    amount: 2
    currency: XAF
    chargeCalculationType: PERCENT_OF_AMOUNT
    chargeTimeType: OVERDUE_INSTALLMENT
    active: true

  - name: Monthly Maintenance Fee
    chargeType: SAVINGS
    amount: 500
    currency: XAF
    chargeCalculationType: FLAT
    chargeTimeType: MONTHLY_FEE
    feeOnMonthDay: 1
    active: true

fundSources:
  - name: Own Capital
    externalId: FUND001

  - name: Bank Loans
    externalId: FUND002

  - name: Donor Funds
    externalId: FUND003

paymentTypes:
  - name: Cash
    description: Cash payment
    isCashPayment: true
    position: 1

  - name: MTN Mobile Money
    description: MTN MoMo
    isCashPayment: false
    position: 2

  - name: Orange Money
    description: Orange Money
    isCashPayment: false
    position: 3

  - name: Bank Transfer
    description: Bank transfer
    isCashPayment: false
    position: 4

loanProducts:
  - name: Microcredit Solidarity Loan
    shortName: MSOL
    description: Small loans for micro-entrepreneurs
    currencyCode: XAF
    digitsAfterDecimal: 0
    principal: 500000
    minPrincipal: 50000
    maxPrincipal: 500000
    numberOfRepayments: 12
    minNumberOfRepayments: 3
    maxNumberOfRepayments: 12
    repaymentEvery: 1
    repaymentFrequencyType: MONTHS
    interestRatePerPeriod: 24.0
    minInterestRatePerPeriod: 18.0
    maxInterestRatePerPeriod: 30.0
    interestRateFrequencyType: YEAR
    amortizationType: EQUAL_INSTALLMENTS
    interestType: DECLINING_BALANCE
    interestCalculationPeriodType: SAME_AS_REPAYMENT_PERIOD
    transactionProcessingStrategyId: 1
    accountingRule: ACCRUAL_PERIODIC
    charges:
      - $charge.Loan Processing Fee
      - $charge.Late Payment Penalty
    fundSources:
      - $fundSource.Own Capital
      - $fundSource.Bank Loans
    accounting:
      fundSource: $glAccount.41
      loanPortfolio: $glAccount.51
      interestReceivable: $glAccount.53
      interestIncome: $glAccount.81
      feeIncome: $glAccount.82
      penaltyIncome: $glAccount.84
      transfersInSuspense: $glAccount.122
      lossesWrittenOff: $glAccount.95
      incomeFromRecovery: $glAccount.86

savingsProducts:
  - name: Voluntary Savings Account
    shortName: VSAV
    description: General savings account
    currencyCode: XAF
    digitsAfterDecimal: 0
    nominalAnnualInterestRate: 3.0
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: MONTHLY
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365
    minRequiredOpeningBalance: 10000
    accountingRule: ACCRUAL_PERIODIC
    charges:
      - $charge.Monthly Maintenance Fee
    accounting:
      savingsReference: $glAccount.61
      savingsControl: $glAccount.61
      interestOnSavings: $glAccount.64
      incomeFromFees: $glAccount.82
      incomeFromPenalties: $glAccount.84
      transfersInSuspense: $glAccount.122
      withholdingTax: $glAccount.141

#==============================================================================
# PHASE 5: CLIENT ACCOUNTS
#==============================================================================

clients:
  - firstname: Marie
    lastname: Ngono
    externalId: CLI001
    office: $office.Douala Branch
    staff: $staff.grace.fonkou
    gender: $code.Gender.Female
    mobile: "+237677123456"
    email: marie.ngono@example.com
    dateOfBirth: "1985-05-15"
    activationDate: "2024-03-01"
    clientType: $code.Client Type.Individual
    address: "Akwa, Douala"
    city: Douala
    customFields:
      id_type: National ID
      id_number: "CM12345678"
      next_of_kin_name: "Paul Ngono"
      next_of_kin_phone: "+237677999888"

  - firstname: Joseph
    lastname: Kamga
    externalId: CLI002
    office: $office.Douala Branch
    staff: $staff.grace.fonkou
    gender: $code.Gender.Male
    mobile: "+237688234567"
    dateOfBirth: "1978-08-20"
    activationDate: "2024-03-05"
    clientType: $code.Client Type.Business

savingsAccounts:
  - client: $client.CLI001
    product: $savingsProduct.VSAV
    externalId: SAV001
    submittedOn: "2024-03-01"
    approvedOn: "2024-03-02"
    activatedOn: "2024-03-02"
    fieldOfficer: $staff.grace.fonkou
    initialDeposit: 50000

  - client: $client.CLI002
    product: $savingsProduct.VSAV
    externalId: SAV002
    submittedOn: "2024-03-05"
    approvedOn: "2024-03-06"
    activatedOn: "2024-03-06"
    fieldOfficer: $staff.grace.fonkou
    initialDeposit: 100000

loanAccounts:
  - client: $client.CLI001
    product: $loanProduct.MSOL
    externalId: LOAN001
    submittedOn: "2024-03-10"
    approvedOn: "2024-03-12"
    expectedDisbursementDate: "2024-03-15"
    disbursedOn: "2024-03-15"
    principal: 500000
    loanTermFrequency: 12
    loanTermFrequencyType: MONTHS
    numberOfRepayments: 12
    repaymentEvery: 1
    repaymentFrequencyType: MONTHS
    interestRatePerPeriod: 24.0
    amortizationType: EQUAL_INSTALLMENTS
    interestType: DECLINING_BALANCE
    loanOfficer: $staff.grace.fonkou
    fundSource: $fundSource.Own Capital

  - client: $client.CLI002
    product: $loanProduct.MSOL
    externalId: LOAN002
    submittedOn: "2024-03-20"
    approvedOn: "2024-03-22"
    expectedDisbursementDate: "2024-03-25"
    disbursedOn: "2024-03-25"
    principal: 300000
    loanTermFrequency: 6
    numberOfRepayments: 6
    repaymentEvery: 1
    repaymentFrequencyType: MONTHS
    interestRatePerPeriod: 24.0
    loanOfficer: $staff.grace.fonkou
```

---

## Phase-by-Phase Examples

### Phase 1 Only: System Configuration

```yaml
tenant: default

systemConfig:
  currency:
    code: XAF
    name: Central African CFA Franc
    decimalPlaces: 0

  workingDays:
    days: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

  globalConfig:
    - name: maker-checker
      enabled: true

  codes:
    - name: Gender
      values:
        - name: Male
        - name: Female
```

### Phase 2 Only: Security & Org

```yaml
tenant: default

roles:
  - name: Manager
    permissions: [ALL_FUNCTIONS_READ_CLIENT, ALL_FUNCTIONS_LOAN]

offices:
  - name: Main Office
    externalId: MAIN
    openingDate: "2024-01-01"

staff:
  - firstname: John
    lastname: Doe
    office: $office.Main Office
    role: $role.Manager
    username: john.doe
    createUser: true
```

---

## Advanced Features

### Variable Substitution

```yaml
systemConfig:
  globalConfig:
    # Environment variable
    - name: amazon-S3_access_key
      value: ${env:AWS_ACCESS_KEY}

    # File content
    - name: smtp_password
      value: ${file:/run/secrets/smtp_password}

    # System property
    - name: api_url
      value: ${sys:fineract.url}

    # Recursive substitution
    - name: db_password
      value: ${file:${env:PASSWORD_FILE}}

staff:
  - firstname: Admin
    lastname: User
    password: ${env:DEFAULT_PASSWORD}
```

### Multi-line Strings

```yaml
notificationTemplates:
  - name: Welcome Email
    type: EMAIL
    subject: "Welcome!"
    message: |
      Dear {{client.name}},

      Welcome to our service.

      Multiple lines are supported.

      Thank you!
```

### References

```yaml
# Reference format: $<entity-type>.<identifier>

staff:
  office: $office.Head Office          # By name
  role: $role.Branch Manager

loanProducts:
  charges:
    - $charge.Processing Fee           # By name
  accounting:
    fundSource: $glAccount.41          # By GL code
    loanPortfolio: $glAccount.51

loanAccounts:
  client: $client.CLI001               # By externalId
  product: $loanProduct.MSOL           # By shortName
  loanOfficer: $staff.john.doe         # By username
```

---

## Schema Validation

### JSON Schema

```bash
# Validate YAML against JSON schema
python3 scripts/validate_yaml.py \
  --config config.yml \
  --schema schema/fineract-config-schema.json
```

### Common Validation Errors

```yaml
# ❌ WRONG: Missing required field
currency:
  name: US Dollar

# ✅ CORRECT: All required fields
currency:
  code: USD
  name: US Dollar
  decimalPlaces: 2

# ❌ WRONG: Invalid reference
staff:
  office: Head Office  # Should be $office.Head Office

# ✅ CORRECT: Proper reference
staff:
  office: $office.Head Office

# ❌ WRONG: Invalid enum value
workingDays:
  days: [Mon, Tue, Wed]  # Should be MONDAY, TUESDAY, etc.

# ✅ CORRECT: Valid enum values
workingDays:
  days: [MONDAY, TUESDAY, WEDNESDAY]
```

---

**For complete entity field definitions, see [ENTITY_REFERENCE.md](ENTITY_REFERENCE.md)**
