# Fineract Demo Data Generator

Production-ready demo data generation system for Apache Fineract, specifically configured for Cameroon OHADA-compliant microfinance operations.

## Overview

This toolkit provides a two-step process to populate a Fineract instance with comprehensive demo data:

1. **Excel Generation**: Creates an Excel template with pre-configured demo data that can be modified
2. **API Loading**: Reads the Excel file and loads data into Fineract via REST APIs

## Features

### Comprehensive Demo Data Coverage

- **4 Offices**: Head office + 3 branches (Yaounde, Douala, Bafoussam, Bamenda)
- **12 Staff Members**: Branch managers, loan officers, and cashiers with user accounts
- **12 Sample Clients**: Complete profiles with KYC information
- **3 Loan Products**: Microcredit, SME Business Loan, Agricultural Seasonal Loan
- **3 Savings Products**: Voluntary Savings, Fixed Deposit, Mandatory Group Savings
- **11 Charges/Fees**: Loan processing fees, late penalties, account fees
- **30 GL Accounts**: OHADA-compliant chart of accounts
- **6 Fund Sources**: Capital tracking (own capital, bank loans, donors)
- **5 Payment Types**: Cash, MTN Mobile Money, Orange Money, Bank Transfer, Cheque
- **12 Public Holidays**: Cameroon holidays for 2024-2025
- **5 Loan Provisioning Categories**: COBAC-compliant (0%, 25%, 50%, 75%, 100%)
- **6 Collateral Types**: Land, vehicles, household goods, equipment, etc.
- **6 Guarantor Types**: Spouse, family, employer, business partner, etc.
- **3 Floating Rates**: BEAC Base Rate (3.5%), Prime Lending (7.5%), SME Lending (9.5%)
- **5 Delinquency Buckets**: Arrears classification from 1-30 days to 180+ days
- **2 Tax Groups**: Savings Interest WHT (15%), Loan Interest WHT (5.5%)
- **Sample Accounts**: 6 active loan accounts + 12 savings accounts
- **Role-Based Access Control**: 3 roles with granular permissions (Branch Manager, Loan Officer, Cashier)
- **Maker-Checker Configuration**: 12 dual-authorization rules for critical operations
- **Currency Configuration**: XAF (Central African CFA Franc) with proper decimal handling
- **Working Days**: Monday-Friday working days configuration
- **Account Number Preferences**: Automatic account number generation for 5 entity types
- **Codes & Code Values**: 43 dropdown values across 9 code categories
- **Scheduler Jobs**: 10 automated cron jobs for routine operations
- **Global Configuration**: 23 comprehensive system settings
- **SMS/Email Configuration**: Twilio & Gmail SMTP setup with 17 configuration items
- **Notification Templates**: 16 SMS/Email templates for all client lifecycle events
- **Data Tables (Custom Fields)**: 24 custom fields for Client, Loan, and Savings entities

### Production-Ready Configuration

- OHADA accounting standards compliance
- COBAC loan provisioning rules
- Inter-branch transaction support
- Tax configuration (15% WHT on savings interest)
- Mobile money integration ready (MTN MoMo, Orange Money)
- Regulatory compliance features (KYC, provisioning, write-offs)
- Role-based access control with segregation of duties
- Maker-Checker (4-eyes principle) for fraud prevention
- Complete accounting integration with Financial Activity Mappings
- Teller/cashier cash management configuration

## Folder Structure

```
fineract-demo-data/
├── README.md                         # Quick start guide (this file)
├── DOCUMENTATION.md                  # Technical documentation and troubleshooting
├── config/
│   └── fineract_config.json          # Fineract API connection settings
├── scripts/
│   ├── generate_excel_template.py    # Excel generator script
│   ├── load_demo_data.py             # API loader script
│   ├── fineract_client.py            # API client wrapper
│   └── loaders/                      # Specialized loader modules
│       ├── system_config.py
│       ├── entities.py
│       ├── products.py
│       ├── accounts.py
│       └── roles_permissions.py
├── output/
│   └── fineract_demo_data_*.xlsx     # Generated Excel files
└── logs/
    └── load_demo_data.log            # Execution logs
```

## Documentation

- **README.md** (this file): Quick start guide with usage instructions
- **DOCUMENTATION.md**: Comprehensive technical documentation including:
  - Architecture and loader implementation details
  - Complete Excel sheets reference (36 sheets)
  - Loading sequence and dependencies
  - Recent enhancements and fixes
  - Configuration options
  - Troubleshooting guide
  - API reference
- **STAFF_TRAINING_GUIDE.md**: End-user training manual for microfinance staff including:
  - Role-specific guides (Branch Manager, Loan Officer, Cashier)
  - Daily workflows and procedures
  - Step-by-step transaction processing
  - Maker-Checker approval workflows
  - Inter-branch transaction handling
  - Security best practices and fraud prevention
  - Common troubleshooting scenarios

## Prerequisites

### System Requirements

- Python 3.8 or higher
- Apache Fineract 1.8.0 or higher (running and accessible)
- Internet connection (for initial package installation)

### Python Dependencies

Install required packages:

```bash
pip install -r requirements.txt
```

Dependencies include:
- pandas
- openpyxl
- requests

## Quick Start

### Step 1: Generate Excel Template

```bash
cd scripts
python3 generate_excel_template.py
```

This creates an Excel file in the `output/` directory with timestamp:
- `fineract_demo_data_YYYYMMDD_HHMMSS.xlsx`

**Optional**: Open the Excel file and customize the data:
- Modify client names, phone numbers, addresses
- Adjust loan and savings product parameters
- Change charge amounts
- Update GL account names
- Modify any other data as needed

### Step 2: Configure Fineract Connection

Edit `config/fineract_config.json`:

```json
{
  "fineract_url": "http://localhost:8080/fineract-provider/api/v1",
  "username": "mifos",
  "password": "password",
  "tenant": "default",
  "default_password": "password"
}
```

Update these values:
- `fineract_url`: Your Fineract API endpoint
- `username`: Fineract admin username
- `password`: Fineract admin password
- `tenant`: Tenant identifier (usually "default")
- `default_password`: Password for created user accounts

### Step 3: Load Data into Fineract

```bash
cd scripts
python3 load_demo_data.py ../output/fineract_demo_data_YYYYMMDD_HHMMSS.xlsx
```

Or specify a custom config file:

```bash
python3 load_demo_data.py ../output/fineract_demo_data_YYYYMMDD_HHMMSS.xlsx ../config/custom_config.json
```

## Excel Sheets Explained

### Accounting Integration

**IMPORTANT**: This toolkit now includes **complete accounting mappings** to ensure all financial transactions are properly recorded in the general ledger. The system uses **Cash-based accounting** with comprehensive GL account mappings for:

- **Loan Products**: 13 GL mappings per product (39 total)
- **Savings Products**: 11 GL mappings per product (33 total)
- **Payment Types**: GL account for each payment channel (5 total)
- **Financial Activities**: Core system activities (6 mappings)

All loan and savings products are created with proper accounting rules, ensuring:
- Loan disbursements debit the loan portfolio and credit the fund source
- Loan repayments properly allocate to principal, interest, fees, and penalties
- Savings deposits and withdrawals update customer liability accounts
- Inter-branch transactions use proper clearing accounts
- All income and expenses are recognized correctly

### 1. Offices
Organizational hierarchy of branches.

**Columns**:
- `office_name`: Name of the office/branch
- `parent_office`: Parent office (empty for head office)
- `opening_date`: Opening date (YYYY-MM-DD)
- `external_id`: Unique identifier
- `address`, `city`, `region`: Location details
- `phone`, `email`: Contact information

### 2. Staff
Staff members with role-based access.

**Columns**:
- `firstname`, `lastname`: Staff names
- `office`: Office assignment
- `role`: Branch Manager, Loan Officer, or Cashier
- `username`: Login username
- `email`: Email address
- `mobile`: Mobile number
- `joining_date`: Employment start date
- `external_id`: Unique identifier

### 3. Clients
Individual microfinance clients.

**Columns**:
- `firstname`, `lastname`: Client names
- `office`: Home branch
- `staff`: Assigned loan officer
- `gender`: Male/Female
- `mobile`, `email`: Contact information
- `date_of_birth`: Birth date
- `client_type`: Individual/Group
- `client_classification`: Active/Pending/Closed
- `external_id`: Unique identifier
- `activation_date`: Account activation date
- `national_id`: National ID number
- `address`, `city`: Location
- `marital_status`: Married/Single/Divorced/Widow
- `number_of_dependents`: Number of dependents
- `occupation`, `business_type`: Business information
- `monthly_income`: Monthly income (XAF)
- `risk_rating`: A (best) to E (worst)

### 4. Loan Products
Loan product definitions.

**Columns**:
- `product_name`: Full product name
- `short_name`: Short code
- `description`: Product description
- `currency`: XAF
- `principal_min/default/max`: Principal amounts
- `number_of_repayments_min/default/max`: Repayment periods
- `repayment_frequency`: Monthly/Weekly
- `interest_rate_min/default/max`: Interest rates (%)
- `interest_calculation_period`: Same as repayment period
- `amortization_type`: Equal installments
- `interest_type`: Declining Balance/Flat
- `grace_on_principal_periods`: Grace periods
- `grace_on_interest_periods`: Interest grace periods
- `processing_fee_percent`: Processing fee (%)
- `insurance_mandatory`: Yes/No
- `insurance_percent`: Insurance rate (%)

### 5. Savings Products
Savings account product definitions.

**Columns**:
- `product_name`, `short_name`: Product names
- `description`: Product description
- `currency`: XAF
- `nominal_annual_interest_rate`: Annual interest (%)
- `interest_compounding_period`: Monthly/Quarterly
- `interest_posting_period`: Monthly/Quarterly
- `interest_calculation_type`: Daily Balance/Average Daily Balance
- `interest_calculation_days_in_year`: 365/360
- `minimum_opening_balance`: Minimum to open
- `minimum_balance`: Minimum ongoing balance
- `overdraft_allowed`: Yes/No
- `withdrawal_fee_for_transfers`: Yes/No
- `allow_dormancy_tracking`: Yes/No
- `days_to_inactive`, `days_to_dormancy`: Dormancy rules
- `withhold_tax`: Tax rate (%)
- `lock_in_period`: Lock-in period (days)

### 6. Charges
Fees and charges.

**Columns**:
- `charge_name`: Charge name
- `charge_type`: Loan/Savings/Client
- `calculation_type`: Flat/Percentage of Amount
- `amount`: Charge amount or percentage
- `currency`: XAF
- `charge_time`: Disbursement/Overdue/Activation/Monthly/etc.
- `active`: Yes/No

### 7. Chart of Accounts
OHADA-compliant general ledger accounts.

**Columns**:
- `gl_code`: GL account code
- `gl_name`: Account name
- `account_type`: Asset/Liability/Equity/Income/Expense
- `classification`: Current Asset/Fixed Asset/etc.
- `usage`: Detail/Header
- `manual_entries`: Yes/No
- `description`: Account description

### 8. Loan Accounts
Sample loan accounts.

**Columns**:
- `client_external_id`: Client identifier (from Clients sheet)
- `product`: Loan product short name
- `submitted_on`, `approved_on`, `disbursed_on`: Dates
- `principal`: Loan amount
- `loan_term`: Number of repayments
- `interest_rate`: Interest rate (%)
- `loan_officer`: Loan officer username
- `fund_source`: Fund source name
- `external_id`: Unique identifier

### 9. Savings Accounts
Sample savings accounts.

**Columns**:
- `client_external_id`: Client identifier
- `product`: Savings product short name
- `submitted_on`, `approved_on`, `activated_on`: Dates
- `initial_deposit`: Initial deposit amount
- `field_officer`: Staff username
- `external_id`: Unique identifier

### 10. Fund Sources
Funding sources for loans.

**Columns**:
- `fund_name`: Fund name
- `external_id`: Unique identifier
- `description`: Fund description

### 11. Payment Types
Payment channels.

**Columns**:
- `payment_type`: Payment type name
- `description`: Description
- `is_cash_payment`: Yes/No
- `order_position`: Display order

### 12. Holidays
Public holidays affecting repayment schedules.

**Columns**:
- `holiday_name`: Holiday name
- `date`: Holiday date (YYYY-MM-DD)
- `repayment_scheduling`: Move to Next Working Day/Same Day

### 13. Loan Provisioning
COBAC loan provisioning criteria.

**Columns**:
- `category_name`: Performing/Watch/Substandard/Doubtful/Loss
- `min_days_overdue`, `max_days_overdue`: Days overdue range
- `provision_percentage`: Provision rate (%)

### 14. Collateral Types
Types of collateral accepted.

**Columns**:
- `collateral_type`: Type name
- `description`: Description
- `requires_valuation`: Yes/No

### 15. Guarantor Types
Types of guarantors.

**Columns**:
- `guarantor_type`: Type name
- `description`: Description

### 16. Configuration
System configuration parameters.

**Columns**:
- `config_key`: Configuration key
- `config_value`: Configuration value
- `category`: General/Accounting/Security/etc.

### 17. Loan Product Accounting
GL account mappings for loan products (39 mappings total - 13 per product).

**Columns**:
- `product_short_name`: Loan product code (MICRO-SOL, SME-BIZ, AGRI-SEASON)
- `mapping_type`: Type of GL mapping (Fund Source, Loan Portfolio, Interest Receivable, Fees Receivable, Penalties Receivable, Transfer in Suspense, Interest Income, Fee Income, Penalty Income, Losses Written Off, Goodwill Credit, Income from Recovery, Over Payment Liability)
- `gl_code`: GL account code
- `gl_name`: GL account name
- `description`: Purpose of this mapping

**Purpose**: Maps each loan product to appropriate GL accounts for complete double-entry accounting. When a loan is disbursed or repaid, these mappings determine which GL accounts are debited and credited.

### 18. Savings Product Accounting
GL account mappings for savings products (33 mappings total - 11 per product).

**Columns**:
- `product_short_name`: Savings product code (VOL-SAV, FIXED-DEP, MAND-GRP)
- `mapping_type`: Type of GL mapping (Savings Reference, Savings Control, Interest on Savings, Income from Fees, Income from Penalties, Overdraft Portfolio Control, Income from Interest, Losses Written Off, Escheat Liability, Withholding Tax, Transfer in Suspense)
- `gl_code`: GL account code
- `gl_name`: GL account name
- `description`: Purpose of this mapping

**Purpose**: Maps each savings product to appropriate GL accounts. Ensures deposits, withdrawals, and interest postings are properly recorded in the general ledger.

### 19. Payment Type Accounting
GL account mappings for payment channels (5 mappings).

**Columns**:
- `payment_type`: Payment channel name (Cash, MTN Mobile Money, Orange Money, Bank Transfer, Cheque)
- `gl_code`: GL account code
- `gl_name`: GL account name
- `fund_source`: Whether this can be used as a fund source (Yes/No)
- `description`: Purpose of this account

**Purpose**: Links each payment channel to its corresponding GL account. When money is received or disbursed, the system knows which account to use.

### 20. Financial Activity Mapping
Core system financial activity mappings (6 mappings).

**Columns**:
- `financial_activity`: Activity name (Asset Transfer, Liability Transfer, Cash at Mainvault, Cash at Teller, Opening Balances Contra, Fund Source)
- `gl_code`: GL account code
- `gl_name`: GL account name
- `description`: Purpose of this mapping

**Purpose**: Maps system-level financial activities to GL accounts for:
- Inter-office transfers
- Teller/vault cash management
- Opening balance entries
- Default fund source for loans

### 21. Teller/Cashier Office Mappings
Configuration for teller cash management per office (4 mappings).

**Columns**:
- `office_name`: Office name (Head Office, Douala, Bafoussam, Bamenda)
- `teller_cash_account`: GL code for teller cash (42)
- `vault_account`: GL code for branch vault (42)
- `cash_shortage_account`: GL code for cash shortages (98)
- `cash_overage_account`: GL code for cash overages (86)

**Purpose**: Configures cash management accounts for teller operations per office.

### 22. Tellers
Physical cash counters created at each branch (4 tellers - 1 per office).

**Columns**:
- `office_name`: Office name (Head Office Yaounde, Douala Branch, Bafoussam Branch, Bamenda Branch)
- `teller_name`: Teller name (e.g., "Teller 1 - Yaounde")
- `description`: Teller description
- `start_date`: Teller start date (YYYY-MM-DD)
- `end_date`: Teller end date (YYYY-MM-DD)
- `status`: Active/Inactive

**Purpose**: Creates actual teller entities (cash counters) at each branch. These are the physical points where cashiers allocate cash at the start of the day and settle cash at the end of the day.

**Important Notes**:
- This sheet creates teller entities via `/tellers` API
- Tellers are office-specific (each branch has its own tellers)
- Branch managers can assign cashiers to tellers for daily cash management
- Tellers enable tracking of:
  - Daily cash allocation to cashiers
  - Cash settlements at end of day
  - Cash shortages and overages per teller
  - Transaction tracking per teller counter
- Teller GL mappings are configured in Sheet 21 (Teller/Cashier Office Mappings)

**Workflow**:
1. **Morning**: Branch manager allocates cash from vault to teller (e.g., 2,000,000 XAF)
2. **During Day**: Cashier assigned to teller processes deposits and withdrawals
3. **Evening**: Cashier settles teller, returns cash to vault
4. **Accounting**: System records any cash shortages (GL 98) or overages (GL 86)

### 23. Roles & Permissions (Reference)
Reference guide for role-based access control configuration (22 permission entries).

**Columns**:
- `role_name`: Role name (Branch Manager, Loan Officer, Cashier)
- `permission_group`: Permission category (Client, Loan, Savings, Teller, Maker-Checker, Report, Accounting)
- `permission`: Permission level (ALL_FUNCTIONS, CREATE_UPDATE_READ, READ, CREATE, APPROVE)
- `description`: Purpose of this permission

**Purpose**: Provides a reference guide for creating roles in Fineract UI. NOT loaded programmatically - must be configured manually via Fineract Admin interface. See [Role-Based Access Control](#role-based-access-control-rbac) section for details.

**Important**: This is a **reference sheet only**. The loader script dynamically assigns users to existing Fineract roles. To use custom roles, create them in Fineract UI first, then update `role_name_mapping` in load_demo_data.py.

### 24. Maker-Checker Configuration
Dual authorization rules for critical operations (12 rules).

**Columns**:
- `task_name`: Task name (Loan Approval, Savings Withdrawal, etc.)
- `entity`: Entity type (Loan, Savings, Client, Accounting, User, Office)
- `action`: Action type (APPROVE, DISBURSE, WITHDRAWAL, CLOSE, CREATE, etc.)
- `threshold_amount`: Amount threshold in XAF (reference only)
- `threshold_currency`: Currency code (XAF)
- `maker_role`: Role that creates tasks (Loan Officer, Cashier, Branch Manager)
- `checker_role`: Role that approves tasks (Branch Manager, Head Office Manager)
- `description`: Purpose and threshold explanation

**Purpose**: Enables 4-eyes principle (Maker-Checker) for critical operations:
- Loan approvals, disbursements, write-offs, rescheduling
- Savings withdrawals and account closures
- Client activation and transfers
- Manual journal entries
- User management
- Inter-office fund transfers

**Important Notes**:
- Fineract enables Maker-Checker globally per permission (not per amount)
- Amount thresholds are **for reference only** and require custom business logic to enforce
- Once enabled, ALL matching operations require approval, not just those above threshold
- See [Maker-Checker (Dual Authorization)](#maker-checker-dual-authorization) section for implementation details

### 25. Currency Configuration
Currency configuration for the system.

**Columns**:
- `currency_code`: ISO 4217 currency code (XAF)
- `currency_name`: Full currency name (Central African CFA Franc)
- `decimal_places`: Number of decimal places (0 for XAF)
- `in_multiples_of`: Rounding multiple (1 for XAF)
- `display_symbol`: Display symbol (FCFA)
- `name_code`: Localization code (currency.XAF)
- `description`: Currency description

**Purpose**: Configures the primary currency for the system. XAF is typically pre-configured in Fineract, but this sheet documents the currency settings for reference.

### 26. Working Days
Configuration of working days and non-working days.

**Columns**:
- `day_of_week`: Day name (Monday through Sunday)
- `working_day`: Yes/No (Monday-Friday are working days)
- `recurrence_type`: Weekly
- `recurrence_frequency`: 1 (every week)

**Purpose**: Defines which days are working days for the institution. Affects:
- Loan repayment schedules (repayments scheduled on working days)
- Holiday rescheduling logic
- System processing windows
- Staff availability tracking

**Configuration**: Monday through Friday are working days; Saturday and Sunday are non-working days.

### 27. Account Number Preferences
Automatic account number generation formats.

**Columns**:
- `entity_type`: Client/Loan/Savings/Groups/Centers
- `prefix_type`: Office Short Name/Product Short Name
- `account_number_length`: Total length including prefix (9-12 characters)
- `example`: Sample account number (e.g., DLA000001)
- `description`: Format explanation

**Purpose**: Configures automatic account number generation for different entity types:

| Entity | Prefix | Example | Format |
|--------|--------|---------|--------|
| Client | Office Code | DLA000001 | Office + 6 digits |
| Loan | Product Code | MICRO000001 | Product + 6 digits |
| Savings | Product Code | VOL00000001 | Product + 8 digits |
| Groups | Office Code | DLA-GRP-001 | Office + GRP + 3 digits |
| Centers | Office Code | DLA-CTR-001 | Office + CTR + 3 digits |

### 28. Codes and Code Values
Dropdown configurations for various system fields (43 code values across 9 categories).

**Columns**:
- `code_name`: Code category name
- `code_value`: Individual dropdown value
- `code_position`: Display order
- `is_active`: Yes/No (active/inactive)
- `description`: Value description

**Code Categories**:

1. **Gender** (3 values): Male, Female, Other
2. **Client Type** (2 values): Individual, Group
3. **Client Classification** (3 values): Active, Pending, VIP
4. **Marital Status** (4 values): Single, Married, Divorced, Widowed
5. **Education Level** (4 values): None, Primary, Secondary, University
6. **Loan Purpose** (6 values): Working Capital, Equipment, Inventory, Expansion, Emergency, Education
7. **Savings Closure Reason** (5 values): Client Request, Dormant Account, Moved to Another Branch, Deceased, Fraudulent Activity
8. **Loan Closure Reason** (4 values): Fully Paid, Written Off, Rescheduled, Foreclosed
9. **Business Type** (7 values): Retail, Agriculture, Services, Manufacturing, Transport, Restaurant, Other

**Purpose**: Provides standardized dropdown values for data entry, ensuring consistency and enabling proper reporting.

### 29. Scheduler Jobs
Automated cron jobs for routine system operations (10 jobs).

**Columns**:
- `job_name`: Internal job identifier
- `display_name`: User-friendly name
- `cron_expression`: Cron schedule (e.g., '0 0 0 1/1 * ? *' for daily at midnight)
- `active`: Yes/No (enabled/disabled)
- `description`: Job purpose and schedule explanation

**Jobs Configured**:

| Job Name | Schedule | Purpose |
|----------|----------|---------|
| Post Interest For Savings | Daily at midnight | Post accrued interest to savings accounts |
| Apply Charges To Overdue Loans | Daily at 3 AM | Apply penalty charges to overdue loans |
| Update Savings Dormancy Flags | Daily at midnight | Mark inactive/dormant savings accounts |
| Transfer Fee For Loans From Savings | Daily at 1 AM | Auto-deduct loan fees from savings |
| Apply Annual Fee For Savings | Yearly on Jan 1 | Apply annual maintenance fees |
| Apply Holidays To Loans | Daily at 2 AM | Adjust loan schedules for holidays |
| Update Loan Arrears Aging | Daily at 4 AM | Update portfolio at risk (PAR) aging |
| Execute Standing Instructions | Daily at midnight | Process recurring payments |
| Update Loan Paid In Advance | Daily at 5 AM | Track prepayments and advance payments |
| Recalculate Interest For Loans | Daily at 6 AM | Recalculate interest for variable-rate loans |

**Purpose**: Automates critical daily, weekly, and yearly operations to ensure accurate accounting and timely processing.

### 30. Global Configuration
Comprehensive system-wide settings (23 configurations).

**Columns**:
- `config_name`: Configuration key
- `enabled`: Yes/No (enabled/disabled)
- `value`: Configuration value
- `description`: Setting explanation

**Key Configurations**:

| Configuration | Value | Purpose |
|---------------|-------|---------|
| maker-checker | Yes | Enable 4-eyes principle globally |
| allow-transactions-on-non_working_day | No | Block transactions on holidays/weekends |
| accounting-rule | 2 (Cash) | Accounting method (1=None, 2=Cash, 3=Accrual Periodic, 4=Accrual Upfront) |
| minimum-age-for-client-activation | 18 | Minimum age for client accounts |
| reschedule-future-repayments | Yes | Allow rescheduling future repayments |
| allow-backdated-transaction | No | Prevent backdated transactions (fraud prevention) |
| enable-auto-generated-external-id | Yes | Auto-generate external IDs |
| loan-reschedule-is-enabled | Yes | Enable loan rescheduling feature |
| savings-interest-posting-current-period-end | Yes | Post interest at end of current period |
| financial-year-beginning-month | 1 (January) | Financial year start month |
| penalty-wait-period | 3 days | Grace period before penalty charges |
| days-in-year | 365 | Days in year for interest calculations |
| payment-type-applicable-for-disbursement | Cash, Mobile Money | Payment types allowed for disbursements |
| interest-charged-from-date | Disbursement Date | When interest calculation starts |
| loan-schedule-type | Installment | Loan schedule type |
| allow-partial-period-interest-calculation | Yes | Calculate partial period interest |
| enable-business-date | No | Use business date (for banks) vs system date |
| enable-post-reversal-txns | Yes | Allow reversing posted transactions |
| enable-address | Yes | Enable client address fields |
| enable-savings-interest-earned | Yes | Track interest earned on savings |
| allow-loan-charges-adjustment | Yes | Allow adjusting loan charges after disbursement |
| grace-on-penalty-posting | No | No grace period on penalty posting |
| purge-external-events-older-than-days | 90 | Delete old external events after 90 days |
| sub-rates | No | Disable sub-rates feature |

**Purpose**: Controls system-wide behavior, accounting rules, business rules, and feature toggles.

### 31. SMS/Email Configuration
SMS gateway and email SMTP configuration (17 configuration items).

**Columns**:
- `config_type`: SMS Gateway / Email SMTP / Notification Settings
- `provider`: Twilio / Infobip / Gmail / System
- `config_key`: Configuration parameter name
- `config_value`: Parameter value
- `is_active`: Yes/No (enabled/disabled)
- `description`: Configuration description

**Providers Configured**:

| Provider | Type | Configuration Items |
|----------|------|---------------------|
| **Twilio** | SMS Gateway | Account SID, Auth Token, From Phone Number |
| **Infobip** | SMS Gateway (Alternative) | Base URL, API Key, Sender ID |
| **Gmail** | Email SMTP | Host, Port (587/TLS), Username, Password, From Email, From Name, Use TLS |
| **System** | Notification Settings | Enable SMS/Email, Max Retry Attempts, Retry Delay |

**Purpose**: Provides reference configuration for SMS and Email notification services. These settings must be configured manually in Fineract's notification service or via external message gateway.

**Important Notes**:
- SMS notifications require external gateway setup (Twilio or Infobip account)
- Email requires SMTP server access (Gmail requires app password, not regular password)
- Configuration is **reference only** - must be set up in Fineract notification service
- See Fineract Notification Service documentation for integration details

### 32. Notification Templates
SMS and Email templates for client lifecycle events (16 templates).

**Columns**:
- `template_name`: Template name (e.g., "Loan Approval", "Savings Deposit")
- `channel`: SMS / Email
- `event_trigger`: Event that triggers notification
- `subject`: Email subject line (empty for SMS)
- `message_body`: Template message with placeholders
- `is_active`: Yes/No
- `description`: Template purpose

**Templates by Category**:

1. **Client Notifications** (2 templates):
   - Client Activation (SMS + Email)

2. **Loan Notifications** (6 templates):
   - Loan Approval (SMS)
   - Loan Disbursement (SMS)
   - Loan Repayment Due - 3 day reminder (SMS)
   - Loan Repayment Overdue (SMS)
   - Loan Repayment Received (SMS)
   - Loan Fully Repaid (SMS)

3. **Savings Notifications** (6 templates):
   - Savings Account Activation (SMS)
   - Savings Deposit (SMS)
   - Savings Withdrawal (SMS)
   - Interest Posted (SMS)
   - Low Balance Alert (SMS)
   - Account Dormancy Warning (SMS)

4. **Security Notifications** (2 templates):
   - Password Changed (Email)
   - Failed Login Attempts (Email)

**Template Placeholders**:
Templates use dynamic placeholders that are replaced with actual data:
- `{clientName}`, `{officeName}`, `{institutionName}`
- `{loanAmount}`, `{loanAccountNumber}`, `{repaymentAmount}`, `{repaymentDate}`
- `{savingsAccountNumber}`, `{accountBalance}`, `{depositAmount}`
- `{userName}`, `{changeDate}`, `{ipAddress}`

**Purpose**: Provides ready-to-use notification templates for all major client lifecycle events. Templates must be configured in Fineract Admin UI → System → Notification Templates.

**Important Notes**:
- Templates are **reference only** - must be created in Fineract UI
- SMS templates should be under 160 characters for single SMS
- Email templates support multi-line formatting with `\n`
- Test templates before enabling in production

### 33. Data Tables (Custom Fields)
Custom field definitions for extending core entities (24 custom fields across 3 entity types).

**Columns**:
- `entity_type`: Client / Loan / Savings
- `table_name`: Data table name (e.g., "client_additional_info")
- `field_name`: Field name (database column name)
- `field_type`: String / Number / Decimal / Date / Datetime / Text / Dropdown
- `mandatory`: Yes/No (required field)
- `dropdown_values`: Comma-separated values for dropdown fields
- `description`: Field purpose

**Custom Fields by Entity**:

**Client Custom Fields** (12 fields in `client_additional_info` table):
| Field Name | Type | Mandatory | Purpose |
|------------|------|-----------|---------|
| id_type | Dropdown | Yes | National ID, Passport, Voter Card, Driver License |
| id_number | String | Yes | ID document number |
| id_expiry_date | Date | No | ID expiry date |
| next_of_kin_name | String | Yes | Next of kin full name |
| next_of_kin_phone | String | Yes | Next of kin contact |
| next_of_kin_relationship | Dropdown | Yes | Spouse, Parent, Sibling, Child, Friend, Other |
| employer_name | String | No | Employer (for salaried clients) |
| employer_phone | String | No | Employer contact |
| years_in_business | Number | No | Business experience (for self-employed) |
| business_location | String | No | Business physical location |
| home_ownership | Dropdown | No | Owned, Rented, Family Home, Other |
| disability_status | Dropdown | No | None, Physical, Visual, Hearing, Other |

**Loan Custom Fields** (7 fields in `loan_additional_info` table):
| Field Name | Type | Mandatory | Purpose |
|------------|------|-----------|---------|
| loan_purpose_detail | Text | Yes | Detailed loan purpose description |
| collateral_description | Text | No | Collateral details |
| collateral_value | Decimal | No | Collateral value (XAF) |
| guarantor_count | Number | No | Number of guarantors |
| credit_score | Number | No | Credit score (0-100) |
| repayment_source | Dropdown | Yes | Business Income, Salary, Remittances, Other |
| previous_loan_history | Dropdown | No | First Loan, Good History, Some Delays, Defaulted Before |

**Savings Custom Fields** (5 fields in `savings_additional_info` table):
| Field Name | Type | Mandatory | Purpose |
|------------|------|-----------|---------|
| savings_goal | String | No | Client's savings goal |
| target_amount | Decimal | No | Target savings amount (XAF) |
| target_date | Date | No | Target date to reach goal |
| monthly_commitment | Decimal | No | Monthly savings commitment |
| preferred_transaction_channel | Dropdown | No | Branch, Mobile Money, Bank Transfer, Agent |

**Purpose**: Extends core Fineract entities with custom fields for additional KYC data, business information, and client preferences. Data tables are created via Fineract API and automatically appear in entity forms.

**Important Notes**:
- Data tables are created programmatically via `/datatables` API
- Custom fields appear in Fineract UI under entity's "Additional Info" tab
- Dropdown fields require corresponding codes to be created first
- Mandatory fields must be filled when creating/editing entities
- Data tables support single-row (one record per entity) or multi-row data

## Loading Process

The data is loaded in the following order to respect dependencies:

### Phase 1: System Configuration (Core Settings)
1. **Currency Configuration** - Configure XAF currency settings
2. **Working Days** - Define Monday-Friday as working days
3. **Global Configuration** - Set 23 system-wide settings (maker-checker, accounting rules, etc.)
4. **Codes and Code Values** - Create dropdown configurations (43 values across 9 categories)
5. **Account Number Preferences** - Configure automatic account number generation
6. **SMS/Email Configuration** - Reference configuration for Twilio & Gmail SMTP (manual setup required)
7. **Notification Templates** - Reference templates for SMS/Email notifications (manual setup required)
8. **Data Tables** - Create custom fields for Client, Loan, and Savings entities

### Phase 2: Organizational Structure
9. **Offices** - Must exist before staff and clients
10. **Staff** - Required for client assignment and loan officers
11. **GL Accounts** - Foundation for accounting
12. **Charges** - Referenced by products
13. **Fund Sources** - Referenced by loans
14. **Payment Types** - Used for transactions
15. **Holidays** - Affects repayment schedules

### Phase 3: System Integration
16. **Financial Activity Mappings** - CRITICAL for inter-branch operations
17. **Maker-Checker Configuration** - Enables dual authorization for critical operations
18. **Scheduler Jobs** - Configure 10 automated cron jobs

### Phase 4: Products and Clients
19. **Loan Products** - Required before creating loan accounts
20. **Savings Products** - Required before creating savings accounts
21. **Clients** - Must exist before accounts

### Phase 5: Accounts and Transactions
22. **Savings Accounts** - Can be created independently
23. **Loan Accounts** - Created last

**Important Notes**:
- **System Configuration** (Phase 1) must be loaded first to establish core system behavior
- **Financial Activity Mappings** must be configured after GL accounts are created but before products are created to ensure inter-branch transactions work correctly
- **Maker-Checker** is enabled before products are created to ensure dual authorization is enforced from the start
- **Scheduler Jobs** are configured after system settings to ensure proper cron schedules
- **Roles & Permissions** (Sheet 22) is a reference guide only and NOT loaded programmatically - create roles manually in Fineract UI

## Inter-Branch Operations

This toolkit provides complete support for inter-branch/cross-branch operations, which is essential for multi-branch microfinance institutions.

### Overview

Fineract's inter-branch functionality allows tracking transactions across different offices while maintaining proper accounting separation. The system uses:

1. **Office-Level Tracking**: Fineract automatically tags all transactions with the office ID
2. **Clearing Accounts**: Special GL accounts for inter-branch reconciliation
3. **Financial Activity Mappings**: System-level configuration for transfer accounting

### Key GL Accounts for Inter-Branch Operations

The toolkit creates two critical clearing accounts:

- **GL 122 - Due from Other Branches (Asset)**: Records amounts receivable from other branches
- **GL 131 - Due to Other Branches (Liability)**: Records amounts payable to other branches

### Financial Activity Mappings Explained

These mappings tell Fineract which GL accounts to use for specific system operations:

| Financial Activity | GL Account | Purpose |
|-------------------|------------|---------|
| **Asset Transfer** | GL 122 (Due from Other Branches) | When a branch transfers assets (e.g., loan ownership, cash) to another branch, the receiving branch records a receivable |
| **Liability Transfer** | GL 131 (Due to Other Branches) | When a branch transfers liabilities (e.g., client deposits) to another branch, the sending branch records a payable |
| **Cash at Mainvault** | GL 42 (Cash on Hand) | Primary cash account for branch vault |
| **Cash at Teller** | GL 42 (Cash on Hand) | Teller cash positions |
| **Opening Balances Contra** | GL 30 (Capital) | Balancing account for initial setup |
| **Fund Source** | GL 41 (Bank Accounts) | Default funding source for loans |

### Example: Inter-Branch Loan Transfer

**Scenario**: A client moves from Yaounde branch to Douala branch with an active loan.

**Accounting Entries - Yaounde (Sending Branch)**:
```
DR GL 131 (Due to Other Branches)    500,000 XAF
CR GL 52 (Loan Portfolio)            500,000 XAF
```

**Accounting Entries - Douala (Receiving Branch)**:
```
DR GL 52 (Loan Portfolio)            500,000 XAF
CR GL 122 (Due from Other Branches)  500,000 XAF
```

**Result**: The loan is now tracked under Douala's portfolio, and the inter-branch balance is recorded for later reconciliation.

### Example: Inter-Branch Cash Transfer

**Scenario**: Head Office sends 1,000,000 XAF to Bamenda branch.

**Accounting Entries - Head Office (Sending)**:
```
DR GL 122 (Due from Other Branches)  1,000,000 XAF
CR GL 42 (Cash on Hand)              1,000,000 XAF
```

**Accounting Entries - Bamenda (Receiving)**:
```
DR GL 42 (Cash on Hand)              1,000,000 XAF
CR GL 131 (Due to Other Branches)    1,000,000 XAF
```

### Office-Level Reporting

With this configuration, you can generate:

1. **Branch-Level Balance Sheets**: Each office's assets, liabilities, and equity
2. **Inter-Branch Reconciliation Reports**: Outstanding balances between branches
3. **Consolidated Reports**: Organization-wide financial statements
4. **Branch Performance Analysis**: Profitability and portfolio quality by branch

### Teller/Cashier Configuration

Sheet 21 (Teller/Cashier Mappings) configures cash management per office:

- **Teller Cash Account**: GL 42 (Cash on Hand) - used across all offices
- **Vault Account**: GL 42 (Cash on Hand) - main branch vault
- **Cash Shortage**: GL 98 (Cash Shortage) - expense when teller is short
- **Cash Overage**: GL 86 (Cash Overage) - income when teller has excess

**Important**: The system uses **single GL codes** across all offices. Office-level differentiation is handled by Fineract's built-in office tagging, not by creating separate GL accounts per office (e.g., NOT GL 42-YDE, GL 42-DLA).

### Best Practices for Inter-Branch Operations

1. **Regular Reconciliation**: Reconcile inter-branch accounts (GL 122 and GL 131) monthly
2. **Balanced Transfers**: Ensure total debits to GL 122 equal total credits to GL 131 across all branches
3. **Audit Trail**: All inter-branch transactions are logged with source and destination office IDs
4. **Access Control**: Limit inter-branch transfer permissions to branch managers
5. **Documentation**: Require proper documentation for all inter-branch movements

### Verifying Inter-Branch Setup

After loading data, verify Financial Activity Mappings were created correctly:

```bash
# Check Financial Activity Mappings
curl -u mifos:password \
  http://localhost:8080/fineract-provider/api/v1/financialactivityaccounts | jq

# You should see mappings for:
# - Asset Transfer (100) → GL 122
# - Liability Transfer (200) → GL 131
# - Cash at Mainvault (101) → GL 42
# - Cash at Teller (102) → GL 42
```

### Troubleshooting Inter-Branch Operations

**Problem**: Inter-branch transfers fail with "Financial activity mapping not found"

**Solution**:
- Verify Financial Activity Mappings were loaded (check logs)
- Ensure GL accounts 122 and 131 were created before mappings
- Re-run the loader if mappings are missing

**Problem**: GL 122 and GL 131 don't balance

**Solution**:
- Generate inter-branch reconciliation report
- Review all transfer transactions
- Identify and correct any unmatched entries

## Inter-Branch Transactions (Cross-Branch Client Access)

### Problem Statement

**Business Requirement**: A client from Branch Douala travels to Branch Yaounde and needs to make a deposit or withdrawal. How can the cashier at Branch Yaounde process the transaction?

### Fineract's Architecture: Office Hierarchy Scoping

Fineract's security model enforces **office hierarchy-based access control**:

- Users can ONLY see clients/accounts in their assigned office and child offices (downward in hierarchy)
- Sibling branches CANNOT see each other's clients through the client search interface
- This is hard-coded in Fineract's core code for security and data segregation

**Example Hierarchy**:
```
.1.         Head Office (can see ALL branches)
.1.1.       Branch Yaounde (can see only Yaounde clients)
.1.2.       Branch Douala (can see only Douala clients)
.1.3.       Branch Bafoussam (can see only Bafoussam clients)
```

**Result**: A cashier at Branch Yaounde **cannot search** for clients from Branch Douala using the client search interface (`GET /clients`).

### The Solution: Direct Account Access

Fineract is intentionally designed to support inter-branch transactions WITHOUT requiring cross-branch client visibility. The key insight: **Cashiers don't need to search for clients; they need to access accounts directly**.

#### Workflow for Inter-Branch Transactions

**Step 1: Client Provides Account Number**
- Clients receive a unique account number when their savings account is created
- Account numbers are **globally unique across all branches**
- Examples: `VOL00000001`, `DLA000001` (see Sheet 26: Account Number Preferences)

**Step 2: Cashier Searches by Account Number**
The cashier uses the direct account lookup APIs:

```bash
# Search for savings account by account number (works across all branches)
curl -u cashier.yaounde:password \
  "https://localhost/fineract-provider/api/v1/savingsaccounts?accountNo=VOL00000001" | jq

# Returns the savings account details regardless of client's home branch
```

**Step 3: Process Transaction**
Once the account is found, the cashier processes the transaction:

```bash
# Make deposit
curl -X POST -u cashier.yaounde:password \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "2024-10-10",
    "transactionAmount": "50000",
    "paymentTypeId": 1,
    "locale": "en",
    "dateFormat": "yyyy-MM-dd"
  }' \
  "https://localhost/fineract-provider/api/v1/savingsaccounts/{accountId}/transactions?command=deposit"

# Make withdrawal
curl -X POST -u cashier.yaounde:password \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "2024-10-10",
    "transactionAmount": "30000",
    "paymentTypeId": 1,
    "locale": "en",
    "dateFormat": "yyyy-MM-dd"
  }' \
  "https://localhost/fineract-provider/api/v1/savingsaccounts/{accountId}/transactions?command=withdrawal"
```

**Step 4: Automatic Inter-Branch Accounting**
Fineract automatically handles the inter-branch GL accounting:

**Branch Douala (Client's Home Branch)**:
```
DR GL 131 (Due to Other Branches)    50,000 XAF
CR GL 32 (Customer Deposits)         50,000 XAF
```

**Branch Yaounde (Transaction Branch)**:
```
DR GL 42 (Cash on Hand)               50,000 XAF
CR GL 122 (Due from Other Branches)   50,000 XAF
```

### Alternative Identifiers for Account Lookup

Clients can be identified by multiple unique identifiers:

| Identifier | API Parameter | Example | Use Case |
|------------|--------------|---------|----------|
| **Account Number** | `accountNo` | VOL00000001 | Primary method - printed on account statement |
| **External ID** | `externalId` | CLI-DLA-001 | Client's unique external reference |
| **Mobile Number** | `mobileNo` | +237670123456 | For mobile banking integration |
| **Client Account No** | `clientAccountNo` | DLA000001 | Client's master account number |

```bash
# Search by mobile number
curl -u cashier.yaounde:password \
  "https://localhost/fineract-provider/api/v1/clients?mobileNo=%2B237670123456" | jq

# Search by external ID
curl -u cashier.yaounde:password \
  "https://localhost/fineract-provider/api/v1/clients?externalId=CLI-DLA-001" | jq

# Then get client's savings accounts
curl -u cashier.yaounde:password \
  "https://localhost/fineract-provider/api/v1/clients/{clientId}/accounts" | jq
```

### Loan Repayments Across Branches

The same principle applies to loan repayments:

```bash
# Search for loan by account number
curl -u cashier.yaounde:password \
  "https://localhost/fineract-provider/api/v1/loans?accountNo=MICRO000001" | jq

# Process repayment
curl -X POST -u cashier.yaounde:password \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "2024-10-10",
    "transactionAmount": "100000",
    "paymentTypeId": 1,
    "locale": "en",
    "dateFormat": "yyyy-MM-dd"
  }' \
  "https://localhost/fineract-provider/api/v1/loans/{loanId}/transactions?command=repayment"
```

### Security Considerations

**Access Control**:
- Cashiers have `READ` permission on clients (can view but not modify)
- Cashiers have `DEPOSIT_WITHDRAWAL` permission on savings accounts
- Cashiers have `READ_REPAYMENT` permission on loans
- Direct account access requires knowing the account number (not guessable)

**Audit Trail**:
- All transactions are logged with:
  - User who processed the transaction
  - Office where transaction occurred
  - Timestamp
  - Transaction details
- Inter-branch transactions are flagged in reports for reconciliation

**Maker-Checker for Large Transactions**:
For withdrawals above configured thresholds (e.g., 1,000,000 XAF), Maker-Checker is enforced:
1. Cashier initiates withdrawal (MAKER)
2. Branch Manager approves (CHECKER)
3. Only then does transaction execute

See [Maker-Checker section](#maker-checker-dual-authorization) for details.

### Training Staff for Inter-Branch Transactions

**Cashier Training**:
1. Always request account number or valid ID from client
2. Use account number lookup, NOT client name search
3. Verify client identity before processing transaction
4. Ensure transaction is within daily limits
5. Escalate large withdrawals to manager (Maker-Checker)

**Client Communication**:
1. Educate clients to always carry their account number
2. Provide account number on printed statements
3. Consider issuing account cards with account numbers
4. Promote mobile banking for balance inquiries

### API Reference for Inter-Branch Operations

| Operation | API Endpoint | Permission Required |
|-----------|-------------|---------------------|
| Search savings by account number | `GET /savingsaccounts?accountNo={number}` | READ_SAVINGSACCOUNT |
| Search loan by account number | `GET /loans?accountNo={number}` | READ_LOAN |
| Search client by mobile | `GET /clients?mobileNo={mobile}` | READ_CLIENT |
| Get client accounts | `GET /clients/{clientId}/accounts` | READ_CLIENT |
| Deposit to savings | `POST /savingsaccounts/{id}/transactions?command=deposit` | DEPOSIT_SAVINGSACCOUNT |
| Withdraw from savings | `POST /savingsaccounts/{id}/transactions?command=withdrawal` | WITHDRAWAL_SAVINGSACCOUNT |
| Loan repayment | `POST /loans/{id}/transactions?command=repayment` | REPAYMENT_LOAN |

### Why NOT Allow Cross-Branch Client Search?

Fineract's design intentionally restricts client search to office hierarchy for important reasons:

**Security**:
- Prevents unauthorized data access across branches
- Limits exposure of client data to only relevant staff
- Reduces risk of data breaches

**Data Privacy**:
- Complies with data protection regulations (each branch sees only their clients)
- Minimizes PII (Personally Identifiable Information) exposure
- Supports multi-tenant architectures

**Performance**:
- Office-scoped queries are much faster
- Reduces database load
- Improves UI responsiveness

**Compliance**:
- Many regulators require office-level data segregation
- Audit logs are cleaner with office-scoping
- Easier to implement branch-level access controls

### Workaround: If You MUST Have Cross-Branch Search

If your business absolutely requires cashiers to search clients by name across all branches, the ONLY solution is:

**Assign Cashiers to Head Office**:
```
# In Staff sheet (Sheet 2), change office assignment:
office: Head Office (instead of Branch Douala)
```

**Trade-offs**:
- ✅ Cashiers can now search ALL clients across ALL branches
- ❌ Branch managers can't assign cashiers to tellers (tellers are office-specific)
- ❌ Cashier transactions appear under Head Office in reports (not branch-level)
- ❌ Defeats the purpose of office hierarchy and security model

**Recommendation**: Don't do this. Use the account number lookup method instead.

## Role-Based Access Control (RBAC)

This toolkit provides comprehensive role-based access control configuration to ensure proper segregation of duties and security in your Fineract instance.

### Overview

Fineract's security model uses:
1. **Roles**: Groups of permissions (e.g., Branch Manager, Loan Officer, Cashier)
2. **Permissions**: Granular access rights to specific operations
3. **Users**: Staff members assigned to roles
4. **Maker-Checker**: Dual authorization for critical operations (4-eyes principle)

### Sheet 22: Roles & Permissions Reference

This sheet provides a **reference guide** for configuring roles in Fineract. Due to Fineract's complex permission system (100+ permissions), roles are NOT created programmatically. Instead:

1. **Reference Sheet**: Shows recommended permissions for each role
2. **Manual Setup**: Create roles in Fineract UI with these permissions
3. **Dynamic Assignment**: Loader automatically assigns users to existing Fineract roles

**Roles Included**:

| Role | Permission Groups | Key Capabilities | Maker-Checker Role |
|------|------------------|------------------|-------------------|
| **Branch Manager** | Client (ALL_FUNCTIONS), Loan (ALL_FUNCTIONS), Savings (ALL_FUNCTIONS), Reports (ALL_FUNCTIONS), Accounting (READ) | Full branch operations, Can approve loans, Can view reports, Cannot modify GL accounts directly | **CHECKER** - Can approve tasks |
| **Loan Officer** | Client (CREATE_UPDATE_READ), Loan (CREATE_UPDATE_READ), Savings (READ), Reports (READ) | Create/update clients, Create/process loan applications, View savings accounts, Limited reporting | **MAKER** - Can create tasks |
| **Cashier** | Teller (ALL_FUNCTIONS), Savings (DEPOSIT_WITHDRAWAL), Client (READ), Loan (READ_REPAYMENT) | Process deposits/withdrawals, Accept loan repayments, View client information, Cash management | **MAKER** - Can create tasks |

### Creating Custom Roles in Fineract

**To set up these roles** (must be done via Fineract UI):

1. **Login as Admin** to Fineract
2. Navigate to **Admin → System → Manage Roles**
3. Click **Add Role**
4. **For Branch Manager Role**:
   - Role Name: `Branch Manager`
   - Select permissions:
     - Client: ALL_FUNCTIONS
     - Loan: ALL_FUNCTIONS
     - Savings: ALL_FUNCTIONS
     - Office: ALL_FUNCTIONS
     - Staff: ALL_FUNCTIONS
     - Report: ALL_FUNCTIONS
     - Accounting: READ
     - Maker-Checker: APPROVE (critical for approval workflow)
5. Repeat for **Loan Officer** and **Cashier** roles

### How Roles Are Assigned

The loader script dynamically assigns roles using this process:

```python
# From load_demo_data.py lines 212-256
role_name_mapping = {
    'Branch Manager': 'Super user',  # Maps to Fineract role
    'Loan Officer': 'Super user',     # Create custom role in Fineract UI
    'Cashier': 'Super user'           # Create custom role in Fineract UI
}
```

**Default Behavior**:
- Fetches available roles from Fineract via `/roles` API
- Maps staff roles from Excel to Fineract role names
- Defaults to "Super user" if custom roles don't exist
- Logs all available roles for transparency

**Production Recommendation**:
1. Create custom roles in Fineract UI first
2. Update `role_name_mapping` in load_demo_data.py:234
```python
role_name_mapping = {
    'Branch Manager': 'Branch Manager',  # Use your custom role
    'Loan Officer': 'Loan Officer',       # Use your custom role
    'Cashier': 'Cashier'                  # Use your custom role
}
```
3. Run the loader

### Permission Groups Explained

**Common Permission Groups**:

- **ALL_FUNCTIONS**: Create, Read, Update, Delete, Approve
- **CREATE_UPDATE_READ**: Can create new records, modify existing, and view
- **READ**: View-only access
- **CREATE**: Can create new records only
- **APPROVE**: Can approve pending items

**Key Permission Categories**:

| Category | Examples | Used By |
|----------|----------|---------|
| **Client Management** | CREATE_CLIENT, UPDATE_CLIENT, ACTIVATE_CLIENT, PROPOSETRANSFER_CLIENT | Branch Manager, Loan Officer |
| **Loan Operations** | CREATE_LOAN, APPROVE_LOAN, DISBURSE_LOAN, WRITEOFF_LOAN, ADJUST_LOAN | Branch Manager, Loan Officer |
| **Savings Operations** | CREATE_SAVINGSACCOUNT, DEPOSIT_SAVINGSACCOUNT, WITHDRAWAL_SAVINGSACCOUNT, CLOSE_SAVINGSACCOUNT | Branch Manager, Cashier |
| **Teller/Cash** | OPEN_TELLER, ALLOCATE_CASH, SETTLE_CASH | Cashier |
| **Accounting** | CREATE_JOURNALENTRY, READ_JOURNALENTRY, UPDATE_JOURNALENTRY | Branch Manager (read), Accountant (full) |
| **Reporting** | READ_REPORT, READ_ClientListReport, READ_LoansReport | All roles |
| **Maker-Checker** | CHECKER_APPROVE, CHECKER_REJECT, MAKER_CREATE | Branch Manager (APPROVE), Others (CREATE) |

### Verifying Role Assignment

After loading data, verify user roles:

```bash
# Get all users
curl -u mifos:password \
  https://localhost/fineract-provider/api/v1/users | jq

# Get specific user details
curl -u mifos:password \
  https://localhost/fineract-provider/api/v1/users/{userId} | jq
```

Check the logs for role assignment:
```bash
grep "Created user account" logs/load_demo_data.log
grep "Found.*roles in Fineract" logs/load_demo_data.log
```

## Maker-Checker (Dual Authorization)

The toolkit includes comprehensive Maker-Checker configuration to enforce the **4-eyes principle** for critical operations, preventing fraud and ensuring proper oversight.

### Overview

**Maker-Checker Workflow**:
1. **Maker** (e.g., Loan Officer) creates/initiates a transaction
2. Transaction enters **pending approval** state
3. **Checker** (e.g., Branch Manager) reviews and approves/rejects
4. Only after approval does the transaction take effect

This is essential for:
- Large withdrawals and disbursements
- Sensitive client operations
- Accounting entries
- User management
- Inter-branch transfers

### Sheet 23: Maker-Checker Configuration

Defines 12 dual-authorization rules with amount thresholds:

| Task | Entity | Threshold | Maker Role | Checker Role | Description |
|------|--------|-----------|------------|--------------|-------------|
| **Loan Approval** | Loan | 2,000,000 XAF | Loan Officer | Branch Manager | Loans above 2M XAF require manager approval |
| **Loan Disbursement** | Loan | 5,000,000 XAF | Loan Officer | Branch Manager | Disbursements above 5M XAF require manager approval |
| **Loan Write-off** | Loan | 0 XAF (ALL) | Loan Officer | Branch Manager | All loan write-offs require manager approval |
| **Loan Reschedule** | Loan | 1,000,000 XAF | Loan Officer | Branch Manager | Rescheduling loans above 1M XAF requires approval |
| **Savings Withdrawal** | Savings | 1,000,000 XAF | Cashier | Branch Manager | Withdrawals above 1M XAF require manager approval |
| **Savings Account Closure** | Savings | 500,000 XAF | Cashier | Branch Manager | Account closures with balance above 500K XAF require approval |
| **Client Activation** | Client | 0 XAF (ALL) | Loan Officer | Branch Manager | All new client activations require manager approval |
| **Client Transfer** | Client | 0 XAF (ALL) | Loan Officer | Branch Manager | All client transfers between branches require approval |
| **Manual Journal Entry** | Accounting | 500,000 XAF | Accountant | Branch Manager | Manual journal entries above 500K XAF require approval |
| **Create User** | User | 0 XAF (ALL) | Branch Manager | Head Office Manager | All new user accounts require head office approval |
| **Update User Roles** | User | 0 XAF (ALL) | Branch Manager | Head Office Manager | All role changes require head office approval |
| **Inter-Office Transfer** | Office | 3,000,000 XAF | Branch Manager | Head Office Manager | Inter-branch fund transfers above 3M XAF require head office approval |

### How Maker-Checker Is Enabled

The loader script enables Maker-Checker via the `/makercheckers` API:

```python
# From load_demo_data.py lines 661-741
entity_permission_mapping = {
    ('Loan', 'APPROVE'): 'APPROVE_LOAN',
    ('Loan', 'DISBURSE'): 'DISBURSE_LOAN',
    ('Loan', 'WRITEOFF'): 'WRITEOFF_LOAN',
    ('Savings', 'WITHDRAWAL'): 'WITHDRAWAL_SAVINGSACCOUNT',
    ('Savings', 'CLOSE'): 'CLOSE_SAVINGSACCOUNT',
    ('Client', 'ACTIVATE'): 'ACTIVATE_CLIENT',
    ('Accounting', 'CREATE_JOURNAL'): 'CREATE_JOURNALENTRY',
    ('User', 'CREATE'): 'CREATE_USER',
    ('Office', 'TRANSFER_FUNDS'): 'CREATE_ACCOUNTTRANSFER',
    # ... etc
}
```

**Important Notes**:
- Fineract enables Maker-Checker **globally per permission**
- Amount thresholds in Excel are **for reference only**
- Actual threshold enforcement requires **custom business logic** layer
- All operations matching the permission will require approval once enabled

### Maker-Checker Workflow Example

**Scenario**: Cashier processes 1,500,000 XAF withdrawal

1. **Cashier (Maker)** initiates withdrawal in Fineract UI
2. System creates **pending approval task** (not executed yet)
3. **Branch Manager (Checker)** logs in and sees pending tasks
4. Manager reviews:
   - Client account balance
   - Withdrawal reason
   - Supporting documentation
5. Manager **approves** or **rejects**:
   - **If Approved**: Withdrawal executes, cash disbursed
   - **If Rejected**: Transaction cancelled, client notified

### Verifying Maker-Checker Setup

After loading data:

```bash
# Check enabled Maker-Checker permissions
curl -u mifos:password \
  https://localhost/fineract-provider/api/v1/makercheckers | jq

# View pending Maker-Checker tasks
curl -u mifos:password \
  https://localhost/fineract-provider/api/v1/makercheckers?command=retrieveAll | jq
```

Check the logs:
```bash
grep "Enabled Maker-Checker" logs/load_demo_data.log
grep "Maker-Checker enabled:" logs/load_demo_data.log
```

### Approving/Rejecting Tasks in Fineract

**Via Fineract UI**:
1. Login as user with CHECKER role (e.g., Branch Manager)
2. Navigate to **Admin → System → Checker Inbox & Tasks**
3. View pending tasks with details:
   - Maker name
   - Operation type
   - Timestamp
   - Affected entity
4. Click task to review details
5. Click **Approve** or **Reject** with optional notes

**Via API**:
```bash
# Approve a task
curl -X POST -u mifos:password \
  -H "Content-Type: application/json" \
  https://localhost/fineract-provider/api/v1/makercheckers/{taskId}?command=approve

# Reject a task
curl -X POST -u mifos:password \
  -H "Content-Type: application/json" \
  -d '{"note":"Insufficient documentation"}' \
  https://localhost/fineract-provider/api/v1/makercheckers/{taskId}?command=reject
```

### Customizing Maker-Checker Rules

**To modify thresholds or add new rules**:

1. Generate Excel template
2. Open `output/fineract_demo_data_*.xlsx`
3. Navigate to **Sheet 23: Maker Checker Config**
4. Modify thresholds or add rows:
   ```
   task_name            | entity    | action       | threshold_amount | maker_role    | checker_role
   Large Deposit        | Savings   | DEPOSIT      | 5000000         | Cashier       | Branch Manager
   ```
5. Save and reload

**To disable Maker-Checker for specific operations**:
- Remove the row from Sheet 23 before loading
- Or delete via Fineract API after loading

### Amount Threshold Limitation

**Important**: Fineract's native Maker-Checker does NOT support amount-based thresholds. When you enable Maker-Checker for `WITHDRAWAL_SAVINGSACCOUNT`, **ALL withdrawals** require approval, not just those above 1M XAF.

**Workarounds**:
1. **Business Process Layer**: Implement amount checking in custom middleware
2. **UI Customization**: Modify Fineract UI to only create checker tasks above threshold
3. **Manual Process**: Train staff to only escalate large transactions
4. **API Wrapper**: Create API wrapper that checks amount before forwarding to Fineract

**Reference Implementation** (conceptual):
```python
def process_withdrawal(amount, account_id):
    THRESHOLD = 1_000_000  # 1M XAF

    if amount >= THRESHOLD:
        # Create as Maker-Checker task
        return create_checker_task('WITHDRAWAL', account_id, amount)
    else:
        # Execute directly
        return execute_withdrawal(account_id, amount)
```

### Best Practices for Maker-Checker

1. **Clear Segregation**: Makers cannot approve their own tasks
2. **Documentation**: Require supporting documents for approval
3. **Audit Trail**: All approvals/rejections are logged with user and timestamp
4. **Training**: Train checkers on approval criteria and fraud indicators
5. **Regular Review**: Monitor checker inbox to prevent backlog
6. **Escalation**: Define escalation process for urgent approvals
7. **Reporting**: Generate weekly reports of approved/rejected tasks
8. **Access Control**: Only authorized users should have CHECKER permissions

### Security Considerations

**DO**:
- ✅ Assign CHECKER role only to senior staff (Branch Managers, Head Office)
- ✅ Assign MAKER role to operational staff (Loan Officers, Cashiers)
- ✅ Enable Maker-Checker for ALL sensitive operations
- ✅ Regularly review checker task logs for suspicious patterns
- ✅ Enforce strong password policies for checker accounts
- ✅ Enable Maker-Checker for user management and role changes
- ✅ Train staff on social engineering and fraud schemes

**DON'T**:
- ❌ Assign both MAKER and CHECKER roles to the same user
- ❌ Share checker account credentials
- ❌ Approve tasks without proper review
- ❌ Disable Maker-Checker for convenience
- ❌ Allow makers to pressure checkers for approval
- ❌ Skip documentation requirements
- ❌ Use Maker-Checker as replacement for proper access control

### Troubleshooting Maker-Checker

**Problem**: Maker-Checker not working, operations execute immediately

**Solution**:
- Verify Maker-Checker was enabled: check logs
- Confirm user has only MAKER permission, not CHECKER
- Check permission mapping: `GET /makercheckers`

**Problem**: No pending tasks in Checker Inbox

**Solution**:
- Verify operations are being created by users with MAKER role
- Check Maker-Checker is enabled for that permission
- Verify checker user has CHECKER_APPROVE permission

**Problem**: User sees "Insufficient permissions" when creating tasks

**Solution**:
- Verify user has base permission (e.g., WITHDRAWAL_SAVINGSACCOUNT)
- Verify user has MAKER role/permission
- Check user's assigned Fineract role includes required permissions

### Maker-Checker Reporting

**Generate Maker-Checker Reports**:

```bash
# Get all pending tasks
curl -u mifos:password \
  "https://localhost/fineract-provider/api/v1/makercheckers?command=retrieveAll" | jq

# Get approved tasks (last 30 days)
curl -u mifos:password \
  "https://localhost/fineract-provider/api/v1/makercheckers?makerDateTimeFrom=2024-09-10&makerDateTimeTo=2024-10-10&status=approved" | jq

# Get rejected tasks
curl -u mifos:password \
  "https://localhost/fineract-provider/api/v1/makercheckers?status=rejected" | jq
```

**Key Metrics to Monitor**:
- Average approval time
- Rejection rate by task type
- Backlog size (pending tasks)
- Checker activity (approvals per checker)
- Peak submission times

## Error Handling

The loader includes comprehensive error handling:

- **Continue on Error**: By default, continues loading remaining records if one fails
- **Detailed Logging**: All operations logged to `logs/load_demo_data.log`
- **Console Output**: Real-time progress display
- **Validation**: Checks for missing dependencies before creating records

## Logging

Logs are stored in `logs/load_demo_data.log` with the following information:

- Timestamp for each operation
- Success/failure status
- Created entity IDs
- Error messages and stack traces
- Summary statistics

## Customization

### Modifying Demo Data

1. Generate the Excel template
2. Open in Excel or LibreOffice
3. Modify any data in the sheets
4. Save the file
5. Run the loader script

### Adding New Data

To add new records:

1. Open the generated Excel file
2. Add new rows to the appropriate sheets
3. Ensure `external_id` values are unique
4. Ensure referenced values exist (e.g., office names, product codes)
5. Save and reload

### Custom Configuration

Create a custom config file:

```json
{
  "fineract_url": "https://your-fineract-instance.com/fineract-provider/api/v1",
  "username": "admin",
  "password": "secure_password",
  "tenant": "production",
  "default_password": "Change@123",
  "connection_settings": {
    "timeout": 60,
    "retry_attempts": 5,
    "retry_delay": 3
  }
}
```

## Testing

### Verify Installation

```bash
# Test Excel generation
cd scripts
python3 generate_excel_template.py

# Check output
ls -lh ../output/
```

### Test API Connection

```bash
# Verify Fineract is running
curl -u mifos:password https://localhost/fineract-provider/api/v1/offices

# If successful, you'll see a JSON response
```

### Load Test Data

```bash
# Load into test instance
python3 load_demo_data.py ../output/fineract_demo_data_*.xlsx ../config/fineract_config.json
```

## Troubleshooting

### Excel Generation Issues

**Problem**: `ModuleNotFoundError: No module named 'pandas'`

**Solution**:
```bash
pip install pandas openpyxl
```

**Problem**: `PermissionError: [Errno 13] Permission denied`

**Solution**: Ensure the `output/` directory exists and is writable:
```bash
mkdir -p output
chmod 755 output
```

### API Loading Issues

**Problem**: `Connection refused` or `ConnectionError`

**Solution**:
- Verify Fineract is running: `docker ps` or check service status
- Verify the URL in config file matches Fineract instance
- Check firewall settings

**Problem**: `401 Unauthorized`

**Solution**:
- Verify username and password in config file
- Ensure the user has admin privileges in Fineract

**Problem**: `404 Not Found`

**Solution**:
- Verify the Fineract API base URL
- Ensure `/fineract-provider/api/v1` is the correct path

**Problem**: `Tenant identifier is invalid`

**Solution**:
- Verify the tenant name in config file
- Check available tenants in Fineract

**Problem**: Records fail to create

**Solution**:
- Check the logs: `tail -f ../logs/load_demo_data.log`
- Verify data integrity in Excel (no missing required fields)
- Ensure dependencies exist (e.g., offices before staff)

## Production Deployment

### Before Loading to Production

1. **Backup**: Create a full backup of your Fineract database
2. **Review Data**: Carefully review all data in the Excel file
3. **Update Credentials**: Change all default passwords
4. **Remove Test Data**: Remove any test/dummy records
5. **Customize**: Adapt data to your specific needs
6. **Test First**: Load into a staging environment first

### Security Considerations

**Passwords and Access**:
- Change default passwords immediately after loading (default: `password`)
- Use strong passwords for all user accounts (minimum 12 characters, mixed case, numbers, special chars)
- Enable password expiration policies (90 days recommended)
- Enforce password history (prevent reuse of last 5 passwords)
- Enable account lockout after failed login attempts (5 attempts recommended)

**Role-Based Access Control**:
- Create custom roles in Fineract UI with granular permissions (see [RBAC section](#role-based-access-control-rbac))
- Never assign "Super user" role to operational staff in production
- Implement segregation of duties: separate makers from checkers
- Regularly audit user permissions and remove unnecessary access
- Disable accounts for terminated employees immediately
- Review role assignments quarterly

**Maker-Checker (Critical)**:
- Enable Maker-Checker for ALL sensitive operations (see [Maker-Checker section](#maker-checker-dual-authorization))
- Never assign both MAKER and CHECKER roles to the same user
- Monitor checker inbox daily to prevent operational bottlenecks
- Generate weekly Maker-Checker activity reports
- Investigate rejected tasks for fraud indicators
- Enforce proper documentation for approvals

**Network and Infrastructure**:
- Enable SSL/TLS for ALL API connections (use HTTPS, not HTTP)
- Use valid SSL certificates (not self-signed in production)
- Restrict API access to trusted networks via firewall rules
- Enable Fineract audit logs for all operations
- Implement intrusion detection/prevention systems (IDS/IPS)
- Use VPN for remote access

**Data Protection**:
- Regularly backup the database (daily recommended)
- Test backup restoration procedures monthly
- Encrypt backups and store offsite
- Enable database audit logging
- Restrict direct database access (use API only)
- Implement data retention and deletion policies

### Post-Loading Steps

1. **Verify Data**: Check all created records in Fineract UI
2. **Configure Roles**:
   - Create custom roles in Fineract UI (Branch Manager, Loan Officer, Cashier)
   - Assign appropriate permissions to each role (see Sheet 22: Roles & Permissions)
   - Update `role_name_mapping` in load_demo_data.py if using custom roles
   - Re-assign users to custom roles if needed
3. **Verify Maker-Checker**:
   - Test Maker-Checker workflow with sample transactions
   - Verify checker inbox is accessible to managers
   - Train staff on approval process
4. **Configure Accounting**: Verify loan/savings product GL mappings
5. **Set Up Workflows**: Verify approval workflows are working correctly
6. **Enable Integrations**: Set up SMS, mobile money, etc.
7. **Train Users**:
   - Train staff on role-specific functions
   - Train managers on Maker-Checker approval process
   - Train staff on security policies and fraud detection
8. **Security Hardening**:
   - Change all default passwords
   - Enable SSL/TLS
   - Configure firewall rules
   - Enable audit logging
9. **Monitor**:
   - Set up monitoring and alerting
   - Monitor checker inbox backlog
   - Monitor failed login attempts
   - Review audit logs weekly

## Advanced Usage

### Batch Processing

Load multiple Excel files:

```bash
for file in ../output/*.xlsx; do
    python3 load_demo_data.py "$file"
done
```

### Selective Loading

Modify `load_demo_data.py` to load only specific data:

```python
# In main()
loader = FineractDemoDataLoader(excel_file, config_file)
loader.load_offices()
loader.load_staff()
# loader.load_clients()  # Skip clients
```

### API Testing

Use the generated data for API testing:

```bash
# Get created client
CLIENT_ID=$(grep "Created client" ../logs/load_demo_data.log | head -1 | grep -o "ID: [0-9]*" | cut -d' ' -f2)

curl -u mifos:password \
  http://localhost:8080/fineract-provider/api/v1/clients/$CLIENT_ID
```

## Support and Contribution

### Getting Help

- Check the logs: `logs/load_demo_data.log`
- Review Fineract API documentation: https://fineract.apache.org/
- Apache Fineract mailing list: https://fineract.apache.org/community.html

### Reporting Issues

Please include:
- Python version: `python3 --version`
- Fineract version
- Config file (remove sensitive data)
- Relevant log excerpts
- Steps to reproduce

### Contributing

Contributions are welcome:
1. Add new data types
2. Improve error handling
3. Add validation rules
4. Enhance documentation
5. Create additional templates

## License

This toolkit is provided as-is for use with Apache Fineract.

## Changelog

### Version 2.0.0 (2025-10-14)
- **NEW**: Expanded to 36 Excel sheets (was 33)
- **NEW**: Floating Rates - Variable interest rates linked to BEAC base rate (3 rates)
- **NEW**: Delinquency Buckets - Loan arrears classification ranges (5 buckets)
- **NEW**: Tax Groups - Withholding tax configuration with tax components (2 groups)
- **FIXED**: Staff role assignment - Staff now correctly assigned to custom roles from Excel
- **FIXED**: Charge frequency implementation - Replaced hardcoded Annual/Monthly with dynamic fee frequency
- **FIXED**: Transfer in Suspense account type - Changed from GL 131 (LIABILITY) to GL 122 (ASSET)
- **FIXED**: Execution order - Roles now loaded BEFORE staff for proper assignment
- **ENHANCED**: Documentation restructured - Consolidated into 2 files (README.md + DOCUMENTATION.md)
- Comprehensive error handling and validation
- Production-ready with all fixes verified

### Version 1.3.0 (2024-10-10)
- **NEW**: Expanded to 33 Excel sheets (was 32)
- **NEW**: Tellers - Physical cash counters created at each branch (4 tellers - 1 per office)
- Teller entities enable daily cash allocation and settlement tracking
- Complete teller workflow documentation with GL mappings
- Enhanced cash management capabilities for branch operations

### Version 1.2.0 (2024-10-10)
- **NEW**: Expanded to 32 Excel sheets (was 29)
- **NEW**: SMS/Email Configuration - Twilio & Gmail SMTP setup (17 config items)
- **NEW**: Notification Templates - 16 SMS/Email templates for all lifecycle events
- **NEW**: Data Tables (Custom Fields) - 24 custom fields for Client, Loan, Savings entities
- Complete client communication setup with SMS and email templates
- Enhanced KYC capabilities with custom fields
- Reference configurations for external integrations
- Updated documentation with communication and custom field features

### Version 1.1.0 (2024-10-10)
- **NEW**: Expanded to 29 Excel sheets (was 23)
- **NEW**: Currency Configuration - XAF currency setup with proper decimal handling
- **NEW**: Working Days Configuration - Monday-Friday working days setup
- **NEW**: Account Number Preferences - Automatic account number generation for 5 entity types
- **NEW**: Codes and Code Values - 43 dropdown values across 9 code categories including account closure reasons
- **NEW**: Scheduler Jobs - 10 automated cron jobs for routine operations
- **NEW**: Global Configuration - 23 comprehensive system-wide settings
- Enhanced system configuration loading sequence (Phase 1-5)
- Updated API endpoint documentation with system configuration endpoints
- Comprehensive documentation for all new features

### Version 1.0.0 (2024-10-10)
- Initial release
- Support for 23 Excel sheets covering all core Fineract entities
- Complete accounting integration with Cash-based accounting
- OHADA and COBAC compliance
- Inter-branch operations support with Financial Activity Mappings
- **Role-Based Access Control**: 3 roles with granular permissions reference guide
- **Maker-Checker (4-eyes principle)**: 12 dual-authorization rules for critical operations
- Dynamic role fetching and assignment from Fineract
- Cameroon-specific configuration
- Comprehensive error handling and security features
- Full documentation with RBAC and Maker-Checker guides

## Appendix

### Fineract API Endpoints Used

**System Configuration**:
- `GET /currencies` - Fetch currency configuration
- `PUT /workingdays` - Configure working days
- `GET /configurations` - Fetch global configurations
- `PUT /configurations/{id}` - Update global configuration
- `POST /codes` - Create code categories
- `POST /codes/{id}/codevalues` - Create code values
- `POST /accountnumberformats` - Configure account number preferences
- `GET /jobs` - Fetch scheduler jobs
- `PUT /jobs/{id}` - Update scheduler job configuration
- `POST /datatables` - Create custom data tables (custom fields)
- `GET /datatables` - List all data tables

**Organizational Setup**:
- `POST /offices` - Create offices
- `POST /staff` - Create staff
- `GET /roles` - Fetch available roles (for dynamic role assignment)
- `POST /users` - Create user accounts with role assignments

**Accounting Setup**:
- `POST /glaccounts` - Create GL accounts
- `POST /charges` - Create charges
- `POST /funds` - Create fund sources
- `POST /paymenttypes` - Create payment types
- `POST /holidays` - Create holidays
- `POST /financialactivityaccounts` - Configure Financial Activity Mappings (inter-branch)

**Security & Workflow**:
- `POST /makercheckers` - Enable Maker-Checker for critical operations
- `GET /makercheckers` - Retrieve pending/approved/rejected checker tasks
- `POST /makercheckers/{taskId}?command=approve` - Approve a checker task
- `POST /makercheckers/{taskId}?command=reject` - Reject a checker task

**Products & Accounts**:
- `POST /loanproducts` - Create loan products with accounting mappings
- `POST /savingsproducts` - Create savings products with accounting mappings
- `POST /clients` - Create clients
- `POST /savingsaccounts` - Create and activate savings accounts
- `POST /loans` - Create, approve, and disburse loan accounts

### Default Credentials

Created user accounts use these credentials:
- **Username**: As specified in Staff sheet (e.g., `manager.douala`)
- **Password**: `password` (must be changed)

### Date Formats

All dates in Excel should use format: `YYYY-MM-DD`
- Example: `2024-01-15`

### Currency

All amounts are in **XAF** (Central African CFA franc)

### Sample Data Summary

The default template includes:
- 4 offices across 4 cities
- 12 staff members (4 managers, 4 loan officers, 4 cashiers)
- 12 diverse clients from different sectors
- 6 active loans totaling 6,030,000 XAF
- 12 savings accounts with various balances
- Complete operational structure ready for microfinance operations

---

## 🎉 What's New in Version 2.0 (Major Refactoring)

### ✨ Idempotent Loading

**Re-run the loader safely without creating duplicates!**

Configuration entities are now **updatable**:
- ✅ **GL Accounts** - Updated by `glCode`
- ✅ **Loan Products** - Updated by `shortName`
- ✅ **Savings Products** - Updated by `shortName`
- ✅ **Charges** - Updated by `name`

**Example Use Case**: Changed interest rate for a product? Just update the Excel and re-run the loader - it will UPDATE the existing product instead of creating a duplicate!

```bash
# Before v2.0: Creates duplicate product ❌
# After v2.0: Updates existing product ✅
python3 load_demo_data.py --config config.yml
```

### 🔐 Maker-Checker Improvements

**BREAKING CHANGE**: Removed amount-based thresholds (not supported by Fineract)

**Maker Checker Config Sheet Changes**:
- ❌ **REMOVED**: `threshold_amount` and `threshold_currency` columns
- ✅ **ADDED**: `enabled` column (boolean: True/False)

**Key Clarification**: Fineract's maker-checker is **boolean-only**:
- When enabled, **ALL** operations of that type require approval
- No native support for amount-based thresholds
- This is a Fineract limitation, not a loader limitation

**New Feature**: Auto-assign maker-checker permissions to roles:
- Maker roles automatically get base permissions (e.g., `APPROVE_LOAN`)
- Checker roles automatically get checker permissions (e.g., `CHECKER_APPROVE_LOAN`)
- No manual permission configuration required!

### 🔀 Loan Workflow States

**NEW FEATURE**: Demonstrate maker-checker approval workflows

**Loan Accounts Sheet - New Column**: `workflow_state`

| Value | Behavior | Use Case |
|-------|----------|----------|
| `active` | Auto-approve + auto-disburse | Normal loans (default) |
| `pending_approval` | Create only, requires checker approval | Test approval workflow |
| `pending_disbursal` | Approve only, requires checker disbursal | Test disbursal workflow |

**Demo Data**:
- **LOAN-002** (2M XAF): `pending_approval` - Requires Branch Manager to approve
- **LOAN-006** (3M XAF): `pending_disbursal` - Requires Branch Manager to disburse
- **All others**: `active` - Fully disbursed

**Test Workflow**:
1. Run loader → LOAN-002 created but not approved
2. Login as Branch Manager
3. View Maker-Checker inbox
4. Approve LOAN-002
5. Loan becomes active!

### ✅ Validation Script

**NEW**: Pre-flight checks before loading

```bash
python3 validate_excel.py ../output/fineract_demo_data_YYYYMMDD.xlsx
```

**Validates**:
- ✓ Required columns present
- ✓ No forbidden columns (threshold_amount, threshold_currency)
- ✓ Valid workflow_state values
- ✓ External ID uniqueness
- ✓ Cross-sheet references (client IDs, product names, etc.)
- ✓ Maker-checker configuration completeness

**Output**:
```
================================================================================
VALIDATION RESULTS
================================================================================
✅ VALIDATION PASSED (No errors or warnings)
================================================================================
```

### 📝 Comprehensive Documentation

**NEW Files**:
- `CHANGELOG.md` - Detailed change history, migration guide, usage examples
- `validate_excel.py` - Validation script with 15+ checks
- Updated `README.md` - This section!

### 🔧 Technical Improvements

**API Client (`fineract_client.py`)**:
- Added `upsert()` method with intelligent search-and-update logic
- Handles multiple Fineract response formats
- Better error handling and logging

**Products Loader (`loaders/products.py`)**:
- GL Accounts, Loan Products, Savings Products, Charges now use upsert
- Fixed maker-checker enablement to respect `enabled` flag
- Removed misleading threshold logging

**Roles & Permissions Loader (`loaders/roles_permissions.py`)**:
- New `_get_maker_checker_permissions()` method
- Auto-merges maker-checker permissions into roles
- Supports roles defined only in Maker Checker Config sheet

**Accounts Loader (`loaders/accounts.py`)**:
- Implements loan workflow states
- Clear logging for pending loans
- Shows next actions for approvals

### 🚀 Quick Migration to v2.0

1. **Regenerate Excel template**:
   ```bash
   python3 generate_excel_template.py
   ```

2. **Review Maker-Checker Config**:
   - Set `enabled=True` only for permissions you want to activate
   - Default: Only `APPROVE_LOAN` and `DISBURSE_LOAN` enabled

3. **Test in Development**:
   ```bash
   # Validate first
   python3 validate_excel.py ../output/fineract_demo_data_*.xlsx

   # Load data
   python3 load_demo_data.py --config config.yml
   ```

4. **Verify Idempotency**:
   ```bash
   # Run twice - should not create duplicates
   python3 load_demo_data.py --config config.yml
   python3 load_demo_data.py --config config.yml
   ```

### ⚠️ Breaking Changes

1. **Maker Checker Config sheet structure changed**:
   - Must regenerate Excel template
   - Cannot use old templates with v2.0 loader

2. **Loan workflow behavior changed**:
   - Some loans may be in pending state (as designed)
   - To preserve old behavior: Set all loans to `workflow_state=active`

3. **Products are now updateable**:
   - Re-running loader updates configuration
   - If you want multiple versions, use different `shortName` values

### 📖 Additional Resources

- **CHANGELOG.md**: Complete change history with code examples
- **validate_excel.py --help**: Validation script usage
- **Fineract Docs**: https://fineract.apache.org/

---

**Happy Data Loading! 🎉**
