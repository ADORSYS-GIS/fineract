# Fineract Config CLI - Configuration Reference

Complete reference for all 40+ supported entity types with field descriptions, examples, and API mappings.

---

## Table of Contents

### Quick Navigation
- [Overview](#overview)
- [Configuration Structure](#configuration-structure)
- [Entity Loading Order](#entity-loading-order)
- [Dependency Resolution](#dependency-resolution)

### Entity Categories

#### Phase 1: System Configuration
- [Currency Configuration](#currency-configuration)
- [Working Days](#working-days)
- [Global Configuration](#global-configuration)
- [Codes & Code Values](#codes--code-values)
- [Account Number Preferences](#account-number-preferences)
- [Notification Configuration](#notification-configuration)
- [Notification Templates](#notification-templates)
- [Data Tables](#data-tables)

#### Phase 2: Security & Organization
- [Offices](#offices)
- [Roles](#roles)
- [Users](#users)
- [Staff](#staff)

#### Phase 3: Accounting Foundation
- [Chart of Accounts (GL Accounts)](#chart-of-accounts-gl-accounts)
- [Payment Types](#payment-types)
- [Fund Sources](#fund-sources)
- [Financial Activity Mappings](#financial-activity-mappings)

#### Phase 4: Financial Products
- [Charges](#charges)
- [Loan Products](#loan-products)
- [Savings Products](#savings-products)

#### Phase 5: Client Operations & Accounts
- [Clients](#clients)
- [Groups](#groups)
- [Centers](#centers)
- [Savings Accounts](#savings-accounts)
- [Loan Accounts](#loan-accounts)

#### Phase 6: Transactions & Operations
- [Savings Transactions](#savings-transactions)
- [Loan Transactions](#loan-transactions)

### Additional Reference
- [Field Types Reference](#field-types-reference)
- [Enumeration Values](#enumeration-values)
- [Common Patterns](#common-patterns)
- [Validation Rules](#validation-rules)

---

## Overview

### Configuration File Structure

```yaml
# Top-level configuration
tenant: default  # Required: Tenant identifier

# Phase 1: System Configuration (8 entity types)
systemConfig:
  currency: {...}
  workingDays: {...}
  globalConfig: [...]
  codes: [...]
  accountNumberPreferences: [...]
  notificationConfig: {...}
  notificationTemplates: [...]
  dataTables: [...]

# Phase 2: Security & Organization (4 entity types)
offices: [...]
roles: [...]
users: [...]
staff: [...]

# Phase 3: Accounting Foundation (4 entity types)
chartOfAccounts: [...]
paymentTypes: [...]
fundSources: [...]
financialActivityMappings: [...]

# Phase 4: Financial Products (3 main types)
charges: [...]
loanProducts: [...]
savingsProducts: [...]

# Phase 5: Client Operations & Accounts (5 entity types)
clients: [...]
groups: [...]
centers: [...]
savingsAccounts: [...]
loanAccounts: [...]

# Phase 6: Transactions & Operations (2 entity types)
savingsTransactions: [...]
loanTransactions: [...]
```

### Entity Loading Order

Entities are loaded in dependency order:

**Phase 1: System Configuration**
1. Currency Configuration
2. Working Days
3. Global Configuration
4. Codes & Code Values
5. Account Number Preferences
6. Notification Configuration
7. Notification Templates
8. Data Tables

**Phase 2: Security & Organization**
1. Offices (hierarchical - parents before children)
2. Roles
3. Users (depends on: offices, roles)
4. Staff (depends on: offices)

**Phase 3: Accounting Foundation**
1. Chart of Accounts (hierarchical - parents before children)
2. Payment Types
3. Fund Sources
4. Financial Activity Mappings (depends on: GL accounts)

**Phase 4: Financial Products**
1. Charges (depends on: GL accounts)
2. Loan Products (depends on: GL accounts, charges)
3. Savings Products (depends on: GL accounts, charges)

**Phase 5: Client Operations & Accounts**
1. Clients (depends on: offices, staff)
2. Groups (depends on: offices, staff, clients)
3. Centers (depends on: offices, staff)
4. Savings Accounts (depends on: clients/groups, savings products, staff)
5. Loan Accounts (depends on: clients/groups, loan products, staff, fund sources)

**Phase 6: Transactions & Operations**
1. Savings Transactions (depends on: savings accounts, payment types)
2. Loan Transactions (depends on: loan accounts, payment types)

---

## Dependency Resolution

### Reference Syntax

Use `$<entity-type>.<identifier>` to reference other entities:

```yaml
# Reference by name
officeName: $office.Head Office

# Reference by external ID
staffExternalId: $staff.STAFF-001

# Reference by GL code
glAccountCode: $glAccount.1000

# Reference by short name
productName: $loanProduct.PL
```

### Supported Reference Types

| Entity Type | Reference Format | Example |
|-------------|-----------------|---------|
| Office | `$office.<name>` | `$office.Head Office` |
| Role | `$role.<name>` | `$role.Branch Manager` |
| User | `$user.<username>` | `$user.john.doe` |
| Staff | `$staff.<externalId>` or `$staff.<firstName> <lastName>` | `$staff.STAFF-001` or `$staff.Jane Advisor` |
| GL Account | `$glAccount.<glCode>` | `$glAccount.1000` |
| Payment Type | `$paymentType.<name>` | `$paymentType.Cash` |
| Fund Source | `$fundSource.<name>` | `$fundSource.Donor Fund` |
| Charge | `$charge.<name>` | `$charge.Processing Fee` |
| Loan Product | `$loanProduct.<shortName>` | `$loanProduct.PL` |
| Savings Product | `$savingsProduct.<shortName>` | `$savingsProduct.SS` |
| Client | `$client.<externalId>` | `$client.CLIENT-001` |
| Group | `$group.<name>` | `$group.Women's Group` |
| Center | `$center.<name>` | `$center.Community Center` |
| Code | `$code.<name>` | `$code.CustomerType` |

---

## Phase 1: System Configuration

### Currency Configuration

**Description**: Organization-wide currency settings.

**API Endpoint**: `PUT /api/v1/currencies`

**YAML Structure**:
```yaml
systemConfig:
  currency:
    code: USD              # Required: ISO 4217 currency code (3 chars)
    name: US Dollar        # Required: Currency display name
    decimalPlaces: 2       # Required: Number of decimal places (0-6)
    inMultiplesOf: 1       # Optional: Rounding multiple (default: 1)
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `code` | String(3) | Yes | - | ISO 4217 currency code (USD, EUR, XAF, etc.) |
| `name` | String(50) | Yes | - | Human-readable currency name |
| `decimalPlaces` | Integer | Yes | - | Decimal precision (0-6). 0 for XAF, 2 for USD |
| `inMultiplesOf` | Integer | No | 1 | Rounding multiple. For XAF: 5 (round to 5, 10, 15...) |

**Examples**:

```yaml
# US Dollar (2 decimal places)
systemConfig:
  currency:
    code: USD
    name: US Dollar
    decimalPlaces: 2
    inMultiplesOf: 1

# Central African CFA Franc (no decimals, round to 5)
systemConfig:
  currency:
    code: XAF
    name: Central African CFA Franc
    decimalPlaces: 0
    inMultiplesOf: 5

# Euro (2 decimal places)
systemConfig:
  currency:
    code: EUR
    name: Euro
    decimalPlaces: 2
    inMultiplesOf: 1
```

**Validation Rules**:
- Currency code must be exactly 3 characters
- Currency code must be a valid ISO 4217 code
- Decimal places must be between 0 and 6
- Cannot change currency after accounts are created

**Common Issues**:
- **Error**: "Currency code must be 3 characters" → Use standard ISO codes
- **Error**: "Cannot change currency with existing accounts" → Currency is immutable

---

### Working Days

**Description**: Business day calendar and repayment rescheduling rules.

**API Endpoint**: `PUT /api/v1/workingdays`

**YAML Structure**:
```yaml
systemConfig:
  workingDays:
    recurrence: FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
    repaymentReschedulingType: MOVE_TO_NEXT_WORKING_DAY
    extendTermForDailyRepayments: false
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `recurrence` | String | Yes | - | iCal RRULE format for working days |
| `repaymentReschedulingType` | Enum | Yes | - | How to handle repayments on non-working days |
| `extendTermForDailyRepayments` | Boolean | No | false | Extend loan term when daily repayments fall on non-working days |

**Repayment Rescheduling Types**:

| Value | Description | Example |
|-------|-------------|---------|
| `SAME_DAY` | Keep original date even if non-working day | Sat → Sat (no change) |
| `MOVE_TO_NEXT_WORKING_DAY` | Move to next working day | Sat → Mon |
| `MOVE_TO_NEXT_REPAYMENT_MEETING_DAY` | Move to next group meeting day | Sat → Next meeting |
| `MOVE_TO_PREVIOUS_WORKING_DAY` | Move to previous working day | Mon → Fri |

**Examples**:

```yaml
# Monday-Friday working days (standard)
systemConfig:
  workingDays:
    recurrence: FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
    repaymentReschedulingType: MOVE_TO_NEXT_WORKING_DAY
    extendTermForDailyRepayments: false

# Monday-Saturday working days
systemConfig:
  workingDays:
    recurrence: FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA
    repaymentReschedulingType: MOVE_TO_NEXT_WORKING_DAY
    extendTermForDailyRepayments: false

# Sunday-Thursday (Middle East)
systemConfig:
  workingDays:
    recurrence: FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH
    repaymentReschedulingType: MOVE_TO_NEXT_WORKING_DAY
    extendTermForDailyRepayments: false
```

**iCal RRULE Format**:
- `FREQ=WEEKLY` - Weekly recurrence
- `BYDAY=MO,TU,WE,TH,FR` - Days of week
  - `MO` = Monday
  - `TU` = Tuesday
  - `WE` = Wednesday
  - `TH` = Thursday
  - `FR` = Friday
  - `SA` = Saturday
  - `SU` = Sunday

---

### Global Configuration

**Description**: System-wide configuration items (boolean flags and string values).

**API Endpoint**: `PUT /api/v1/configurations/{id}`

**YAML Structure**:
```yaml
systemConfig:
  globalConfig:
    - name: maker-checker
      enabled: true
      description: Enable maker-checker workflow

    - name: amazon-S3_bucket_name
      value: fineract-documents
      enabled: true
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String | Yes | - | Configuration item name (system-defined) |
| `enabled` | Boolean | Yes* | - | Enable/disable flag (for boolean configs) |
| `value` | String | Yes* | - | Configuration value (for string configs) |
| `description` | String | No | - | Human-readable description |

*Either `enabled` or `value` is required depending on configuration type

**Common Global Configurations**:

| Name | Type | Description |
|------|------|-------------|
| `maker-checker` | Boolean | Enable maker-checker workflow for sensitive operations |
| `force-password-reset-days` | Integer | Days before password reset required |
| `enable-address` | Boolean | Enable address management for clients |
| `enable-sub-rates` | Boolean | Enable sub-rates for loan products |
| `amazon-S3_access_key` | String | AWS S3 access key for document storage |
| `amazon-S3_secret_key` | String | AWS S3 secret key |
| `amazon-S3_bucket_name` | String | S3 bucket name |
| `smtp-host` | String | SMTP server hostname |
| `smtp-port` | Integer | SMTP server port |
| `smtp-username` | String | SMTP username |
| `smtp-password` | String | SMTP password |

**Examples**:

```yaml
# Enable maker-checker workflow
systemConfig:
  globalConfig:
    - name: maker-checker
      enabled: true

# Configure S3 document storage (with variable substitution)
systemConfig:
  globalConfig:
    - name: amazon-S3_access_key
      value: ${env:AWS_ACCESS_KEY}
      enabled: true

    - name: amazon-S3_secret_key
      value: ${env:AWS_SECRET_KEY}
      enabled: true

    - name: amazon-S3_bucket_name
      value: fineract-prod-documents
      enabled: true

# Configure SMTP for email notifications
systemConfig:
  globalConfig:
    - name: smtp-host
      value: smtp.gmail.com
      enabled: true

    - name: smtp-port
      value: "587"
      enabled: true

    - name: smtp-username
      value: notifications@example.com
      enabled: true

    - name: smtp-password
      value: ${file:/run/secrets/smtp_password}
      enabled: true

    - name: smtp-from-email
      value: noreply@example.com
      enabled: true

# Force password reset every 90 days
systemConfig:
  globalConfig:
    - name: force-password-reset-days
      value: "90"
      enabled: true
```

**Security Best Practices**:
- Never hardcode passwords or secrets
- Use environment variables: `${env:VARIABLE_NAME}`
- Use file-based secrets: `${file:/path/to/secret}`
- Mark sensitive configs in comments

---

### Codes & Code Values

**Description**: Custom dropdown lists (picklists) for various entity fields.

**API Endpoints**:
- `POST /api/v1/codes` (create code)
- `POST /api/v1/codes/{codeId}/codevalues` (create code value)

**YAML Structure**:
```yaml
systemConfig:
  codes:
    - name: CustomerType
      values:
        - name: Individual
          position: 1
          isActive: true
          description: Individual customer

        - name: Business
          position: 2
          isActive: true
          description: Business/corporate customer
```

**Field Reference - Code**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Unique code name (e.g., "CustomerType") |
| `values` | Array | Yes | - | List of code values |

**Field Reference - Code Value**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Display name for the value |
| `position` | Integer | Yes | - | Sort order (1, 2, 3...) |
| `isActive` | Boolean | No | true | Active status |
| `description` | String(500) | No | - | Optional description |

**Common System Codes**:

| Code Name | Used By | Purpose |
|-----------|---------|---------|
| `CustomerType` | Clients | Individual vs Business |
| `Education` | Clients | Education level |
| `MaritalStatus` | Clients | Marital status |
| `Profession` | Clients | Occupation/profession |
| `LoanPurpose` | Loan Accounts | Purpose of loan |
| `ClientType` | Clients | Client classification |
| `Constitution` | Groups | Group legal structure |

**Examples**:

```yaml
# Customer Type (Individual vs Business)
systemConfig:
  codes:
    - name: CustomerType
      values:
        - name: Individual
          position: 1
          isActive: true
        - name: Business
          position: 2
          isActive: true
        - name: NGO
          position: 3
          isActive: true

# Education Level
systemConfig:
  codes:
    - name: Education
      values:
        - name: Primary
          position: 1
          isActive: true
        - name: Secondary
          position: 2
          isActive: true
        - name: University
          position: 3
          isActive: true
        - name: Postgraduate
          position: 4
          isActive: true

# Loan Purpose
systemConfig:
  codes:
    - name: LoanPurpose
      values:
        - name: Agriculture
          position: 1
          isActive: true
          description: Agricultural activities
        - name: Business
          position: 2
          isActive: true
          description: Business expansion or startup
        - name: Education
          position: 3
          isActive: true
          description: Educational expenses
        - name: Home Improvement
          position: 4
          isActive: true
        - name: Medical
          position: 5
          isActive: true
        - name: Wedding
          position: 6
          isActive: true

# Marital Status
systemConfig:
  codes:
    - name: MaritalStatus
      values:
        - name: Single
          position: 1
          isActive: true
        - name: Married
          position: 2
          isActive: true
        - name: Divorced
          position: 3
          isActive: true
        - name: Widowed
          position: 4
          isActive: true
```

**Usage in Entities**:

```yaml
# Reference code value in client
clients:
  - firstName: John
    lastName: Doe
    customerType: Individual      # References CustomerType code
    education: University          # References Education code
    maritalStatus: Married         # References MaritalStatus code

# Reference code value in loan account
loanAccounts:
  - externalId: LOAN-001
    clientExternalId: CLIENT-001
    productName: Personal Loan
    loanPurpose: Education        # References LoanPurpose code
```

---

### Account Number Preferences

**Description**: Configure automatic account number generation formats.

**API Endpoint**: `POST /api/v1/accountnumberformats`

**YAML Structure**:
```yaml
systemConfig:
  accountNumberPreferences:
    - accountType: CLIENT
      prefixType: OFFICE_NAME

    - accountType: LOAN
      prefixType: LOAN_PRODUCT_SHORT_NAME

    - accountType: SAVINGS
      prefixType: SAVINGS_PRODUCT_SHORT_NAME
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `accountType` | Enum | Yes | - | Type of account |
| `prefixType` | Enum | Yes | - | Prefix strategy for account numbers |

**Account Types**:

| Value | Description |
|-------|-------------|
| `CLIENT` | Client account numbers |
| `LOAN` | Loan account numbers |
| `SAVINGS` | Savings account numbers |
| `CENTER` | Center account numbers |
| `GROUP` | Group account numbers |

**Prefix Types**:

| Value | Description | Example |
|-------|-------------|---------|
| `NONE` | No prefix, sequential only | 000001, 000002 |
| `CLIENT_TYPE` | Prefix with client type code | IND000001, BUS000001 |
| `OFFICE_NAME` | Prefix with office code | HO000001, BR000001 |
| `LOAN_PRODUCT_SHORT_NAME` | Prefix with product short name | PL000001, BL000001 |
| `SAVINGS_PRODUCT_SHORT_NAME` | Prefix with product short name | SAV000001, CUR000001 |

**Examples**:

```yaml
# Client numbers prefixed by office
systemConfig:
  accountNumberPreferences:
    - accountType: CLIENT
      prefixType: OFFICE_NAME
# Results: HO000001, HO000002, BR000001, BR000002

# Loan numbers prefixed by product
systemConfig:
  accountNumberPreferences:
    - accountType: LOAN
      prefixType: LOAN_PRODUCT_SHORT_NAME
# Results: PL000001, PL000002, BL000001, BL000002

# Savings with no prefix
systemConfig:
  accountNumberPreferences:
    - accountType: SAVINGS
      prefixType: NONE
# Results: 000001, 000002, 000003

# Complete configuration
systemConfig:
  accountNumberPreferences:
    - accountType: CLIENT
      prefixType: OFFICE_NAME

    - accountType: GROUP
      prefixType: OFFICE_NAME

    - accountType: CENTER
      prefixType: OFFICE_NAME

    - accountType: LOAN
      prefixType: LOAN_PRODUCT_SHORT_NAME

    - accountType: SAVINGS
      prefixType: SAVINGS_PRODUCT_SHORT_NAME
```

**Behavior**:
- Account numbers are auto-generated at creation time
- Format: `[PREFIX][SEQUENCE]`
- Sequence is padded with zeros (6 digits by default)
- Cannot change format after accounts are created

---

### Notification Configuration

**Description**: Enable/disable SMS and email notifications globally.

**API Endpoint**: `PUT /api/v1/notification`

**YAML Structure**:
```yaml
systemConfig:
  notificationConfig:
    smsEnabled: true
    emailEnabled: true
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `smsEnabled` | Boolean | No | false | Enable SMS notifications |
| `emailEnabled` | Boolean | No | false | Enable email notifications |

**Examples**:

```yaml
# Enable both SMS and email
systemConfig:
  notificationConfig:
    smsEnabled: true
    emailEnabled: true

# Email only
systemConfig:
  notificationConfig:
    smsEnabled: false
    emailEnabled: true

# SMS only
systemConfig:
  notificationConfig:
    smsEnabled: true
    emailEnabled: false

# Disable all notifications
systemConfig:
  notificationConfig:
    smsEnabled: false
    emailEnabled: false
```

**Prerequisites**:
- For SMS: Configure SMS provider in global config
- For Email: Configure SMTP settings in global config

---

### Notification Templates

**Description**: Message templates for SMS and email notifications.

**API Endpoint**: `POST /api/v1/templates`

**YAML Structure**:
```yaml
systemConfig:
  notificationTemplates:
    - name: Loan Approved
      type: SMS
      text: "Dear {{client.displayName}}, your loan {{loan.accountNo}} for {{loan.principal}} has been approved."

    - name: Loan Approved Email
      type: EMAIL
      subject: "Loan Application Approved"
      text: "Dear {{client.displayName}},\n\nYour loan application for {{loan.principal}} has been approved.\nLoan Account: {{loan.accountNo}}"
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Unique template name |
| `type` | Enum | Yes | - | SMS or EMAIL |
| `text` | String(5000) | Yes | - | Template body with placeholders |
| `subject` | String(200) | No | - | Email subject (EMAIL only) |

**Template Types**:

| Value | Description |
|-------|-------------|
| `SMS` | SMS message template (160 chars recommended) |
| `EMAIL` | Email message template |

**Template Variables**:

| Variable | Description | Example |
|----------|-------------|---------|
| `{{client.displayName}}` | Client full name | John Doe |
| `{{client.accountNo}}` | Client account number | 000001 |
| `{{client.mobileNo}}` | Client mobile number | +237677... |
| `{{loan.accountNo}}` | Loan account number | LN000001 |
| `{{loan.principal}}` | Loan principal amount | 10000 |
| `{{loan.approvedOnDate}}` | Loan approval date | 2024-11-20 |
| `{{savings.accountNo}}` | Savings account number | SAV000001 |
| `{{savings.balance}}` | Savings balance | 5000 |
| `{{user.username}}` | Username | john.doe |
| `{{office.name}}` | Office name | Head Office |

**Examples**:

```yaml
# Loan approval SMS
systemConfig:
  notificationTemplates:
    - name: Loan Approved SMS
      type: SMS
      text: "Dear {{client.displayName}}, your loan {{loan.accountNo}} for {{loan.principal}} {{loan.currency.code}} has been approved."

# Loan approval email
systemConfig:
  notificationTemplates:
    - name: Loan Approved Email
      type: EMAIL
      subject: "Loan Application Approved - {{loan.accountNo}}"
      text: |
        Dear {{client.displayName}},

        We are pleased to inform you that your loan application has been approved.

        Loan Details:
        - Account Number: {{loan.accountNo}}
        - Principal Amount: {{loan.principal}} {{loan.currency.code}}
        - Approval Date: {{loan.approvedOnDate}}
        - Number of Repayments: {{loan.numberOfRepayments}}

        Please contact your loan officer for next steps.

        Best regards,
        {{office.name}}

# Repayment reminder SMS
systemConfig:
  notificationTemplates:
    - name: Repayment Reminder
      type: SMS
      text: "Reminder: Your loan {{loan.accountNo}} repayment of {{loan.nextRepayment.amount}} is due on {{loan.nextRepayment.dueDate}}."

# Savings deposit confirmation
systemConfig:
  notificationTemplates:
    - name: Deposit Confirmation
      type: SMS
      text: "Deposit confirmed: {{transaction.amount}} {{savings.currency.code}} deposited to {{savings.accountNo}}. New balance: {{savings.balance}}."

# Password reset email
systemConfig:
  notificationTemplates:
    - name: Password Reset
      type: EMAIL
      subject: "Password Reset Request"
      text: |
        Hello {{user.username}},

        A password reset was requested for your account.

        If you did not request this, please contact support immediately.

        To reset your password, click: {{passwordResetLink}}

        This link expires in 24 hours.
```

---

### Data Tables

**Description**: Custom field definitions for extending entity data structures.

**API Endpoint**: `POST /api/v1/datatables`

**YAML Structure**:
```yaml
systemConfig:
  dataTables:
    - registeredTableName: m_client_additional_info
      applicationTableName: m_client
      columns:
        - name: national_id
          type: String
          length: 20
          mandatory: true

        - name: tax_id
          type: String
          length: 20
          mandatory: false

        - name: annual_income
          type: Decimal
          mandatory: false
```

**Field Reference - Data Table**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `registeredTableName` | String | Yes | - | Physical table name (must start with `m_`) |
| `applicationTableName` | String | Yes | - | Entity table to extend |
| `columns` | Array | Yes | - | Custom column definitions |

**Field Reference - Column**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String | Yes | - | Column name (lowercase, underscores) |
| `type` | Enum | Yes | - | Data type |
| `length` | Integer | No | - | Max length (for String types) |
| `mandatory` | Boolean | No | false | Required field |
| `unique` | Boolean | No | false | Unique constraint |

**Application Tables** (Entity Types):

| Value | Entity |
|-------|--------|
| `m_client` | Client |
| `m_loan` | Loan Account |
| `m_savings_account` | Savings Account |
| `m_group` | Group |
| `m_center` | Center |
| `m_office` | Office |

**Column Types**:

| Type | Description | Example |
|------|-------------|---------|
| `String` | Text field | "John Doe" |
| `Number` | Integer | 42 |
| `Decimal` | Decimal number | 123.45 |
| `Date` | Date only | 2024-11-20 |
| `DateTime` | Date and time | 2024-11-20 10:30:00 |
| `Boolean` | True/false | true |
| `Text` | Long text (no length limit) | "Long description..." |
| `Dropdown` | Reference to code | References code table |

**Examples**:

```yaml
# Client additional information
systemConfig:
  dataTables:
    - registeredTableName: m_client_kyc
      applicationTableName: m_client
      columns:
        - name: national_id
          type: String
          length: 20
          mandatory: true

        - name: passport_number
          type: String
          length: 20
          mandatory: false

        - name: drivers_license
          type: String
          length: 20
          mandatory: false

        - name: annual_income
          type: Decimal
          mandatory: false

        - name: employment_status
          type: Dropdown
          code: EmploymentStatus
          mandatory: false

# Loan additional fields
systemConfig:
  dataTables:
    - registeredTableName: m_loan_collateral_details
      applicationTableName: m_loan
      columns:
        - name: collateral_type
          type: String
          length: 50
          mandatory: true

        - name: collateral_value
          type: Decimal
          mandatory: true

        - name: valuation_date
          type: Date
          mandatory: true

        - name: appraiser_name
          type: String
          length: 100
          mandatory: false

# Savings account custom fields
systemConfig:
  dataTables:
    - registeredTableName: m_savings_account_preferences
      applicationTableName: m_savings_account
      columns:
        - name: statement_frequency
          type: Dropdown
          code: StatementFrequency
          mandatory: false

        - name: preferred_channel
          type: Dropdown
          code: CommunicationChannel
          mandatory: false

        - name: auto_transfer_enabled
          type: Boolean
          mandatory: false
```

**Usage**:
- Custom fields appear in entity forms
- Accessible via API: `/api/v1/datatables/{tableName}/{entityId}`
- Can be made mandatory or optional
- Dropdown types reference code tables

---

## Phase 2: Security & Organization

### Offices

**Description**: Organizational units in a hierarchical structure.

**API Endpoint**: `POST /api/v1/offices`

**YAML Structure**:
```yaml
offices:
  - name: Head Office
    externalId: HO001
    openingDate: [2020, 1, 1]

  - name: Yaoundé Branch
    externalId: YAO001
    openingDate: [2020, 6, 1]
    parentName: Head Office
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Unique office name |
| `externalId` | String(100) | No | - | External system identifier |
| `openingDate` | Date Array | Yes | - | Office opening date [year, month, day] |
| `parentName` | String | No | - | Parent office name (for hierarchy) |

**Date Format**: `[year, month, day]`
- Example: `[2024, 11, 20]` = November 20, 2024
- Month is 1-based (1 = January, 12 = December)

**Hierarchical Structure**:
- Root office has no parent
- Child offices reference parent by name
- Unlimited depth supported
- Parent must exist before child

**Examples**:

```yaml
# Single office (root)
offices:
  - name: Head Office
    externalId: HO001
    openingDate: [2020, 1, 1]

# Two-level hierarchy
offices:
  - name: Head Office
    externalId: HO001
    openingDate: [2020, 1, 1]

  - name: Yaoundé Branch
    externalId: YAO001
    openingDate: [2020, 6, 1]
    parentName: Head Office

  - name: Douala Branch
    externalId: DLA001
    openingDate: [2020, 6, 1]
    parentName: Head Office

# Three-level hierarchy
offices:
  - name: Head Office
    externalId: HO001
    openingDate: [2020, 1, 1]

  - name: Yaoundé Branch
    externalId: YAO001
    openingDate: [2020, 6, 1]
    parentName: Head Office

  - name: Yaoundé North Sub-Branch
    externalId: YAO-N001
    openingDate: [2021, 1, 1]
    parentName: Yaoundé Branch

  - name: Yaoundé South Sub-Branch
    externalId: YAO-S001
    openingDate: [2021, 1, 1]
    parentName: Yaoundé Branch
```

**Usage in Other Entities**:

```yaml
# Reference in staff
staff:
  - firstName: Jane
    lastName: Doe
    officeName: Yaoundé Branch  # Must match office name exactly

# Reference in users
users:
  - username: john.doe
    officeName: Head Office

# Reference in clients
clients:
  - firstName: Marie
    lastName: Ngono
    officeName: Yaoundé Branch
```

**Immutable Fields**:
- `openingDate` - Cannot be changed after creation
- Hierarchy changes require special handling

---

### Roles

**Description**: User roles with permission assignments.

**API Endpoint**: `POST /api/v1/roles`

**YAML Structure**:
```yaml
roles:
  - name: Branch Manager
    description: Full branch operations access
    disabled: false
    permissions:
      - READ_OFFICE
      - CREATE_CLIENT
      - READ_CLIENT
      - UPDATE_CLIENT
      - DELETE_CLIENT
      - CREATE_LOAN
      - READ_LOAN
      - APPROVE_LOAN
      - DISBURSE_LOAN
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Unique role name |
| `description` | String(500) | No | - | Role description |
| `disabled` | Boolean | No | false | Disable role |
| `permissions` | Array | Yes | - | List of permission codes |

**Common Permission Codes**:

**General**:
- `ALL_FUNCTIONS` - All permissions (super admin)
- `ALL_FUNCTIONS_READ` - Read-only access to everything

**Client Operations**:
- `CREATE_CLIENT`
- `READ_CLIENT`
- `UPDATE_CLIENT`
- `DELETE_CLIENT`
- `ACTIVATE_CLIENT`
- `CLOSE_CLIENT`
- `REJECT_CLIENT`
- `WITHDRAW_CLIENT`

**Loan Operations**:
- `CREATE_LOAN`
- `READ_LOAN`
- `UPDATE_LOAN`
- `DELETE_LOAN`
- `APPROVE_LOAN`
- `DISBURSE_LOAN`
- `REPAY_LOAN`
- `ADJUSTTRANSACTION_LOAN`
- `WRITEOFF_LOAN`
- `CLOSE_LOAN`

**Savings Operations**:
- `CREATE_SAVINGSACCOUNT`
- `READ_SAVINGSACCOUNT`
- `UPDATE_SAVINGSACCOUNT`
- `DELETE_SAVINGSACCOUNT`
- `APPROVE_SAVINGSACCOUNT`
- `ACTIVATE_SAVINGSACCOUNT`
- `DEPOSIT_SAVINGSACCOUNT`
- `WITHDRAWAL_SAVINGSACCOUNT`

**Configuration**:
- `CREATE_OFFICE`
- `READ_OFFICE`
- `UPDATE_OFFICE`
- `CREATE_USER`
- `READ_USER`
- `UPDATE_USER`
- `DELETE_USER`
- `CREATE_ROLE`
- `READ_ROLE`
- `UPDATE_ROLE`
- `DELETE_ROLE`

**Accounting**:
- `CREATE_GLACCOUNT`
- `READ_GLACCOUNT`
- `UPDATE_GLACCOUNT`
- `DELETE_GLACCOUNT`
- `CREATE_GLJOURNAL ENTRY`
- `READ_GLJOURNAL ENTRY`

**Products**:
- `CREATE_LOANPRODUCT`
- `READ_LOANPRODUCT`
- `UPDATE_LOANPRODUCT`
- `CREATE_SAVINGSPRODUCT`
- `READ_SAVINGSPRODUCT`
- `UPDATE_SAVINGSPRODUCT`

**Examples**:

```yaml
# Branch Manager (full branch access)
roles:
  - name: Branch Manager
    description: Branch manager with full access to branch operations
    permissions:
      - READ_OFFICE
      - CREATE_CLIENT
      - READ_CLIENT
      - UPDATE_CLIENT
      - ACTIVATE_CLIENT
      - CREATE_LOAN
      - READ_LOAN
      - APPROVE_LOAN
      - DISBURSE_LOAN
      - REPAY_LOAN
      - CREATE_SAVINGSACCOUNT
      - READ_SAVINGSACCOUNT
      - APPROVE_SAVINGSACCOUNT
      - DEPOSIT_SAVINGSACCOUNT
      - WITHDRAWAL_SAVINGSACCOUNT

# Loan Officer (client and loan management)
roles:
  - name: Loan Officer
    description: Loan officer with client and loan management access
    permissions:
      - READ_OFFICE
      - CREATE_CLIENT
      - READ_CLIENT
      - UPDATE_CLIENT
      - CREATE_LOAN
      - READ_LOAN
      - REPAY_LOAN
      - CREATE_SAVINGSACCOUNT
      - READ_SAVINGSACCOUNT
      - DEPOSIT_SAVINGSACCOUNT

# Cashier (transaction processing only)
roles:
  - name: Cashier
    description: Cashier with transaction processing access
    permissions:
      - READ_CLIENT
      - REPAY_LOAN
      - DEPOSIT_SAVINGSACCOUNT
      - WITHDRAWAL_SAVINGSACCOUNT

# Accountant (read-only for reporting)
roles:
  - name: Accountant
    description: Accountant with read-only access for reporting
    disabled: false
    permissions:
      - READ_OFFICE
      - READ_CLIENT
      - READ_LOAN
      - READ_SAVINGSACCOUNT
      - READ_GLJOURNAL ENTRY
      - READ_GLACCOUNT

# System Administrator (full access)
roles:
  - name: System Administrator
    description: Full system access including configuration
    permissions:
      - ALL_FUNCTIONS
```

**Permission Hierarchy**:
- `ALL_FUNCTIONS` grants all permissions
- `ALL_FUNCTIONS_READ` grants all read permissions
- Specific permissions can be mixed

---

### Users

**Description**: User accounts for system access.

**API Endpoint**: `POST /api/v1/users`

**YAML Structure**:
```yaml
users:
  - username: john.doe
    firstname: John
    lastname: Doe
    email: john.doe@example.com
    password: SecurePass123!
    passwordNeverExpires: false
    officeName: Head Office
    roles:
      - Branch Manager
      - System Administrator
    isSelfServiceUser: false
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `username` | String(100) | Yes | - | Unique username (immutable) |
| `firstname` | String(100) | Yes | - | First name |
| `lastname` | String(100) | Yes | - | Last name |
| `email` | String(100) | Yes | - | Email address |
| `password` | String | Yes | - | Password (min 8 chars, mixed case, numbers, symbols) |
| `passwordNeverExpires` | Boolean | No | false | Disable password expiry |
| `officeName` | String | No | - | Assigned office name |
| `roles` | Array | Yes | - | List of role names |
| `isSelfServiceUser` | Boolean | No | false | Self-service user flag |
| `staffName` | String | No | - | Linked staff member name |

**Password Requirements**:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character
- Cannot contain username

**Examples**:

```yaml
# System administrator
users:
  - username: admin
    firstname: System
    lastname: Administrator
    email: admin@example.com
    password: ${env:ADMIN_PASSWORD}  # From environment
    passwordNeverExpires: true
    officeName: Head Office
    roles:
      - System Administrator
    isSelfServiceUser: false

# Branch manager
users:
  - username: marie.kamga
    firstname: Marie
    lastname: Kamga
    email: marie.kamga@example.com
    password: BranchMgr2024!
    passwordNeverExpires: false
    officeName: Yaoundé Branch
    roles:
      - Branch Manager
    isSelfServiceUser: false

# Loan officer (linked to staff)
users:
  - username: paul.mbida
    firstname: Paul
    lastname: Mbida
    email: paul.mbida@example.com
    password: LoanOff2024!
    passwordNeverExpires: false
    officeName: Yaoundé Branch
    staffName: Paul Mbida  # Links to staff record
    roles:
      - Loan Officer
    isSelfServiceUser: false

# Cashier
users:
  - username: alice.ngo
    firstname: Alice
    lastname: Ngo
    email: alice.ngo@example.com
    password: Cashier2024!
    passwordNeverExpires: false
    officeName: Douala Branch
    roles:
      - Cashier
    isSelfServiceUser: false

# Self-service user (for client portal)
users:
  - username: client.portal
    firstname: Client
    lastname: Portal
    email: clients@example.com
    password: ClientPortal2024!
    passwordNeverExpires: true
    roles:
      - Self Service User
    isSelfServiceUser: true
```

**Security Best Practices**:
```yaml
# ❌ BAD - Hardcoded password
users:
  - username: admin
    password: admin123

# ✅ GOOD - Environment variable
users:
  - username: admin
    password: ${env:ADMIN_PASSWORD}

# ✅ GOOD - File-based secret
users:
  - username: admin
    password: ${file:/run/secrets/admin_password}
```

**Password Export Behavior**:
- Exported passwords are sanitized: `***EXPORTED***`
- Must be replaced before importing to another system
- Never commit passwords to version control

**Immutable Fields**:
- `username` - Cannot be changed after creation

---

### Staff

**Description**: Staff members (loan officers, managers, etc.).

**API Endpoint**: `POST /api/v1/staff`

**YAML Structure**:
```yaml
staff:
  - externalId: STAFF-001
    officeName: Head Office
    firstName: Jane
    lastName: Advisor
    isLoanOfficer: true
    isActive: true
    joiningDate: [2023, 1, 15]
    mobileNo: "+237670000001"
    emailAddress: jane.advisor@example.com
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `externalId` | String(100) | No | - | External system identifier |
| `officeName` | String | Yes | - | Assigned office name |
| `firstName` | String(50) | Yes | - | First name |
| `lastName` | String(50) | Yes | - | Last name |
| `isLoanOfficer` | Boolean | No | false | Can be assigned to loans/clients |
| `isActive` | Boolean | No | true | Active status |
| `joiningDate` | Date Array | No | - | Date joined [year, month, day] |
| `mobileNo` | String(50) | No | - | Mobile phone number |
| `emailAddress` | String(100) | No | - | Email address |

**Examples**:

```yaml
# Loan officer
staff:
  - externalId: STAFF-001
    officeName: Head Office
    firstName: Jane
    lastName: Advisor
    isLoanOfficer: true
    isActive: true
    joiningDate: [2023, 1, 15]
    mobileNo: "+237670000001"
    emailAddress: jane.advisor@example.com

# Branch staff (non-loan officer)
staff:
  - externalId: STAFF-002
    officeName: Yaoundé Branch
    firstName: Pierre
    lastName: Lending
    isLoanOfficer: true
    isActive: true
    joiningDate: [2023, 3, 1]
    mobileNo: "+237670000002"
    emailAddress: pierre.lending@example.com

# Inactive staff member
staff:
  - externalId: STAFF-003
    officeName: Douala Branch
    firstName: Sophie
    lastName: Credit
    isLoanOfficer: true
    isActive: false  # Left organization
    joiningDate: [2023, 6, 15]
    mobileNo: "+237670000003"
    emailAddress: sophie.credit@example.com

# Support staff (not loan officer)
staff:
  - externalId: STAFF-004
    officeName: Head Office
    firstName: Emmanuel
    lastName: Support
    isLoanOfficer: false  # Administrative staff
    isActive: true
    joiningDate: [2024, 1, 1]
    mobileNo: "+237670000004"
    emailAddress: emmanuel.support@example.com
```

**Usage in Other Entities**:

```yaml
# Assign loan officer to client
clients:
  - firstName: Marie
    lastName: Ngono
    officeName: Yaoundé Branch
    staffName: Pierre Lending  # References staff by full name

# Assign loan officer to loan account
loanAccounts:
  - externalId: LOAN-001
    clientExternalId: CLIENT-001
    productName: Personal Loan
    loanOfficerName: Jane Advisor  # References staff

# Assign to group
groups:
  - name: Women's Group
    officeName: Yaoundé Branch
    staffName: Pierre Lending
```

**Immutable Fields**:
- `externalId` - Cannot be changed after creation
- `joiningDate` - May be immutable depending on Fineract version

**Update Support**: ✅ Full update support
- All fields except immutable ones can be updated
- Active/inactive status can be toggled
- Office transfers supported

---

## Phase 3: Accounting Foundation

### Chart of Accounts (GL Accounts)

**Description**: General Ledger accounts in a hierarchical structure.

**API Endpoint**: `POST /api/v1/glaccounts`

**YAML Structure**:
```yaml
chartOfAccounts:
  - name: Assets
    glCode: "10000"
    type: ASSET
    usage: HEADER
    manualEntriesAllowed: false

  - name: Cash in Hand
    glCode: "10101"
    type: ASSET
    usage: DETAIL
    parentGlCode: "10000"
    manualEntriesAllowed: true
    description: Cash held at teller counters
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(200) | Yes | - | Account name |
| `glCode` | String(100) | Yes | - | Unique GL code (typically numeric) |
| `type` | Enum | Yes | - | Account type (ASSET, LIABILITY, etc.) |
| `usage` | Enum | Yes | - | HEADER or DETAIL |
| `parentGlCode` | String | No | - | Parent account GL code (for hierarchy) |
| `manualEntriesAllowed` | Boolean | No | true | Allow manual journal entries |
| `description` | String(500) | No | - | Account description |
| `tagName` | String | No | - | Account tag/classification |
| `disabled` | Boolean | No | false | Disable account |

**Account Types**:

| Type | Description | Normal Balance |
|------|-------------|----------------|
| `ASSET` | Assets (what you own) | Debit |
| `LIABILITY` | Liabilities (what you owe) | Credit |
| `EQUITY` | Owner's equity/capital | Credit |
| `INCOME` | Revenue/income | Credit |
| `EXPENSE` | Expenses/costs | Debit |

**Account Usage**:

| Usage | Description | Manual Entries |
|-------|-------------|----------------|
| `HEADER` | Summary/parent account | No |
| `DETAIL` | Leaf/transaction account | Yes (if allowed) |

**Account Structure**:
- Root accounts have no parent (type-level headers)
- Child accounts reference parent by GL code
- HEADER accounts cannot have transactions
- DETAIL accounts can have transactions

**Typical Chart of Accounts**:

```yaml
chartOfAccounts:
  # ASSETS
  - name: Assets
    glCode: "10000"
    type: ASSET
    usage: HEADER
    manualEntriesAllowed: false

  - name: Current Assets
    glCode: "10100"
    type: ASSET
    usage: HEADER
    parentGlCode: "10000"
    manualEntriesAllowed: false

  - name: Cash in Hand
    glCode: "10101"
    type: ASSET
    usage: DETAIL
    parentGlCode: "10100"
    manualEntriesAllowed: true
    description: Cash held at teller counters

  - name: Cash at Bank
    glCode: "10102"
    type: ASSET
    usage: DETAIL
    parentGlCode: "10100"
    manualEntriesAllowed: true
    description: Bank account balances

  - name: Loan Portfolio
    glCode: "10200"
    type: ASSET
    usage: HEADER
    parentGlCode: "10000"
    manualEntriesAllowed: false

  - name: Loans Outstanding
    glCode: "10201"
    type: ASSET
    usage: DETAIL
    parentGlCode: "10200"
    manualEntriesAllowed: false
    description: Outstanding loan principal

  - name: Interest Receivable
    glCode: "10202"
    type: ASSET
    usage: DETAIL
    parentGlCode: "10200"
    manualEntriesAllowed: false
    description: Accrued interest income

  # LIABILITIES
  - name: Liabilities
    glCode: "20000"
    type: LIABILITY
    usage: HEADER
    manualEntriesAllowed: false

  - name: Savings Deposits
    glCode: "20101"
    type: LIABILITY
    usage: DETAIL
    parentGlCode: "20000"
    manualEntriesAllowed: false
    description: Customer savings balances

  - name: Fixed Deposits
    glCode: "20102"
    type: LIABILITY
    usage: DETAIL
    parentGlCode: "20000"
    manualEntriesAllowed: false

  # EQUITY
  - name: Equity
    glCode: "30000"
    type: EQUITY
    usage: HEADER
    manualEntriesAllowed: false

  - name: Share Capital
    glCode: "30101"
    type: EQUITY
    usage: DETAIL
    parentGlCode: "30000"
    manualEntriesAllowed: true
    description: Paid-in capital

  - name: Retained Earnings
    glCode: "30201"
    type: EQUITY
    usage: DETAIL
    parentGlCode: "30000"
    manualEntriesAllowed: true

  # INCOME
  - name: Income
    glCode: "40000"
    type: INCOME
    usage: HEADER
    manualEntriesAllowed: false

  - name: Interest Income
    glCode: "40101"
    type: INCOME
    usage: DETAIL
    parentGlCode: "40000"
    manualEntriesAllowed: false
    description: Interest earned on loans

  - name: Fee Income
    glCode: "40201"
    type: INCOME
    usage: DETAIL
    parentGlCode: "40000"
    manualEntriesAllowed: false
    description: Fees charged to customers

  # EXPENSES
  - name: Expenses
    glCode: "50000"
    type: EXPENSE
    usage: HEADER
    manualEntriesAllowed: false

  - name: Interest Expense
    glCode: "50101"
    type: EXPENSE
    usage: DETAIL
    parentGlCode: "50000"
    manualEntriesAllowed: false
    description: Interest paid on deposits

  - name: Operating Expenses
    glCode: "50201"
    type: EXPENSE
    usage: DETAIL
    parentGlCode: "50000"
    manualEntriesAllowed: true
    description: General operating expenses
```

**Usage in Products**:

```yaml
# Reference in loan product accounting
loanProducts:
  - name: Personal Loan
    shortName: PL
    accounting:
      fundSourceAccountCode: $glAccount.10102  # Cash at Bank
      loanPortfolioAccountCode: $glAccount.10201  # Loans Outstanding
      interestOnLoansAccountCode: $glAccount.40101  # Interest Income
      incomeFromFeesAccountCode: $glAccount.40201  # Fee Income
```

**Immutable Fields**:
- `glCode` - Cannot be changed after creation
- `type` - Cannot be changed after creation
- `usage` - Cannot be changed after transactions exist

---

### Payment Types

**Description**: Payment methods for transactions (cash, transfer, cheque, etc.).

**API Endpoint**: `POST /api/v1/paymenttypes`

**YAML Structure**:
```yaml
paymentTypes:
  - name: Cash
    description: Cash payment
    isCashPayment: true
    position: 1

  - name: Bank Transfer
    description: Electronic bank transfer
    isCashPayment: false
    position: 2
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Unique payment type name |
| `description` | String(500) | No | - | Description |
| `isCashPayment` | Boolean | No | false | Is this cash? |
| `position` | Integer | No | - | Sort order |

**Examples**:

```yaml
paymentTypes:
  # Cash payment
  - name: Cash
    description: Cash payment at branch
    isCashPayment: true
    position: 1

  # Electronic transfer
  - name: Bank Transfer
    description: Electronic bank transfer
    isCashPayment: false
    position: 2

  # Cheque
  - name: Cheque
    description: Cheque payment
    isCashPayment: false
    position: 3

  # Mobile money
  - name: Mobile Money
    description: Mobile money transfer (MTN, Orange)
    isCashPayment: false
    position: 4

  # ATM
  - name: ATM
    description: ATM withdrawal
    isCashPayment: false
    position: 5

  # Card payment
  - name: Debit Card
    description: Debit card payment
    isCashPayment: false
    position: 6

  # Online payment
  - name: Online Payment
    description: Online payment gateway
    isCashPayment: false
    position: 7
```

**Usage in Transactions**:

```yaml
# Savings transaction
savingsTransactions:
  - savingsAccountExternalId: SAV-001
    transactionType: DEPOSIT
    transactionDate: [2024, 11, 20]
    amount: 1000.00
    paymentTypeName: Cash  # References payment type

# Loan transaction
loanTransactions:
  - loanAccountExternalId: LOAN-001
    transactionType: REPAYMENT
    transactionDate: [2024, 11, 20]
    amount: 500.00
    paymentTypeName: Mobile Money  # References payment type
```

**isCashPayment Flag**:
- `true`: Affects cash balances, teller cash drawer
- `false`: Non-cash payment (bank, electronic, etc.)

---

### Fund Sources

**Description**: Sources of funding for loans (donors, banks, internal funds).

**API Endpoint**: `POST /api/v1/funds`

**YAML Structure**:
```yaml
fundSources:
  - name: Internal Funds
    externalId: FUND-001

  - name: Donor Fund A
    externalId: FUND-DONOR-A

  - name: World Bank Loan
    externalId: FUND-WB-001
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(255) | Yes | - | Unique fund source name |
| `externalId` | String(100) | No | - | External identifier |

**Examples**:

```yaml
fundSources:
  # Internal operating funds
  - name: Internal Funds
    externalId: FUND-INT-001

  # Donor funding
  - name: USAID Grant
    externalId: FUND-USAID-2024

  - name: European Commission Grant
    externalId: FUND-EC-2024

  # Bank loans
  - name: World Bank Loan
    externalId: FUND-WB-001

  - name: African Development Bank
    externalId: FUND-ADB-001

  # Special purpose funds
  - name: Agricultural Fund
    externalId: FUND-AGR-001

  - name: Women's Empowerment Fund
    externalId: FUND-WEF-001
```

**Usage in Loan Products**:

```yaml
loanProducts:
  - name: Microcredit Loan
    shortName: MCL
    fundSourceName: USAID Grant  # Default fund source for this product

# Usage in loan accounts
loanAccounts:
  - externalId: LOAN-001
    clientExternalId: CLIENT-001
    productName: Microcredit Loan
    fundSourceName: USAID Grant  # Can override product default
    principal: 5000
```

---

### Financial Activity Mappings

**Description**: Map system financial activities to GL accounts.

**API Endpoint**: `POST /api/v1/financialactivityaccounts`

**YAML Structure**:
```yaml
financialActivityMappings:
  - financialActivityName: ASSET_FUND_SOURCE
    glAccountCode: $glAccount.10102

  - financialActivityName: CASH_AT_MAINVAULT
    glAccountCode: $glAccount.10101
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `financialActivityName` | Enum | Yes | - | System financial activity |
| `glAccountCode` | String | Yes | - | GL account code (or reference) |

**Financial Activity Types**:

| Activity | Description | Typical GL Account |
|----------|-------------|-------------------|
| `ASSET_FUND_SOURCE` | Fund source asset | Cash at Bank |
| `ASSET_TRANSFER` | Asset transfers | Cash Transfer |
| `LIABILITY_TRANSFER` | Liability transfers | Liability Transfer |
| `CASH_AT_MAINVAULT` | Main vault cash | Cash in Hand |
| `CASH_AT_TELLER` | Teller cash drawer | Teller Cash |
| `OPENING_BALANCES_TRANSFER_CONTRA` | Opening balances contra | Equity account |

**Examples**:

```yaml
financialActivityMappings:
  # Fund source mapping
  - financialActivityName: ASSET_FUND_SOURCE
    glAccountCode: "10102"  # Cash at Bank

  # Main vault cash
  - financialActivityName: CASH_AT_MAINVAULT
    glAccountCode: "10101"  # Cash in Hand

  # Teller cash
  - financialActivityName: CASH_AT_TELLER
    glAccountCode: "10103"  # Teller Cash

  # Asset transfers
  - financialActivityName: ASSET_TRANSFER
    glAccountCode: "10104"  # Asset Transfer Account

  # Opening balances
  - financialActivityName: OPENING_BALANCES_TRANSFER_CONTRA
    glAccountCode: "30201"  # Retained Earnings

# Using GL account references
financialActivityMappings:
  - financialActivityName: ASSET_FUND_SOURCE
    glAccountCode: $glAccount.Cash at Bank

  - financialActivityName: CASH_AT_MAINVAULT
    glAccountCode: $glAccount.Cash in Hand
```

**Purpose**:
- Maps system operations to accounting entries
- Required for cash management
- Required for proper accounting of fund sources
- Affects automatic journal entry generation

---

# Phase 4: Financial Products

Financial products define the terms and conditions for loans and savings accounts.

## Charges

**Description**: Fees and penalties that can be applied to loans, savings accounts, clients, or shares.

**API Endpoint**: `POST /api/v1/charges`

**YAML Structure**:

```yaml
charges:
  - name: Loan Processing Fee          # Required: Charge name
    currencyCode: XAF                  # Required: ISO currency code
    chargeAppliesTo: LOAN              # Required: LOAN, SAVINGS, CLIENT, SHARES
    chargeTimeType: DISBURSEMENT       # Required: When charge applies
    chargeCalculationType: FLAT        # Required: How charge is calculated
    amount: 5000                       # Required: Charge amount
    chargePaymentMode: REGULAR         # Optional: REGULAR, ACCOUNT_TRANSFER
    active: true                       # Optional: Active status (default: true)
    penalty: false                     # Optional: Is penalty (default: false)
    incomeAccountCode: "40101"         # Required: GL account for income
    feeFrequency: 1                    # Optional: For recurring charges
    feeInterval: MONTHS                # Optional: DAYS, WEEKS, MONTHS, YEARS
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Charge display name |
| `currencyCode` | String(3) | Yes | - | ISO 4217 currency code |
| `chargeAppliesTo` | Enum | Yes | - | Entity type: LOAN, SAVINGS, CLIENT, SHARES |
| `chargeTimeType` | Enum | Yes | - | When charge applies (see table below) |
| `chargeCalculationType` | Enum | Yes | - | Calculation method (see table below) |
| `amount` | BigDecimal | Yes | - | Charge amount (or percentage if PERCENT type) |
| `chargePaymentMode` | Enum | No | REGULAR | REGULAR (paid by client) or ACCOUNT_TRANSFER (auto-deducted) |
| `active` | Boolean | No | true | Is charge currently active |
| `penalty` | Boolean | No | false | true = penalty, false = fee |
| `incomeAccountCode` | String | Yes | - | GL account code for income (or reference) |
| `feeFrequency` | Integer | No | - | For recurring charges (e.g., monthly = 1) |
| `feeInterval` | Enum | No | - | Time unit: DAYS, WEEKS, MONTHS, YEARS |

**Charge Applies To**:

| Value | Description | Used With |
|-------|-------------|-----------|
| `LOAN` | Loan charges | Loan products, loan accounts |
| `SAVINGS` | Savings charges | Savings products, savings accounts |
| `CLIENT` | Client charges | Clients (annual fee, etc.) |
| `SHARES` | Share charges | Share products |

**Charge Time Type (for Loans)**:

| Value | Description | When Applied |
|-------|-------------|--------------|
| `DISBURSEMENT` | At loan disbursement | When loan is disbursed |
| `SPECIFIED_DUE_DATE` | On specific date | Date specified when adding charge |
| `INSTALMENT_FEE` | Per installment | Each repayment due date |
| `OVERDUE_INSTALLMENT` | Late payment penalty | When installment is overdue |
| `TRANCHE_DISBURSEMENT` | Per tranche | Multi-disbursement loans |
| `OVERDUE_ON_MATURITY` | Maturity penalty | When loan matures unpaid |

**Charge Time Type (for Savings)**:

| Value | Description | When Applied |
|-------|-------------|--------------|
| `SPECIFIED_DUE_DATE` | On specific date | Date specified when adding charge |
| `SAVINGS_ACTIVATION` | Account activation | When account is activated |
| `WITHDRAWAL_FEE` | Per withdrawal | Each withdrawal transaction |
| `ANNUAL_FEE` | Yearly | Annually on activation anniversary |
| `MONTHLY_FEE` | Monthly | Monthly on activation day |
| `QUARTERLY_FEE` | Quarterly | Every 3 months |
| `WEEKLY_FEE` | Weekly | Every 7 days |

**Charge Calculation Type**:

| Value | Description | Example |
|-------|-------------|---------|
| `FLAT` | Fixed amount | 5,000 XAF flat fee |
| `PERCENT_OF_AMOUNT` | % of principal/balance | 2% of loan amount |
| `PERCENT_OF_AMOUNT_AND_INTEREST` | % of principal + interest | 2% of (principal + interest) |
| `PERCENT_OF_INTEREST` | % of interest only | 10% of interest amount |
| `PERCENT_OF_DISBURSEMENT_AMOUNT` | % of disbursement | 1% of disbursed amount |

**Examples**:

```yaml
# Example 1: Flat loan processing fee
charges:
  - name: Loan Processing Fee
    currencyCode: XAF
    chargeAppliesTo: LOAN
    chargeTimeType: DISBURSEMENT
    chargeCalculationType: FLAT
    amount: 5000
    active: true
    penalty: false
    incomeAccountCode: "40101"  # Fee Income

# Example 2: Percentage-based origination fee
charges:
  - name: Loan Origination Fee
    currencyCode: XAF
    chargeAppliesTo: LOAN
    chargeTimeType: DISBURSEMENT
    chargeCalculationType: PERCENT_OF_AMOUNT
    amount: 2.0                  # 2% of loan amount
    active: true
    penalty: false
    incomeAccountCode: "40101"

# Example 3: Late payment penalty
charges:
  - name: Late Payment Penalty
    currencyCode: XAF
    chargeAppliesTo: LOAN
    chargeTimeType: OVERDUE_INSTALLMENT
    chargeCalculationType: PERCENT_OF_AMOUNT
    amount: 5.0                  # 5% penalty on overdue amount
    active: true
    penalty: true                # This is a penalty
    incomeAccountCode: "40102"   # Penalty Income

# Example 4: Monthly savings account fee
charges:
  - name: Monthly Account Fee
    currencyCode: XAF
    chargeAppliesTo: SAVINGS
    chargeTimeType: MONTHLY_FEE
    chargeCalculationType: FLAT
    amount: 500
    active: true
    penalty: false
    incomeAccountCode: "40103"   # Service Charge Income
    feeFrequency: 1
    feeInterval: MONTHS

# Example 5: Withdrawal fee with account transfer
charges:
  - name: ATM Withdrawal Fee
    currencyCode: XAF
    chargeAppliesTo: SAVINGS
    chargeTimeType: WITHDRAWAL_FEE
    chargeCalculationType: FLAT
    amount: 200
    chargePaymentMode: ACCOUNT_TRANSFER  # Auto-deducted from account
    active: true
    penalty: false
    incomeAccountCode: "40103"

# Example 6: Using GL account reference
charges:
  - name: Processing Fee
    currencyCode: XAF
    chargeAppliesTo: LOAN
    chargeTimeType: DISBURSEMENT
    chargeCalculationType: FLAT
    amount: 10000
    active: true
    penalty: false
    incomeAccountCode: $glAccount.Fee Income
```

**Usage in Other Entities**:

Charges are referenced by name in:
- Loan products: `chargeNames: ["Loan Processing Fee", "Late Payment Penalty"]`
- Savings products: `chargeNames: ["Monthly Account Fee", "ATM Withdrawal Fee"]`

**Validation Rules**:
- Charge name must be unique
- Currency must match product currency when assigned
- For percentage charges, amount should be 0-100
- For flat charges, amount must be positive
- Income account must be of type INCOME
- Recurring charges (MONTHLY_FEE, etc.) require feeFrequency and feeInterval

**Immutable Fields**: `name`, `currencyCode`, `chargeAppliesTo`

**Update Support**: Yes (via ChargeLoader with Pattern B)

**Common Issues**:
- **Currency mismatch**: Charge currency must match loan/savings product currency
- **Wrong account type**: Income account must be INCOME type, not ASSET or LIABILITY
- **Invalid percentage**: For PERCENT types, amount should be 0-100, not 0-1
- **Missing fee interval**: Recurring charges require both feeFrequency and feeInterval

---

## Loan Products

**Description**: Defines loan product terms, interest rates, repayment schedule, charges, and accounting rules.

**API Endpoint**: `POST /api/v1/loanproducts`

**YAML Structure**:

```yaml
loanProducts:
  - name: Personal Loan                      # Required: Product name
    shortName: PL                            # Required: Abbreviation
    description: Personal loan up to 5M      # Optional: Description
    fundSourceName: Bank Loan Fund           # Optional: Fund source reference
    currencyCode: XAF                        # Required: ISO currency code
    digitsAfterDecimal: 0                    # Required: Decimal places

    # Principal limits
    principal: 1000000                       # Required: Default principal
    minPrincipal: 100000                     # Optional: Minimum allowed
    maxPrincipal: 5000000                    # Optional: Maximum allowed

    # Loan term limits
    numberOfRepayments: 12                   # Required: Default term
    minNumberOfRepayments: 3                 # Optional: Minimum term
    maxNumberOfRepayments: 24                # Optional: Maximum term
    repaymentFrequencyType: MONTHS           # Required: DAYS, WEEKS, MONTHS, YEARS
    repaymentEvery: 1                        # Required: Frequency (e.g., every 1 month)

    # Interest configuration
    interestRatePerPeriod: 2.0               # Required: Interest rate
    minInterestRatePerPeriod: 1.5            # Optional: Min rate
    maxInterestRatePerPeriod: 3.0            # Optional: Max rate
    interestRateFrequencyType: MONTH         # Required: MONTH, YEAR
    interestType: DECLINING_BALANCE          # Required: FLAT, DECLINING_BALANCE
    interestCalculationPeriodType: SAME_AS_REPAYMENT_PERIOD  # Required
    amortizationType: EQUAL_INSTALLMENTS     # Required: EQUAL_INSTALLMENTS, EQUAL_PRINCIPAL

    # Charges
    chargeNames:                             # Optional: List of charge names
      - Loan Processing Fee
      - Late Payment Penalty

    # Accounting
    accountingRule: CASH_BASED               # Required: NONE, CASH_BASED, ACCRUAL_PERIODIC, ACCRUAL_UPFRONT
    fundSourceAccountCode: "10102"           # Required if accounting enabled
    loanPortfolioAccountCode: "12101"        # Required if accounting enabled
    interestOnLoansAccountCode: "40201"      # Required if accounting enabled
    incomeFromFeesAccountCode: "40101"       # Required if accounting enabled
    incomeFromPenaltiesAccountCode: "40102"  # Required if accounting enabled
    writeOffAccountCode: "13101"             # Required if accounting enabled

    # Additional settings
    includeInBorrowerCycle: false            # Optional: Track borrower cycle
    useBorrowerCycle: false                  # Optional: Use cycle-based terms
    multiDisburseLoan: false                 # Optional: Allow multiple disbursements
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Product display name |
| `shortName` | String(4) | Yes | - | Short code (max 4 chars) |
| `description` | String(500) | No | - | Product description |
| `fundSourceName` | String | No | - | Fund source reference (by name) |
| `currencyCode` | String(3) | Yes | - | ISO 4217 currency code |
| `digitsAfterDecimal` | Integer | Yes | - | Decimal places (0 for XAF, 2 for USD) |
| **Principal Configuration** |
| `principal` | BigDecimal | Yes | - | Default loan principal amount |
| `minPrincipal` | BigDecimal | No | - | Minimum principal allowed |
| `maxPrincipal` | BigDecimal | No | - | Maximum principal allowed |
| **Loan Term Configuration** |
| `numberOfRepayments` | Integer | Yes | - | Default number of repayments |
| `minNumberOfRepayments` | Integer | No | - | Minimum repayments allowed |
| `maxNumberOfRepayments` | Integer | No | - | Maximum repayments allowed |
| `repaymentFrequencyType` | Enum | Yes | - | DAYS, WEEKS, MONTHS, YEARS |
| `repaymentEvery` | Integer | Yes | - | Frequency multiplier (e.g., every 2 weeks) |
| **Interest Configuration** |
| `interestRatePerPeriod` | BigDecimal | Yes | - | Default interest rate per period |
| `minInterestRatePerPeriod` | BigDecimal | No | - | Minimum rate allowed |
| `maxInterestRatePerPeriod` | BigDecimal | No | - | Maximum rate allowed |
| `interestRateFrequencyType` | Enum | Yes | - | MONTH or YEAR |
| `interestType` | Enum | Yes | - | FLAT or DECLINING_BALANCE |
| `interestCalculationPeriodType` | Enum | Yes | - | DAILY or SAME_AS_REPAYMENT_PERIOD |
| `amortizationType` | Enum | Yes | - | EQUAL_INSTALLMENTS or EQUAL_PRINCIPAL |
| **Charges** |
| `chargeNames` | List<String> | No | [] | List of charge names to attach |
| **Accounting** |
| `accountingRule` | Enum | Yes | - | NONE, CASH_BASED, ACCRUAL_PERIODIC, ACCRUAL_UPFRONT |
| `fundSourceAccountCode` | String | Conditional | - | Required if accounting enabled (GL code or reference) |
| `loanPortfolioAccountCode` | String | Conditional | - | Loans receivable account |
| `interestOnLoansAccountCode` | String | Conditional | - | Interest income account |
| `incomeFromFeesAccountCode` | String | Conditional | - | Fee income account |
| `incomeFromPenaltiesAccountCode` | String | Conditional | - | Penalty income account |
| `writeOffAccountCode` | String | Conditional | - | Loan write-off expense account |
| **Additional Settings** |
| `includeInBorrowerCycle` | Boolean | No | false | Track borrower loan cycle |
| `useBorrowerCycle` | Boolean | No | false | Use cycle-based terms |
| `multiDisburseLoan` | Boolean | No | false | Allow multiple disbursements |
| `maxTrancheCount` | Integer | No | - | Max number of tranches (if multi-disburse) |
| `outstandingLoanBalance` | BigDecimal | No | - | Max outstanding balance (if multi-disburse) |

**Interest Type**:

| Value | Description | Calculation |
|-------|-------------|-------------|
| `FLAT` | Flat interest | Interest = Principal × Rate × Term (fixed) |
| `DECLINING_BALANCE` | Reducing balance | Interest calculated on outstanding principal |

**Interest Calculation Period Type**:

| Value | Description |
|-------|-------------|
| `DAILY` | Calculate interest daily (365 days basis) |
| `SAME_AS_REPAYMENT_PERIOD` | Calculate interest based on repayment frequency |

**Amortization Type**:

| Value | Description |
|-------|-------------|
| `EQUAL_INSTALLMENTS` | Equal total payments (principal + interest) each period |
| `EQUAL_PRINCIPAL` | Equal principal payments (interest varies) |

**Accounting Rule**:

| Value | Description | Requires GL Accounts |
|-------|-------------|---------------------|
| `NONE` | No accounting integration | No |
| `CASH_BASED` | Cash basis accounting | Yes (6 accounts) |
| `ACCRUAL_PERIODIC` | Periodic accrual | Yes (6+ accounts) |
| `ACCRUAL_UPFRONT` | Upfront accrual | Yes (6+ accounts) |

**Examples**:

```yaml
# Example 1: Simple personal loan (cash accounting)
loanProducts:
  - name: Personal Loan
    shortName: PL
    description: Short-term personal loan
    currencyCode: XAF
    digitsAfterDecimal: 0

    principal: 1000000
    minPrincipal: 100000
    maxPrincipal: 5000000

    numberOfRepayments: 12
    minNumberOfRepayments: 6
    maxNumberOfRepayments: 24
    repaymentFrequencyType: MONTHS
    repaymentEvery: 1

    interestRatePerPeriod: 2.0
    minInterestRatePerPeriod: 1.5
    maxInterestRatePerPeriod: 3.0
    interestRateFrequencyType: MONTH
    interestType: DECLINING_BALANCE
    interestCalculationPeriodType: SAME_AS_REPAYMENT_PERIOD
    amortizationType: EQUAL_INSTALLMENTS

    chargeNames:
      - Loan Processing Fee
      - Late Payment Penalty

    accountingRule: CASH_BASED
    fundSourceAccountCode: "10102"
    loanPortfolioAccountCode: "12101"
    interestOnLoansAccountCode: "40201"
    incomeFromFeesAccountCode: "40101"
    incomeFromPenaltiesAccountCode: "40102"
    writeOffAccountCode: "13101"

# Example 2: Business loan with fund source
loanProducts:
  - name: Business Loan
    shortName: BL
    description: Working capital loan for businesses
    fundSourceName: Bank Loan Fund       # References fund source
    currencyCode: XAF
    digitsAfterDecimal: 0

    principal: 5000000
    minPrincipal: 1000000
    maxPrincipal: 20000000

    numberOfRepayments: 24
    minNumberOfRepayments: 12
    maxNumberOfRepayments: 60
    repaymentFrequencyType: MONTHS
    repaymentEvery: 1

    interestRatePerPeriod: 18.0          # 18% per year
    minInterestRatePerPeriod: 15.0
    maxInterestRatePerPeriod: 24.0
    interestRateFrequencyType: YEAR
    interestType: DECLINING_BALANCE
    interestCalculationPeriodType: DAILY
    amortizationType: EQUAL_INSTALLMENTS

    chargeNames:
      - Business Loan Fee
      - Late Payment Penalty

    accountingRule: CASH_BASED
    fundSourceAccountCode: $glAccount.Cash at Bank
    loanPortfolioAccountCode: $glAccount.Loans Receivable
    interestOnLoansAccountCode: $glAccount.Interest Income
    incomeFromFeesAccountCode: $glAccount.Fee Income
    incomeFromPenaltiesAccountCode: $glAccount.Penalty Income
    writeOffAccountCode: $glAccount.Loan Write-off

# Example 3: Agricultural loan with multi-disbursement
loanProducts:
  - name: Agricultural Loan
    shortName: AG
    description: Seasonal agricultural financing
    currencyCode: XAF
    digitsAfterDecimal: 0

    principal: 2000000
    minPrincipal: 500000
    maxPrincipal: 10000000

    numberOfRepayments: 3
    repaymentFrequencyType: MONTHS
    repaymentEvery: 4                    # Every 4 months (seasonal)

    interestRatePerPeriod: 12.0
    interestRateFrequencyType: YEAR
    interestType: DECLINING_BALANCE
    interestCalculationPeriodType: DAILY
    amortizationType: EQUAL_INSTALLMENTS

    multiDisburseLoan: true              # Allow multiple disbursements
    maxTrancheCount: 3                   # Up to 3 disbursements
    outstandingLoanBalance: 10000000     # Max outstanding

    chargeNames:
      - Loan Processing Fee

    accountingRule: CASH_BASED
    fundSourceAccountCode: "10102"
    loanPortfolioAccountCode: "12101"
    interestOnLoansAccountCode: "40201"
    incomeFromFeesAccountCode: "40101"
    incomeFromPenaltiesAccountCode: "40102"
    writeOffAccountCode: "13101"

# Example 4: Microfinance loan with flat interest
loanProducts:
  - name: Microfinance Loan
    shortName: MF
    description: Small loans for micro-entrepreneurs
    currencyCode: XAF
    digitsAfterDecimal: 0

    principal: 200000
    minPrincipal: 50000
    maxPrincipal: 500000

    numberOfRepayments: 10
    repaymentFrequencyType: WEEKS
    repaymentEvery: 1                    # Weekly repayments

    interestRatePerPeriod: 20.0
    interestRateFrequencyType: YEAR
    interestType: FLAT                   # Flat interest (simpler calculation)
    interestCalculationPeriodType: SAME_AS_REPAYMENT_PERIOD
    amortizationType: EQUAL_PRINCIPAL

    chargeNames:
      - Processing Fee

    accountingRule: CASH_BASED
    fundSourceAccountCode: "10102"
    loanPortfolioAccountCode: "12101"
    interestOnLoansAccountCode: "40201"
    incomeFromFeesAccountCode: "40101"
    incomeFromPenaltiesAccountCode: "40102"
    writeOffAccountCode: "13101"

# Example 5: No accounting (product definition only)
loanProducts:
  - name: Basic Loan
    shortName: BAS
    currencyCode: XAF
    digitsAfterDecimal: 0

    principal: 500000
    numberOfRepayments: 12
    repaymentFrequencyType: MONTHS
    repaymentEvery: 1

    interestRatePerPeriod: 2.0
    interestRateFrequencyType: MONTH
    interestType: DECLINING_BALANCE
    interestCalculationPeriodType: SAME_AS_REPAYMENT_PERIOD
    amortizationType: EQUAL_INSTALLMENTS

    accountingRule: NONE                 # No accounting integration
```

**Validation Rules**:
- Product name must be unique
- Short name must be unique and max 4 characters
- Min values must be ≤ default values ≤ max values
- Interest rate frequency must match repayment frequency logic
- If accountingRule ≠ NONE, all GL account codes are required
- All GL accounts must exist and be of correct type
- Charges must exist and have matching currency
- For multi-disbursement: maxTrancheCount and outstandingLoanBalance required

**Immutable Fields**: `name`, `shortName`, `currencyCode`

**Update Support**: Yes (via LoanProductLoader with Pattern B)

**Common Issues**:
- **Currency mismatch**: Charges must have same currency as product
- **Account type mismatch**: Fund source must be ASSET, portfolio must be ASSET, income accounts must be INCOME
- **Interest rate confusion**: Month vs Year - ensure rates are appropriate for frequency
- **Missing accounting**: If accountingRule is CASH_BASED but GL accounts missing, creation fails
- **Charge not found**: Charge names must exactly match existing charges

---

## Savings Products

**Description**: Defines savings account product terms, interest rates, balance requirements, and accounting rules.

**API Endpoint**: `POST /api/v1/savingsproducts`

**YAML Structure**:

```yaml
savingsProducts:
  - name: Regular Savings                     # Required: Product name
    shortName: RS                             # Required: Abbreviation
    description: Standard savings account     # Optional: Description
    currencyCode: XAF                         # Required: ISO currency code
    digitsAfterDecimal: 0                     # Required: Decimal places

    # Interest configuration
    nominalAnnualInterestRate: 4.0            # Required: Annual interest rate (%)
    interestCompoundingPeriodType: MONTHLY    # Required: Compounding frequency
    interestPostingPeriodType: MONTHLY        # Required: Posting frequency
    interestCalculationType: DAILY_BALANCE    # Required: Calculation method
    interestCalculationDaysInYearType: DAYS_365  # Required: Day count convention

    # Balance requirements
    minRequiredOpeningBalance: 10000          # Optional: Min to open account
    minBalanceForInterestCalculation: 5000    # Optional: Min for interest
    enforceMinRequiredBalance: false          # Optional: Enforce minimum
    minRequiredBalance: 5000                  # Optional: Minimum balance

    # Withdrawal settings
    withdrawalFeeForTransfers: false          # Optional: Charge fee on transfers
    allowOverdraft: false                     # Optional: Allow negative balance
    overdraftLimit: 0                         # Optional: Max overdraft

    # Charges
    chargeNames:                              # Optional: List of charge names
      - Monthly Account Fee
      - ATM Withdrawal Fee

    # Accounting
    accountingRule: CASH_BASED                # Required: NONE, CASH_BASED
    savingsReferenceAccountCode: "20101"      # Required if accounting enabled
    savingsControlAccountCode: "20102"        # Required if accounting enabled
    transfersInSuspenseAccountCode: "20103"   # Required if accounting enabled
    interestOnSavingsAccountCode: "50101"     # Required if accounting enabled
    incomeFromFeesAccountCode: "40103"        # Required if accounting enabled
    incomeFromPenaltiesAccountCode: "40104"   # Required if accounting enabled
```

**Field Reference**:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String(100) | Yes | - | Product display name |
| `shortName` | String(4) | Yes | - | Short code (max 4 chars) |
| `description` | String(500) | No | - | Product description |
| `currencyCode` | String(3) | Yes | - | ISO 4217 currency code |
| `digitsAfterDecimal` | Integer | Yes | - | Decimal places (0 for XAF, 2 for USD) |
| **Interest Configuration** |
| `nominalAnnualInterestRate` | BigDecimal | Yes | - | Annual interest rate (percentage, e.g., 4.0 = 4%) |
| `interestCompoundingPeriodType` | Enum | Yes | - | DAILY, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL |
| `interestPostingPeriodType` | Enum | Yes | - | MONTHLY, QUARTERLY, BIANNUAL, ANNUAL |
| `interestCalculationType` | Enum | Yes | - | DAILY_BALANCE or AVERAGE_DAILY_BALANCE |
| `interestCalculationDaysInYearType` | Enum | Yes | - | DAYS_360 or DAYS_365 |
| **Balance Requirements** |
| `minRequiredOpeningBalance` | BigDecimal | No | 0 | Minimum deposit to open account |
| `minBalanceForInterestCalculation` | BigDecimal | No | 0 | Minimum balance to earn interest |
| `enforceMinRequiredBalance` | Boolean | No | false | Block withdrawals if below minimum |
| `minRequiredBalance` | BigDecimal | No | 0 | Minimum balance to maintain |
| **Withdrawal Settings** |
| `withdrawalFeeForTransfers` | Boolean | No | false | Apply withdrawal fee to transfers |
| `allowOverdraft` | Boolean | No | false | Allow negative balance |
| `overdraftLimit` | BigDecimal | No | 0 | Maximum negative balance allowed |
| **Charges** |
| `chargeNames` | List<String> | No | [] | List of charge names to attach |
| **Accounting** |
| `accountingRule` | Enum | Yes | - | NONE or CASH_BASED |
| `savingsReferenceAccountCode` | String | Conditional | - | Client savings liability account |
| `savingsControlAccountCode` | String | Conditional | - | Control account for savings |
| `transfersInSuspenseAccountCode` | String | Conditional | - | Suspense account for transfers |
| `interestOnSavingsAccountCode` | String | Conditional | - | Interest expense account |
| `incomeFromFeesAccountCode` | String | Conditional | - | Fee income account |
| `incomeFromPenaltiesAccountCode` | String | Conditional | - | Penalty income account |
| `overdraftPortfolioControlAccountCode` | String | No | - | Overdraft control account (if overdrafts allowed) |
| `incomeFromInterestAccountCode` | String | No | - | Income from overdraft interest |

**Interest Compounding Period**:

| Value | Description |
|-------|-------------|
| `DAILY` | Interest compounds daily |
| `MONTHLY` | Interest compounds monthly |
| `QUARTERLY` | Interest compounds every 3 months |
| `SEMI_ANNUAL` | Interest compounds every 6 months |
| `ANNUAL` | Interest compounds annually |

**Interest Posting Period**:

| Value | Description |
|-------|-------------|
| `MONTHLY` | Interest posted to account monthly |
| `QUARTERLY` | Interest posted every 3 months |
| `BIANNUAL` | Interest posted every 6 months |
| `ANNUAL` | Interest posted annually |

**Interest Calculation Type**:

| Value | Description |
|-------|-------------|
| `DAILY_BALANCE` | Interest = Balance × Rate × Days |
| `AVERAGE_DAILY_BALANCE` | Interest = Avg(Daily Balances) × Rate |

**Examples**:

```yaml
# Example 1: Regular savings account
savingsProducts:
  - name: Regular Savings
    shortName: RS
    description: Standard savings account with monthly interest
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 4.0
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: MONTHLY
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    minRequiredOpeningBalance: 10000
    minBalanceForInterestCalculation: 5000
    enforceMinRequiredBalance: false

    chargeNames:
      - Monthly Account Fee

    accountingRule: CASH_BASED
    savingsReferenceAccountCode: "20101"
    savingsControlAccountCode: "20102"
    transfersInSuspenseAccountCode: "20103"
    interestOnSavingsAccountCode: "50101"
    incomeFromFeesAccountCode: "40103"
    incomeFromPenaltiesAccountCode: "40104"

# Example 2: High-yield savings with minimum balance
savingsProducts:
  - name: Premium Savings
    shortName: PS
    description: High interest with minimum balance requirement
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 6.0          # Higher rate
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: QUARTERLY    # Posted quarterly
    interestCalculationType: AVERAGE_DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    minRequiredOpeningBalance: 100000       # Higher opening balance
    minBalanceForInterestCalculation: 50000 # Must maintain 50K for interest
    enforceMinRequiredBalance: true         # Enforce minimum
    minRequiredBalance: 50000

    chargeNames:
      - Below Minimum Balance Fee

    accountingRule: CASH_BASED
    savingsReferenceAccountCode: "20101"
    savingsControlAccountCode: "20102"
    transfersInSuspenseAccountCode: "20103"
    interestOnSavingsAccountCode: "50101"
    incomeFromFeesAccountCode: "40103"
    incomeFromPenaltiesAccountCode: "40104"

# Example 3: Basic savings with overdraft
savingsProducts:
  - name: Checking Account
    shortName: CHK
    description: Checking account with overdraft facility
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 0.5          # Low interest
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: MONTHLY
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    minRequiredOpeningBalance: 5000
    allowOverdraft: true                    # Allow overdraft
    overdraftLimit: 100000                  # Up to 100K overdraft

    chargeNames:
      - ATM Withdrawal Fee
      - Overdraft Fee

    accountingRule: CASH_BASED
    savingsReferenceAccountCode: "20101"
    savingsControlAccountCode: "20102"
    transfersInSuspenseAccountCode: "20103"
    interestOnSavingsAccountCode: "50101"
    incomeFromFeesAccountCode: "40103"
    incomeFromPenaltiesAccountCode: "40104"
    overdraftPortfolioControlAccountCode: "12201"
    incomeFromInterestAccountCode: "40202"

# Example 4: Children's savings account
savingsProducts:
  - name: Junior Savings
    shortName: JS
    description: Savings account for minors
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 5.0
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: ANNUAL       # Post interest once a year
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    minRequiredOpeningBalance: 1000         # Low opening balance
    minBalanceForInterestCalculation: 0     # All balances earn interest
    enforceMinRequiredBalance: false
    withdrawalFeeForTransfers: false        # No withdrawal fees

    chargeNames: []                         # No charges

    accountingRule: CASH_BASED
    savingsReferenceAccountCode: "20101"
    savingsControlAccountCode: "20102"
    transfersInSuspenseAccountCode: "20103"
    interestOnSavingsAccountCode: "50101"
    incomeFromFeesAccountCode: "40103"
    incomeFromPenaltiesAccountCode: "40104"

# Example 5: Using GL account references
savingsProducts:
  - name: Standard Savings
    shortName: STD
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 4.0
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: MONTHLY
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    accountingRule: CASH_BASED
    savingsReferenceAccountCode: $glAccount.Savings Deposits
    savingsControlAccountCode: $glAccount.Savings Control
    transfersInSuspenseAccountCode: $glAccount.Transfers Suspense
    interestOnSavingsAccountCode: $glAccount.Interest on Savings
    incomeFromFeesAccountCode: $glAccount.Service Charge Income
    incomeFromPenaltiesAccountCode: $glAccount.Penalty Income

# Example 6: No accounting (product definition only)
savingsProducts:
  - name: Basic Savings
    shortName: BS
    currencyCode: XAF
    digitsAfterDecimal: 0

    nominalAnnualInterestRate: 3.0
    interestCompoundingPeriodType: MONTHLY
    interestPostingPeriodType: MONTHLY
    interestCalculationType: DAILY_BALANCE
    interestCalculationDaysInYearType: DAYS_365

    accountingRule: NONE                    # No accounting integration
```

**Validation Rules**:
- Product name must be unique
- Short name must be unique and max 4 characters
- Interest rate must be non-negative (can be 0)
- Compounding period must be ≤ posting period (e.g., can't compound quarterly and post monthly)
- If enforceMinRequiredBalance = true, minRequiredBalance must be > 0
- If allowOverdraft = true, overdraftLimit must be > 0
- If accountingRule = CASH_BASED, all required GL accounts must be provided
- All GL accounts must exist and be of correct type
- Charges must exist and have matching currency

**Immutable Fields**: `name`, `shortName`, `currencyCode`

**Update Support**: Yes (via SavingsProductLoader with Pattern B)

**Common Issues**:
- **Posting before compounding**: Interest posting period must be ≥ compounding period
- **Account type mismatch**: Savings accounts must be LIABILITY, interest expense must be EXPENSE, income accounts must be INCOME
- **Overdraft without accounts**: If allowOverdraft = true, must provide overdraft GL accounts
- **Currency mismatch**: Charges must have same currency as product
- **Charge not found**: Charge names must exactly match existing charges

---

For complete documentation, see:
- [README.md](README.md) - Overview and quick start
- [USAGE.md](USAGE.md) - Command examples and workflows
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [CONTRIBUTING.md](CONTRIBUTING.md) - Development guidelines
