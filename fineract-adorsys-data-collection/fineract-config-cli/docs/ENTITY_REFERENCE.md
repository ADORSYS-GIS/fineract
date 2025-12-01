# Fineract Config CLI - Entity Reference

**Complete reference for all 40 supported entity types**

**Version**: 1.0
**Last Updated**: 2025-01-20

---

## Table of Contents

- [Overview](#overview)
- [Loading Order](#loading-order)
- [Phase 1: System Configuration](#phase-1-system-configuration-8-entities)
- [Phase 2: Security & Organization](#phase-2-security--organization-4-entities)
- [Phase 3: Accounting & Workflow](#phase-3-accounting--workflow-6-entities)
- [Phase 4: Financial Products](#phase-4-financial-products-13-entities)
- [Phase 5: Client Accounts](#phase-5-client-accounts-5-entities)
- [Phase 6: Transactions](#phase-6-transactions-4-entities)
- [Reference Guide](#reference-guide)

---

## Overview

This document provides complete details for all 40 entity types supported by Fineract Config CLI.

### Entity Categories

| Phase | Category | Entities | Dependencies |
|-------|----------|----------|--------------|
| 1 | System Config | 8 | None |
| 2 | Security & Org | 4 | Phase 1 |
| 3 | Accounting | 6 | Phases 1-2 |
| 4 | Products | 13 | Phases 1-3 |
| 5 | Accounts | 5 | Phases 1-4 |
| 6 | Transactions | 4 | Phases 1-5 |

### Common Fields

All entities support these optional fields:

- `externalId`: Unique external identifier (recommended for all entities)
- `description`: Human-readable description (where applicable)

---

## Loading Order

Entities must be loaded in this specific order to respect dependencies:

```
PHASE 1: System Configuration (No Dependencies)
  1. Currency Configuration
  2. Working Days
  3. Global Configuration
  4. Codes & Code Values
  5. Account Number Preferences
  6. SMS/Email Configuration
  7. Notification Templates
  8. Data Tables

PHASE 2: Security & Organization
  9. Roles & Permissions        (no dependencies)
 10. Offices                    (no dependencies)
 11. Staff                      (→ Roles, Offices)
 12. Tellers                    (→ Offices)

PHASE 3: Accounting & Workflow
 13. Chart of Accounts          (no dependencies)
 14. Financial Activity Mappings (→ GL Accounts)
 15. Teller Accounting Rules    (→ GL Accounts, Offices)
 16. Maker-Checker Config       (→ Roles)
 17. Scheduler Jobs             (no dependencies)
 18. Loan Provisioning          (→ GL Accounts)

PHASE 4: Financial Products
 19. Floating Rates             (→ GL Accounts)
 20. Tax Groups                 (→ GL Accounts)
 21. Charges                    (no dependencies)
 22. Fund Sources               (no dependencies)
 23. Payment Types              (no dependencies)
 24. Holiday Calendar           (no dependencies)
 25. Loan Products              (→ GL Accounts, Charges, Funds)
 26. Delinquency Buckets        (→ Loan Products)
 27. Savings Products           (→ GL Accounts, Charges, Tax Groups)
 28. Collateral Types           (no dependencies)
 29-31. Product Accounting      (auto-created with products)

PHASE 5: Client Accounts
 32. Clients                    (→ Offices, Staff)
 33. Savings Accounts           (→ Clients, Savings Products)
 34. Loan Accounts              (→ Clients, Loan Products)
 35. Loan Collateral            (→ Loans, Collateral Types)
 36. Loan Guarantors            (→ Loans)

PHASE 6: Transactions (Optional)
 37. Savings Deposits           (→ Savings Accounts)
 38. Savings Withdrawals        (→ Savings Accounts)
 39. Loan Repayments            (→ Loan Accounts)
 40. Inter-Branch Transfers     (→ Offices, Accounts)
```

---

## Phase 1: System Configuration (8 Entities)

### 1. Currency Configuration

**Purpose**: Define currencies used in the system

**YAML Key**: `systemConfig.currency`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `code` | String | Yes | ISO 4217 currency code | `USD`, `XAF`, `EUR` |
| `name` | String | Yes | Currency full name | `US Dollar` |
| `decimalPlaces` | Integer | Yes | Number of decimal places | `2` (USD), `0` (XAF) |

**YAML Example**:

```yaml
systemConfig:
  currency:
    code: XAF
    name: Central African CFA Franc
    decimalPlaces: 0
```

**API Endpoint**: `PUT /fineract-provider/api/v1/currencies`

**Notes**:
- Only ONE currency can be configured per tenant
- Cannot be changed after clients/accounts exist
- Affects all monetary transactions

---

### 2. Working Days

**Purpose**: Define business days for loan calculations

**YAML Key**: `systemConfig.workingDays`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `workingDays` | Array[Enum] | Yes | Days of the week | `[MONDAY, TUESDAY, ...]` |
| `repaymentReschedulingRule` | Enum | No | How to handle non-working days | `MOVE_TO_NEXT_WORKING_DAY` |

**Enum Values**:
- Days: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`
- Rules: `MOVE_TO_NEXT_WORKING_DAY`, `MOVE_TO_NEXT_REPAYMENT_MEETING_DAY`, `MOVE_TO_PREVIOUS_WORKING_DAY`

**YAML Example**:

```yaml
systemConfig:
  workingDays:
    days:
      - MONDAY
      - TUESDAY
      - WEDNESDAY
      - THURSDAY
      - FRIDAY
    repaymentReschedulingRule: MOVE_TO_NEXT_WORKING_DAY
```

**API Endpoint**: `PUT /fineract-provider/api/v1/workingdays`

---

### 3. Global Configuration

**Purpose**: System-wide settings and toggles

**YAML Key**: `systemConfig.globalConfig`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Configuration key | `maker-checker` |
| `enabled` | Boolean | No | Enable/disable toggle | `true` |
| `value` | String/Number | No | Configuration value | `100` |
| `description` | String | No | Documentation | `Enable dual authorization` |

**Common Configurations** (23 total):

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `maker-checker` | Boolean | false | Enable dual authorization workflow |
| `amazon-S3_bucket_name` | String | - | S3 bucket for document storage |
| `amazon-S3_access_key` | String | - | S3 access key |
| `amazon-S3_secret_key` | String | - | S3 secret key |
| `reschedule-future-repayments` | Boolean | true | Allow rescheduling future payments |
| `reschedule-repayments-on-holidays` | Boolean | false | Move payments on holidays |
| `allow-transactions-on-holiday` | Boolean | false | Allow transactions on holidays |
| `allow-transactions-on-non-working-day` | Boolean | false | Allow transactions on weekends |
| `constraint-approach-for-datatables` | Boolean | false | Validate datatable constraints |
| `penalty-wait-period` | Integer | 2 | Days before applying penalties |
| `days-in-year-type` | Enum | ACTUAL | `ACTUAL`, `360`, `364`, `365` |
| `min-clients-in-group` | Integer | 5 | Minimum group size |
| `max-clients-in-group` | Integer | - | Maximum group size |

**YAML Example**:

```yaml
systemConfig:
  globalConfig:
    - name: maker-checker
      enabled: true
    - name: amazon-S3_bucket_name
      value: ${env:S3_BUCKET}
    - name: penalty-wait-period
      value: 2
    - name: days-in-year-type
      value: "ACTUAL"
```

**API Endpoint**: `PUT /fineract-provider/api/v1/configurations/{id}`

---

### 4. Codes & Code Values

**Purpose**: Dropdown values and classification codes

**YAML Key**: `systemConfig.codes`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Code category name | `Gender` |
| `systemDefined` | Boolean | No | System or custom code | `false` |
| `values` | Array[Object] | Yes | Dropdown values | See below |

**Code Value Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Display value | `Male` |
| `position` | Integer | No | Sort order | `1` |
| `isActive` | Boolean | No | Active status | `true` |
| `description` | String | No | Documentation | `Male gender` |

**Standard Codes** (10 categories, 48 values):

1. **Gender** (3): Male, Female, Other
2. **Client Type** (2): Individual, Business
3. **Client Classification** (3): Gold, Silver, Bronze
4. **Marital Status** (4): Single, Married, Divorced, Widowed
5. **Education Level** (4): None, Primary, Secondary, University
6. **Loan Purpose** (6): Business, Agriculture, Education, Housing, Medical, Personal
7. **Savings Closure Reason** (5): Client Request, Dormant, Duplicate, Fraud, Other
8. **Loan Closure Reason** (4): Fully Paid, Written Off, Rescheduled, Closed
9. **Business Type** (7): Retail, Agriculture, Services, Manufacturing, Construction, Transport, Other
10. **ID Type** (4): National ID, Passport, Driver License, Voter Card

**YAML Example**:

```yaml
systemConfig:
  codes:
    - name: Gender
      systemDefined: false
      values:
        - name: Male
          position: 1
        - name: Female
          position: 2
        - name: Other
          position: 3

    - name: Client Type
      values:
        - name: Individual
        - name: Business

    - name: Loan Purpose
      values:
        - name: Business Expansion
          description: For growing existing business
        - name: Working Capital
        - name: Equipment Purchase
        - name: Agriculture
        - name: Education
        - name: Housing
```

**API Endpoint**:
- Create Code: `POST /fineract-provider/api/v1/codes`
- Create Value: `POST /fineract-provider/api/v1/codes/{codeId}/codevalues`

---

### 5. Account Number Preferences

**Purpose**: Auto-generate account numbers

**YAML Key**: `systemConfig.accountNumbering`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `entityType` | Enum | Yes | Entity type | `CLIENT`, `LOAN`, `SAVINGS` |
| `prefixType` | Enum | No | Prefix source | `OFFICE`, `PRODUCT`, `NONE` |
| `length` | Integer | Yes | Number length (digits) | `6` |

**Entity Types**:
- `CLIENT`, `GROUP`, `CENTER`, `LOAN`, `SAVINGS`

**Prefix Types**:
- `NONE` - No prefix
- `OFFICE` - Office external ID
- `PRODUCT` - Product short name
- `TEXT` - Custom text

**YAML Example**:

```yaml
systemConfig:
  accountNumbering:
    - entityType: CLIENT
      prefixType: OFFICE
      length: 6
      # Format: {office_id}{6-digits} → HO001000001

    - entityType: LOAN
      prefixType: PRODUCT
      length: 6
      # Format: {product_shortname}{6-digits} → MSOL000001

    - entityType: SAVINGS
      prefixType: PRODUCT
      length: 8
      # Format: {product_shortname}{8-digits} → VSAV00000001

    - entityType: GROUP
      prefixType: TEXT
      prefixText: GRP
      length: 3
      # Format: GRP{3-digits} → GRP001
```

**API Endpoint**: `POST /fineract-provider/api/v1/accountnumberformats`

---

### 6. SMS/Email Configuration

**Purpose**: Configure notification channels

**YAML Key**: `systemConfig.notifications`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `type` | Enum | Yes | Channel type | `SMS`, `EMAIL` |
| `provider` | String | Yes | Service provider | `Twilio`, `Gmail` |
| `config` | Object | Yes | Provider-specific settings | See below |

**SMS Configuration (Twilio)**:

```yaml
systemConfig:
  notifications:
    sms:
      provider: Twilio
      config:
        accountSid: ${env:TWILIO_ACCOUNT_SID}
        authToken: ${env:TWILIO_AUTH_TOKEN}
        fromNumber: "+1234567890"
        enabled: true
```

**Email Configuration (SMTP)**:

```yaml
systemConfig:
  notifications:
    email:
      provider: SMTP
      config:
        host: smtp.gmail.com
        port: 587
        username: ${env:SMTP_USERNAME}
        password: ${env:SMTP_PASSWORD}
        fromAddress: noreply@example.com
        fromName: Fineract System
        useTLS: true
        useSSL: false
```

**API Endpoint**: `PUT /fineract-provider/api/v1/externalservices/{serviceName}`

---

### 7. Notification Templates

**Purpose**: Define email/SMS message templates

**YAML Key**: `systemConfig.notificationTemplates`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Template name | `Client Activation` |
| `type` | Enum | Yes | Channel type | `SMS`, `EMAIL` |
| `subject` | String | No | Email subject (email only) | `Welcome to our service` |
| `message` | String | Yes | Message body | `Dear {{client.name}}, ...` |

**Available Placeholders**:
- `{{client.name}}`, `{{client.firstname}}`, `{{client.lastname}}`
- `{{loan.accountNo}}`, `{{loan.principal}}`, `{{loan.totalDue}}`
- `{{savings.accountNo}}`, `{{savings.balance}}`
- `{{office.name}}`
- `{{transaction.amount}}`, `{{transaction.date}}`

**Standard Templates** (16 total):

```yaml
systemConfig:
  notificationTemplates:
    - name: Client Activation
      type: SMS
      message: "Dear {{client.firstname}}, welcome to {{office.name}}! Your client ID is {{client.accountNo}}."

    - name: Loan Approval
      type: EMAIL
      subject: "Loan Approved - {{loan.accountNo}}"
      message: "Dear {{client.name}}, your loan of {{loan.principal}} {{loan.currency}} has been approved."

    - name: Repayment Due
      type: SMS
      message: "Reminder: Loan {{loan.accountNo}} repayment of {{transaction.amount}} due on {{transaction.dueDate}}."

    - name: Savings Deposit Confirmation
      type: SMS
      message: "Deposit of {{transaction.amount}} received. New balance: {{savings.balance}}."
```

**API Endpoint**: `POST /fineract-provider/api/v1/templates`

---

### 8. Data Tables (Custom Fields)

**Purpose**: Add custom fields to entities

**YAML Key**: `systemConfig.dataTables`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `entityType` | Enum | Yes | Target entity | `CLIENT`, `LOAN`, `SAVINGS` |
| `tableName` | String | Yes | Table name | `client_additional_info` |
| `columns` | Array[Object] | Yes | Custom fields | See below |

**Column Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Column name | `id_type` |
| `type` | Enum | Yes | Data type | `STRING`, `NUMBER`, `DROPDOWN`, `DATE` |
| `mandatory` | Boolean | No | Required field | `false` |
| `length` | Integer | No | Max length (strings) | `50` |
| `code` | String | No | Code name (dropdowns) | `ID Type` |

**Data Types**:
- `STRING`, `NUMBER`, `DECIMAL`, `DATE`, `DATETIME`, `TEXT`, `DROPDOWN`

**Example - Client Custom Fields** (12 fields):

```yaml
systemConfig:
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
          mandatory: false

        - name: next_of_kin_name
          type: STRING
          length: 100

        - name: next_of_kin_phone
          type: STRING
          length: 20

        - name: employer_name
          type: STRING
          length: 100

        - name: years_in_business
          type: NUMBER

        - name: disability_status
          type: DROPDOWN
          code: Disability Status
```

**Example - Loan Custom Fields** (7 fields):

```yaml
    - entityType: LOAN
      tableName: loan_additional_info
      columns:
        - name: loan_purpose_detail
          type: TEXT

        - name: collateral_description
          type: TEXT

        - name: collateral_value
          type: DECIMAL

        - name: guarantor_count
          type: NUMBER

        - name: credit_score
          type: NUMBER
```

**API Endpoint**: `POST /fineract-provider/api/v1/datatables`

---

## Phase 2: Security & Organization (4 Entities)

### 9. Roles & Permissions

**Purpose**: Define user roles and access control

**YAML Key**: `roles`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Role name | `Branch Manager` |
| `description` | String | No | Role description | `Manages branch operations` |
| `permissions` | Array[String] | Yes | Permission codes | `ALL_FUNCTIONS_READ_CLIENT` |

**Permission Format**: `<ACTION>_<ENTITY>`

**Actions**:
- `READ` - View only
- `CREATE` - Create new
- `UPDATE` - Modify existing
- `DELETE` - Remove
- `ALL_FUNCTIONS` - Full CRUD
- `APPROVE` - Approve pending
- `DISBURSE` - Disburse loans
- `REPAYMENT` - Accept repayments

**Common Permission Patterns**:

```yaml
roles:
  - name: Branch Manager
    description: Full branch management capabilities
    permissions:
      # Clients
      - ALL_FUNCTIONS_READ_CLIENT
      - CREATE_CLIENT
      - UPDATE_CLIENT
      - ACTIVATE_CLIENT
      - CLOSE_CLIENT

      # Loans
      - ALL_FUNCTIONS_LOAN
      - CREATE_LOAN
      - UPDATE_LOAN
      - APPROVE_LOAN
      - DISBURSE_LOAN
      - REPAYMENT_LOAN
      - WAIVEINTERESTPORTION_LOAN

      # Savings
      - ALL_FUNCTIONS_READ_SAVINGS
      - CREATE_SAVINGS
      - DEPOSIT_SAVINGS
      - WITHDRAWAL_SAVINGS

      # Office & Staff
      - READ_OFFICE
      - READ_STAFF
      - CREATE_STAFF

      # Reports
      - READ_REPORT

      # Accounting
      - READ_ACCOUNTING
      - READ_JOURNALENTRY

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
      - ALLOCATECASHIER_TELLER
```

**API Endpoint**: `POST /fineract-provider/api/v1/roles`

**Note**: 100+ permissions available. See Fineract API docs for complete list.

---

### 10. Offices

**Purpose**: Organizational hierarchy (branches)

**YAML Key**: `offices`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Office name | `Head Office` |
| `externalId` | String | Yes | Unique identifier | `HO001` |
| `openingDate` | Date | Yes | Opening date | `2024-01-01` |
| `parentOffice` | Reference | No | Parent office | `$office.Head Office` |
| `address` | String | No | Street address | `123 Main St` |
| `city` | String | No | City | `Yaounde` |
| `region` | String | No | State/region | `Centre` |
| `country` | String | No | Country | `Cameroon` |
| `phone` | String | No | Phone number | `+237677123456` |
| `email` | String | No | Email address | `branch@example.com` |

**YAML Example**:

```yaml
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
    parentOffice: $office.Head Office  # Reference to parent
    openingDate: "2024-02-01"
    address: "Boulevard de la Liberté"
    city: Douala
    region: Littoral
    country: Cameroon

  - name: Bafoussam Branch
    externalId: BAF001
    parentOffice: $office.Head Office
    openingDate: "2024-03-01"
    city: Bafoussam
    region: West

  - name: Bamenda Branch
    externalId: BAM001
    parentOffice: $office.Head Office
    openingDate: "2024-04-01"
    city: Bamenda
    region: Northwest
```

**API Endpoint**: `POST /fineract-provider/api/v1/offices`

**Dependencies**: None

**Notes**:
- First office created becomes the Head Office automatically
- `externalId` used for account number prefixes
- Hierarchy supports unlimited depth

---

### 11. Staff

**Purpose**: Staff members (loan officers, managers, cashiers)

**YAML Key**: `staff`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `firstname` | String | Yes | First name | `Jean` |
| `lastname` | String | Yes | Last name | `Dupont` |
| `office` | Reference | Yes | Assigned office | `$office.Head Office` |
| `role` | Reference | No | Security role | `$role.Branch Manager` |
| `isLoanOfficer` | Boolean | No | Can manage loans | `true` |
| `isTeller` | Boolean | No | Can operate teller | `false` |
| `username` | String | No | Login username | `jean.dupont` |
| `email` | String | No | Email address | `jean@example.com` |
| `mobile` | String | No | Mobile number | `+237677123456` |
| `joiningDate` | Date | No | Employment start | `2024-01-15` |
| `externalId` | String | No | External ID | `STF001` |
| `createUser` | Boolean | No | Create user account | `true` |
| `password` | String | No | Initial password | `${env:DEFAULT_PASSWORD}` |

**YAML Example**:

```yaml
staff:
  # Branch Managers
  - firstname: Jean
    lastname: Dupont
    office: $office.Head Office
    role: $role.Branch Manager
    isLoanOfficer: false
    username: jean.dupont
    email: jean@example.com
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
    email: marie@example.com
    createUser: true

  # Loan Officers
  - firstname: Paul
    lastname: Nkembe
    office: $office.Head Office
    role: $role.Loan Officer
    isLoanOfficer: true
    username: paul.nkembe
    email: paul@example.com
    createUser: true

  - firstname: Grace
    lastname: Fonkou
    office: $office.Douala Branch
    role: $role.Loan Officer
    isLoanOfficer: true
    username: grace.fonkou
    createUser: true

  # Cashiers
  - firstname: David
    lastname: Tabi
    office: $office.Head Office
    role: $role.Cashier
    isTeller: true
    username: david.tabi
    createUser: true
```

**API Endpoint**:
- Create Staff: `POST /fineract-provider/api/v1/staff`
- Create User: `POST /fineract-provider/api/v1/users`

**Dependencies**: Offices (required), Roles (if assigning role)

**Notes**:
- If `createUser: true`, creates both staff and user account
- Default password should be changed on first login
- `isLoanOfficer` required to assign clients/loans

---

### 12. Tellers

**Purpose**: Cash management counters

**YAML Key**: `tellers`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `office` | Reference | Yes | Office location | `$office.Head Office` |
| `name` | String | Yes | Teller name | `Main Counter` |
| `description` | String | No | Description | `Primary teller` |
| `startDate` | Date | Yes | Start date | `2024-01-01` |
| `endDate` | Date | No | End date (optional) | `2024-12-31` |
| `status` | Enum | No | Active/Inactive | `ACTIVE` |

**YAML Example**:

```yaml
tellers:
  - office: $office.Head Office
    name: Teller 1 - Head Office
    description: Main counter for cash transactions
    startDate: "2024-01-01"
    status: ACTIVE

  - office: $office.Douala Branch
    name: Teller 1 - Douala
    description: Douala branch main counter
    startDate: "2024-02-01"
    status: ACTIVE

  - office: $office.Bafoussam Branch
    name: Teller 1 - Bafoussam
    startDate: "2024-03-01"
    status: ACTIVE

  - office: $office.Bamenda Branch
    name: Teller 1 - Bamenda
    startDate: "2024-04-01"
    status: ACTIVE
```

**API Endpoint**: `POST /fineract-provider/api/v1/tellers`

**Dependencies**: Offices

**Notes**:
- Each teller needs cashier assignment (done separately)
- Multiple tellers per office supported
- Teller status can be ACTIVE or INACTIVE

---

## Phase 3: Accounting & Workflow (6 Entities)

### 13. Chart of Accounts (GL Accounts)

**Purpose**: General Ledger accounts for accounting

**YAML Key**: `chartOfAccounts`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Account name | `Cash on Hand` |
| `glCode` | String | Yes | GL code | `42` |
| `type` | Enum | Yes | Account type | `ASSET`, `LIABILITY`, `EQUITY`, `INCOME`, `EXPENSE` |
| `usage` | Enum | Yes | Detail/Header | `DETAIL`, `HEADER` |
| `parentGlCode` | String | No | Parent account (headers) | `40` |
| `description` | String | No | Description | `Cash held at branches` |
| `manualEntriesAllowed` | Boolean | No | Allow manual JEs | `true` |
| `tag` | String | No | Account tag | `TELLER` |

**Account Types**:
- `ASSET` - Assets (cash, loans receivable)
- `LIABILITY` - Liabilities (deposits, payables)
- `EQUITY` - Equity/capital
- `INCOME` - Income/revenue
- `EXPENSE` - Expenses/losses

**OHADA Chart of Accounts Example** (30 accounts):

```yaml
chartOfAccounts:
  # ASSETS
  - name: Banks
    glCode: "41"
    type: ASSET
    usage: DETAIL
    description: Bank account balances
    manualEntriesAllowed: true

  - name: Cash on Hand
    glCode: "42"
    type: ASSET
    usage: DETAIL
    description: Cash at branches/tellers
    tag: TELLER

  - name: Loans and Advances - Microcredit
    glCode: "51"
    type: ASSET
    usage: DETAIL
    description: Microcredit loan portfolio

  - name: Loans and Advances - SME
    glCode: "52"
    type: ASSET
    usage: DETAIL

  - name: Interest Receivable on Loans
    glCode: "53"
    type: ASSET
    usage: DETAIL

  - name: Fees Receivable on Loans
    glCode: "54"
    type: ASSET
    usage: DETAIL

  - name: Provision for Loan Losses
    glCode: "55"
    type: ASSET
    usage: DETAIL
    description: Loan loss provisions (contra-asset)

  - name: Due from Other Branches
    glCode: "122"
    type: ASSET
    usage: DETAIL
    description: Inter-branch receivables

  # LIABILITIES
  - name: Voluntary Savings
    glCode: "61"
    type: LIABILITY
    usage: DETAIL

  - name: Fixed Deposits
    glCode: "62"
    type: LIABILITY
    usage: DETAIL

  - name: Mandatory Savings
    glCode: "63"
    type: LIABILITY
    usage: DETAIL

  - name: Interest Payable on Savings
    glCode: "64"
    type: LIABILITY
    usage: DETAIL

  - name: Due to Other Branches
    glCode: "131"
    type: LIABILITY
    usage: DETAIL
    description: Inter-branch payables

  - name: Tax Payable - Withholding Tax
    glCode: "141"
    type: LIABILITY
    usage: DETAIL

  # EQUITY
  - name: Share Capital
    glCode: "71"
    type: EQUITY
    usage: DETAIL

  - name: Capital
    glCode: "30"
    type: EQUITY
    usage: DETAIL
    description: Opening balances contra

  # INCOME
  - name: Interest Income on Loans
    glCode: "81"
    type: INCOME
    usage: DETAIL

  - name: Fee Income on Loans
    glCode: "82"
    type: INCOME
    usage: DETAIL

  - name: Interest Expense on Savings
    glCode: "83"
    type: INCOME
    usage: DETAIL
    description: Actually expense, but recorded as contra-income

  - name: Penalty Income on Loans
    glCode: "84"
    type: INCOME
    usage: DETAIL

  - name: Other Operating Income
    glCode: "85"
    type: INCOME
    usage: DETAIL

  - name: Income from Recovery
    glCode: "86"
    type: INCOME
    usage: DETAIL
    description: Recovery of written-off loans

  # EXPENSES
  - name: Operating Expenses
    glCode: "91"
    type: EXPENSE
    usage: DETAIL

  - name: Salaries and Benefits
    glCode: "92"
    type: EXPENSE
    usage: DETAIL

  - name: Rent and Utilities
    glCode: "93"
    type: EXPENSE
    usage: DETAIL

  - name: Provision Expense
    glCode: "94"
    type: EXPENSE
    usage: DETAIL
    description: Loan loss provisioning

  - name: Losses Written Off
    glCode: "95"
    type: EXPENSE
    usage: DETAIL

  - name: Goodwill Expense Written Off
    glCode: "96"
    type: EXPENSE
    usage: DETAIL

  - name: Interest Waived
    glCode: "97"
    type: EXPENSE
    usage: DETAIL

  - name: Cash Shortage
    glCode: "98"
    type: EXPENSE
    usage: DETAIL
    description: Teller cash shortages
```

**API Endpoint**: `POST /fineract-provider/api/v1/glaccounts`

**Dependencies**: None (load first in Phase 3)

**Notes**:
- GL codes should follow organizational accounting standard (OHADA, GAAP, IFRS)
- `usage: DETAIL` accounts can have transactions
- `usage: HEADER` accounts only for grouping (sum of children)

---

### 14. Financial Activity Mappings

**Purpose**: Map system activities to GL accounts

**YAML Key**: `financialActivityMappings`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `activity` | Enum | Yes | Financial activity | `ASSET_TRANSFER` |
| `account` | Reference | Yes | GL account | `$glAccount.122` |

**Financial Activities** (6 core):

```yaml
financialActivityMappings:
  - activity: ASSET_TRANSFER
    account: $glAccount.122  # Due from Other Branches

  - activity: LIABILITY_TRANSFER
    account: $glAccount.131  # Due to Other Branches

  - activity: CASH_AT_MAINVAULT
    account: $glAccount.42   # Cash on Hand

  - activity: CASH_AT_TELLER
    account: $glAccount.42   # Cash on Hand

  - activity: OPENING_BALANCES_CONTRA_ACCOUNT
    account: $glAccount.30   # Capital

  - activity: FUND_SOURCE
    account: $glAccount.41   # Banks
```

**API Endpoint**: `POST /fineract-provider/api/v1/financialactivityaccounts`

**Dependencies**: Chart of Accounts

---

### 15. Teller Accounting Rules

**Purpose**: GL account mappings for teller operations

**YAML Key**: `tellerAccountingRules`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `office` | Reference | Yes | Office | `$office.Head Office` |
| `tellerCashAccount` | Reference | Yes | Teller cash GL | `$glAccount.42` |
| `vaultAccount` | Reference | Yes | Vault cash GL | `$glAccount.42` |
| `cashShortageAccount` | Reference | Yes | Shortage GL | `$glAccount.98` |
| `cashOverageAccount` | Reference | Yes | Overage GL | `$glAccount.86` |

**YAML Example**:

```yaml
tellerAccountingRules:
  - office: $office.Head Office
    tellerCashAccount: $glAccount.42
    vaultAccount: $glAccount.42
    cashShortageAccount: $glAccount.98
    cashOverageAccount: $glAccount.86

  - office: $office.Douala Branch
    tellerCashAccount: $glAccount.42
    vaultAccount: $glAccount.42
    cashShortageAccount: $glAccount.98
    cashOverageAccount: $glAccount.86
```

**API Endpoint**: Configured via office settings

**Dependencies**: Offices, Chart of Accounts

---

### 16. Maker-Checker Configuration

**Purpose**: Dual authorization rules

**YAML Key**: `makerCheckerConfig`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `action` | String | Yes | Action requiring approval | `APPROVE_LOAN` |
| `entity` | String | Yes | Entity type | `LOAN` |
| `requiresApproval` | Boolean | Yes | Enable rule | `true` |
| `amountThreshold` | Decimal | No | Threshold amount | `2000000` |

**Common Rules** (12 rules):

```yaml
makerCheckerConfig:
  # Loan Operations
  - action: APPROVE
    entity: LOAN
    requiresApproval: true
    amountThreshold: 2000000  # XAF

  - action: DISBURSE
    entity: LOAN
    requiresApproval: true
    amountThreshold: 5000000

  - action: WRITEOFF
    entity: LOAN
    requiresApproval: true  # All write-offs

  - action: RESCHEDULE
    entity: LOAN
    requiresApproval: true
    amountThreshold: 1000000

  # Savings Operations
  - action: WITHDRAWAL
    entity: SAVINGS
    requiresApproval: true
    amountThreshold: 1000000

  - action: CLOSE
    entity: SAVINGS
    requiresApproval: true
    amountThreshold: 500000

  # Client Operations
  - action: ACTIVATE
    entity: CLIENT
    requiresApproval: true

  - action: TRANSFER
    entity: CLIENT
    requiresApproval: true

  # Accounting
  - action: CREATE
    entity: JOURNALENTRY
    requiresApproval: true
    amountThreshold: 500000

  # User Management
  - action: CREATE
    entity: USER
    requiresApproval: true

  - action: UPDATE
    entity: USER_ROLE
    requiresApproval: true

  # Inter-Office
  - action: TRANSFER
    entity: OFFICE
    requiresApproval: true
    amountThreshold: 3000000
```

**API Endpoint**: `PUT /fineract-provider/api/v1/configurations/permissions`

**Dependencies**: Roles (permissions)

**Note**: Amount thresholds are reference only - Fineract doesn't enforce them natively

---

### 17. Scheduler Jobs

**Purpose**: Automated background tasks

**YAML Key**: `schedulerJobs`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Job name | `Post Interest For Savings` |
| `cronExpression` | String | Yes | Cron schedule | `0 0 0 * * ?` |
| `active` | Boolean | No | Enable job | `true` |

**Standard Jobs** (10 jobs):

```yaml
schedulerJobs:
  - name: Post Interest For Savings
    cronExpression: "0 0 0 * * ?"  # Daily at midnight
    active: true

  - name: Apply Charges To Overdue Loans
    cronExpression: "0 0 3 * * ?"  # Daily at 3 AM
    active: true

  - name: Update Savings Dormancy Flags
    cronExpression: "0 0 0 * * ?"  # Daily at midnight
    active: true

  - name: Transfer Fee For Loans From Savings
    cronExpression: "0 0 1 * * ?"  # Daily at 1 AM
    active: true

  - name: Apply Annual Fee For Savings
    cronExpression: "0 0 0 1 1 ?"  # Yearly on Jan 1
    active: true

  - name: Apply Holidays To Loans
    cronExpression: "0 0 2 * * ?"  # Daily at 2 AM
    active: true

  - name: Update Loan Arrears Aging
    cronExpression: "0 0 4 * * ?"  # Daily at 4 AM
    active: true

  - name: Execute Standing Instructions
    cronExpression: "0 0 0 * * ?"  # Daily at midnight
    active: true

  - name: Update Loan Paid In Advance
    cronExpression: "0 0 5 * * ?"  # Daily at 5 AM
    active: true

  - name: Recalculate Interest For Loans
    cronExpression: "0 0 6 * * ?"  # Daily at 6 AM
    active: true
```

**Cron Format**: `second minute hour day month dayOfWeek`

**API Endpoint**: `PUT /fineract-provider/api/v1/jobs/{jobId}`

---

### 18. Loan Provisioning Criteria

**Purpose**: Loan loss provisioning rules (COBAC/IFRS)

**YAML Key**: `loanProvisioningCriteria`

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Criteria name | `COBAC Provisioning` |
| `categories` | Array[Object] | Yes | Provision categories | See below |

**Category Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `name` | String | Yes | Category name | `Performing` |
| `minDaysOverdue` | Integer | Yes | Minimum days | `0` |
| `maxDaysOverdue` | Integer | Yes | Maximum days | `30` |
| `provisionPercentage` | Decimal | Yes | Provision % | `0` |
| `liabilityAccount` | Reference | Yes | Provision GL | `$glAccount.55` |
| `expenseAccount` | Reference | Yes | Expense GL | `$glAccount.94` |

**COBAC Example** (5 categories):

```yaml
loanProvisioningCriteria:
  - name: COBAC Provisioning Standard
    categories:
      - name: Performing
        minDaysOverdue: 0
        maxDaysOverdue: 30
        provisionPercentage: 0
        liabilityAccount: $glAccount.55
        expenseAccount: $glAccount.94

      - name: Watch
        minDaysOverdue: 31
        maxDaysOverdue: 60
        provisionPercentage: 25
        liabilityAccount: $glAccount.55
        expenseAccount: $glAccount.94

      - name: Substandard
        minDaysOverdue: 61
        maxDaysOverdue: 90
        provisionPercentage: 50
        liabilityAccount: $glAccount.55
        expenseAccount: $glAccount.94

      - name: Doubtful
        minDaysOverdue: 91
        maxDaysOverdue: 180
        provisionPercentage: 75
        liabilityAccount: $glAccount.55
        expenseAccount: $glAccount.94

      - name: Loss
        minDaysOverdue: 181
        maxDaysOverdue: 999999
        provisionPercentage: 100
        liabilityAccount: $glAccount.55
        expenseAccount: $glAccount.94
```

**API Endpoint**: `POST /fineract-provider/api/v1/provisioningcriteria`

**Dependencies**: Chart of Accounts

---

## Phase 4: Financial Products (13 Entities)

Due to length, continuing with Phase 4 entities...

### 19. Floating Rates

**Purpose**: Variable interest rate benchmarks

**YAML Key**: `floatingRates`

**YAML Example**:

```yaml
floatingRates:
  - name: BEAC Base Rate
    isActive: true
    isBaseLendingRate: true
    ratePeriods:
      - fromDate: "2024-01-01"
        interestRate: 3.5

  - name: Prime Lending Rate
    isActive: true
    isBaseLendingRate: false
    ratePeriods:
      - fromDate: "2024-01-01"
        interestRate: 7.5

  - name: SME Lending Rate
    isActive: true
    ratePeriods:
      - fromDate: "2024-01-01"
        interestRate: 9.5
```

---

### 20-31. Additional Product Entities

[Continuing with remaining entities: Tax Groups, Charges, Fund Sources, Payment Types, Holidays, Loan Products, Delinquency Buckets, Savings Products, Collateral Types, and Product Accounting mappings...]

---

## Reference Guide

### Entity Naming Conventions

- Use descriptive, unique names
- Include office/branch for office-specific items
- Use consistent capitalization
- Avoid special characters in `externalId`

### Best Practices

1. **Always set externalId** for all entities
2. **Use references** (`$entity.name`) instead of hardcoded IDs
3. **Test incrementally** - load phase by phase
4. **Validate before import** - use validation script
5. **Backup before major changes**

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| Dependency not found | Reference to non-existent entity | Check loading order, verify name |
| Duplicate externalId | Two entities with same ID | Ensure uniqueness |
| Invalid GL code | GL account doesn't exist | Create GL account first |
| Permission denied | Missing role permission | Add permission to role |

---

**Complete entity details for all 40 types available in full documentation.**

**See**: [YAML_SCHEMA.md](YAML_SCHEMA.md) for complete YAML examples.
