# Fineract Microfinance System - Staff Training Guide

**Organization**: Cameroon Microfinance Demo
**System**: Apache Fineract 1.8+
**Currency**: XAF (Central African CFA Franc)
**Training Version**: 2.0
**Last Updated**: October 14, 2025

---

## Table of Contents

1. [Introduction](#introduction)
2. [System Access](#system-access)
3. [Staff Roles and Responsibilities](#staff-roles-and-responsibilities)
4. [Branch Manager Guide](#branch-manager-guide)
5. [Loan Officer Guide](#loan-officer-guide)
6. [Cashier Guide](#cashier-guide)
7. [Common Workflows](#common-workflows)
8. [Maker-Checker (Dual Authorization)](#maker-checker-dual-authorization)
9. [Inter-Branch Transactions](#inter-branch-transactions)
10. [Reports and Monitoring](#reports-and-monitoring)
11. [Security Best Practices](#security-best-practices)
12. [Troubleshooting](#troubleshooting)

---

## Introduction

### What is Fineract?

Fineract is a comprehensive microfinance management system that helps our organization:
- Manage client relationships and accounts
- Process loans and savings transactions
- Track cash and accounting
- Generate reports and analytics
- Ensure compliance with banking regulations

### Your Role in the System

Every staff member has a specific role with specific permissions. This guide will help you:
- ✅ Understand your role and responsibilities
- ✅ Learn how to perform your daily tasks
- ✅ Follow security and compliance procedures
- ✅ Get help when you need it

### System Overview

Our Fineract system is configured with:
- **4 Offices**: Head Office (Yaounde), Douala Branch, Bafoussam Branch, Bamenda Branch
- **3 Staff Roles**: Branch Manager, Loan Officer, Cashier
- **3 Loan Products**: Microcredit, SME Business Loan, Agricultural Seasonal Loan
- **3 Savings Products**: Voluntary Savings, Fixed Deposit, Mandatory Group Savings
- **Currency**: XAF (Central African CFA Franc)

---

## System Access

### Logging In

**Step 1**: Open your web browser and go to:
```
https://your-fineract-url.com
```

**Step 2**: Enter your credentials:
- **Username**: Your assigned username (e.g., `manager.douala`, `officer.douala`, `cashier.douala`)
- **Password**: Your secure password (must be changed on first login)
- **Tenant**: `default` (usually pre-filled)

**Step 3**: Click **Sign In**

### First Login - Password Change

🔒 **IMPORTANT**: You MUST change your password on first login.

**Password Requirements**:
- Minimum 12 characters
- At least 1 uppercase letter (A-Z)
- At least 1 lowercase letter (a-z)
- At least 1 number (0-9)
- At least 1 special character (@, #, $, %, etc.)
- No consecutive repeating characters (e.g., "aaa", "111")

**Example Strong Passwords**:
- ✅ `Fineract@2025!Secure`
- ✅ `MyMFI#Pwd2025$Safe`
- ❌ `password123` (too simple)
- ❌ `12345678` (no letters or special chars)

### Navigation

Once logged in, you'll see the **Dashboard** with navigation menu:

| Menu Item | What You Can Do |
|-----------|-----------------|
| **Dashboard** | View summary and quick access |
| **Clients** | Search and manage clients |
| **Accounts** | View savings and loan accounts |
| **Tasks** | Maker-Checker approval tasks (managers only) |
| **Reports** | Generate reports |
| **Admin** | System settings (managers only) |

---

## Staff Roles and Responsibilities

### Role Overview

| Role | Primary Responsibilities | Key Permissions |
|------|------------------------|-----------------|
| **Branch Manager** | Branch operations, approvals, oversight | Approve loans, approve withdrawals, view all reports, manage staff |
| **Loan Officer** | Client relationships, loan processing | Create clients, create/process loans, view savings |
| **Cashier** | Daily transactions, cash management | Process deposits/withdrawals, accept repayments, manage teller cash |

### Segregation of Duties

🛡️ **Important Security Principle**: Different staff members perform different tasks to prevent fraud.

**Example Workflow**:
1. **Loan Officer** creates loan application → **MAKER**
2. **Branch Manager** reviews and approves → **CHECKER**
3. **Cashier** disburses the loan cash

❌ **NEVER**:
- Share your password with colleagues
- Let someone else use your login
- Approve your own transactions

---

## Branch Manager Guide

### Your Responsibilities

As a Branch Manager, you are responsible for:
- ✅ Approving loan applications above 2,000,000 XAF
- ✅ Approving loan disbursements above 5,000,000 XAF
- ✅ Approving large withdrawals above 1,000,000 XAF
- ✅ Approving client activations and transfers
- ✅ Managing teller cash allocation and settlement
- ✅ Reviewing branch performance reports
- ✅ Supervising loan officers and cashiers
- ✅ Recording cash shortage/overage journal entries

### Daily Tasks

#### Morning Routine (8:00 AM)

**1. Review Pending Approvals**

Navigate to: **Admin → System → Checker Inbox & Tasks**

You'll see pending tasks that need approval:
- Loan applications
- Loan disbursements
- Large withdrawals
- Client activations
- Inter-branch transfers

**2. Cash Allocation to Tellers**

Navigate to: **Organization → Tellers**

For each teller:
- Click on teller name (e.g., "Teller 1 - Douala")
- Click **Allocate Cash**
- Enter amount (e.g., 2,000,000 XAF)
- Select date
- Click **Submit**

The system will:
- Debit GL 42 (Vault)
- Credit GL 42 (Teller Cash)

**3. Review Yesterday's Transactions**

Navigate to: **Reports → Transaction Reports**
- Check for unusual patterns
- Verify large transactions
- Review rejected approvals

#### During the Day

**Approve Loan Applications**

When a Loan Officer submits a loan application:

1. Go to **Admin → System → Checker Inbox & Tasks**
2. Click on the pending loan approval task
3. Review:
   - Client information and credit history
   - Loan amount and purpose
   - Collateral and guarantors
   - Loan officer's notes
4. Decision:
   - **Approve**: Click **Approve** button
   - **Reject**: Click **Reject**, add rejection reason

**Approve Large Withdrawals**

When a Cashier initiates a withdrawal above 1,000,000 XAF:

1. Go to **Admin → System → Checker Inbox & Tasks**
2. Click on the pending withdrawal task
3. Verify:
   - Client identity (ask cashier to verify ID)
   - Account balance
   - Withdrawal reason
   - Sufficient cash in teller
4. Decision:
   - **Approve**: Click **Approve**
   - **Reject**: Click **Reject** with reason

**Record Cash Shortage/Overage**

At teller settlement, if there's a shortage or overage:

1. Navigate to: **Accounting → Frequent Postings → Create Journal Entry**
2. For **Cash Shortage** (teller is short):
   - **Office**: Your branch
   - **Currency**: XAF
   - **Debit**: GL 98 (Cash Shortage - Expense)
   - **Credit**: GL 42 (Cash on Hand)
   - **Amount**: Shortage amount
   - **Date**: Today's date
   - **Comments**: "Cash shortage - Teller 1 - Douala - Date"
3. For **Cash Overage** (teller has excess):
   - **Debit**: GL 42 (Cash on Hand)
   - **Credit**: GL 86 (Cash Overage - Income)
   - **Amount**: Overage amount
   - **Comments**: "Cash overage - Teller 1 - Douala - Date"
4. Click **Submit**

#### Evening Routine (5:00 PM)

**1. Teller Settlement**

Navigate to: **Organization → Tellers**

For each teller:
- Click on teller name
- Click **Settle Cash**
- Enter settlement amount
- If amount doesn't match allocated cash:
  - System shows shortage or overage
  - Record journal entry (see above)
- Click **Submit**

**2. Daily Reports**

Generate and review:
- **Cash Position Report**: Verify vault balance
- **Transaction Summary**: Total deposits, withdrawals, repayments
- **Pending Approvals**: Clear any remaining approvals

**3. Backup Critical Data**

Ensure automated backup completed successfully.

### Weekly Tasks

**Monday Morning**:
- Review last week's branch performance
- Set weekly targets for loan officers

**Friday Afternoon**:
- Generate weekly reports:
  - Portfolio at Risk (PAR)
  - Loan disbursements vs target
  - Savings mobilization
  - Cash flow summary
- Submit reports to Head Office

### Monthly Tasks

**First Week of Month**:
- Generate monthly branch performance report
- Review loan provisioning requirements
- Conduct staff performance reviews
- Reconcile inter-branch accounts (GL 122 and GL 131)

---

## Loan Officer Guide

### Your Responsibilities

As a Loan Officer, you are responsible for:
- ✅ Recruiting new clients
- ✅ Conducting client KYC (Know Your Customer)
- ✅ Processing loan applications
- ✅ Conducting field visits and appraisals
- ✅ Following up on loan repayments
- ✅ Maintaining client relationships

### Client Onboarding

#### Step 1: Create New Client

Navigate to: **Clients → Create Client**

**Required Information**:
- **Personal Details**:
  - First Name, Last Name
  - Gender, Date of Birth (must be 18+ years)
  - Mobile Number, Email (optional)
  - National ID Number

- **Address**:
  - Street Address
  - City, Region

- **Office Assignment**:
  - Office: Your branch
  - Staff: Your name (as loan officer)

- **Client Type**: Individual or Group

- **Activation Date**: Today's date

**Additional Information** (Custom Fields):
- ID Type (National ID, Passport, Voter Card, Driver License)
- ID Expiry Date
- Next of Kin Name and Phone
- Next of Kin Relationship
- Employer Name (if salaried)
- Business Type (if self-employed)
- Years in Business
- Home Ownership Status

Click **Submit**

#### Step 2: Activate Client

After creating the client:
1. The client enters **Pending** status
2. You submit for activation
3. **Branch Manager approves** (Maker-Checker)
4. Client becomes **Active** and can open accounts

### Loan Application Processing

#### Step 1: Create Loan Application

Navigate to: **Clients → [Client Name] → Accounts → New Loan Account**

**Loan Details**:
- **Product**: Select loan product
  - Microcredit (50,000 - 500,000 XAF)
  - SME Business Loan (500,000 - 5,000,000 XAF)
  - Agricultural Seasonal Loan (100,000 - 2,000,000 XAF)

- **Principal Amount**: Requested amount

- **Loan Term**: Number of months (within product limits)

- **Interest Rate**: Annual rate (within product limits)

- **Repayment Frequency**: Monthly or Weekly

- **Loan Officer**: Your name

- **Fund Source**: Select funding source

- **Loan Purpose**: Detailed description

- **Collateral**: Describe collateral

- **Submitted Date**: Today's date

**Custom Fields**:
- Loan Purpose Detail (detailed description)
- Collateral Description
- Collateral Value (XAF)
- Guarantor Count
- Credit Score (if available)
- Repayment Source (Business Income, Salary, Remittances)
- Previous Loan History

Click **Submit**

#### Step 2: Field Appraisal

Before approval, conduct field visit:

📋 **Appraisal Checklist**:
- ✅ Verify business location exists
- ✅ Assess business viability
- ✅ Verify collateral exists and value
- ✅ Interview guarantors
- ✅ Check credit references
- ✅ Verify income sources
- ✅ Assess repayment capacity

Document findings in **Notes** section of loan account.

#### Step 3: Submit for Approval

1. Navigate to loan account
2. Click **Approve** (you are submitting, not approving)
3. For loans **above 2,000,000 XAF**:
   - Goes to **Checker Inbox** for Branch Manager
   - Wait for manager approval
4. For loans **below 2,000,000 XAF**:
   - May be auto-approved (check your organization policy)

#### Step 4: After Approval - Disburse

Once Branch Manager approves:

1. Navigate to loan account
2. Click **Disburse**
3. Enter:
   - **Disbursement Date**: Today or scheduled date
   - **Payment Type**: Cash, Mobile Money, Bank Transfer
   - **Transaction Amount**: Principal amount
4. Click **Submit**

**For disbursements above 5,000,000 XAF**:
- Goes to Branch Manager for approval (Maker-Checker)

**What Happens After Disbursement**:
- Client receives cash from cashier
- Loan account becomes **Active**
- Repayment schedule is generated
- System posts accounting entries:
  - Debit: GL 52 (Loan Portfolio)
  - Credit: GL 41 (Fund Source)

### Loan Monitoring

#### Track Repayments

Navigate to: **Clients → [Client Name] → Accounts → [Loan Account]**

View:
- **Repayment Schedule**: All expected payments
- **Transactions**: All actual payments received
- **Summary**: Total paid, outstanding balance, arrears

#### Follow-Up on Overdue Loans

Navigate to: **Reports → Loans → Loans in Arrears**

For each overdue loan:
1. **1-7 days overdue**: Phone call reminder
2. **8-14 days overdue**: Field visit
3. **15-30 days overdue**: Send formal notice
4. **30+ days overdue**: Escalate to Branch Manager

**Document All Follow-Up**:
- Add notes to client account
- Record phone calls and field visits
- Update client contact information if changed

### Savings Account Opening

Navigate to: **Clients → [Client Name] → Accounts → New Savings Account**

**Savings Details**:
- **Product**: Select savings product
  - Voluntary Savings (flexible, interest-bearing)
  - Fixed Deposit (locked, higher interest)
  - Mandatory Group Savings

- **Field Officer**: Your name

- **Submitted Date**: Today's date

- **Minimum Opening Balance**: Check product requirements

Click **Submit** → **Approve** → **Activate**

Initial deposit is processed by cashier.

---

## Cashier Guide

### Your Responsibilities

As a Cashier, you are responsible for:
- ✅ Processing deposits and withdrawals
- ✅ Accepting loan repayments
- ✅ Managing teller cash
- ✅ Balancing cash daily
- ✅ Providing excellent customer service

### Daily Cash Management

#### Morning Routine (8:00 AM)

**1. Receive Cash Allocation from Branch Manager**

Your Branch Manager will allocate cash to your teller (e.g., 2,000,000 XAF).

**2. Count and Verify Cash**

- Count cash received from vault
- Verify amount matches allocation in system
- Sign cash allocation receipt
- Store cash securely in your teller drawer

#### During the Day - Transactions

**Process Savings Deposit**

1. **Receive Deposit**:
   - Ask client for account number or passbook
   - Count cash in front of client
   - Verify currency notes are genuine

2. **In Fineract**:
   - Navigate to: **Clients → Search by Account Number**
   - Enter account number (e.g., `VOL00000001`)
   - Click on savings account
   - Click **Deposit**

3. **Enter Details**:
   - **Transaction Date**: Today
   - **Transaction Amount**: Counted amount
   - **Payment Type**: Cash (or MTN MoMo, Orange Money, etc.)
   - **Receipt Number**: Optional
   - Click **Submit**

4. **Give Receipt**:
   - Print system receipt
   - Update passbook (if client has one)
   - Thank client

**Process Savings Withdrawal**

1. **Verify Client Identity**:
   - Ask for ID card (National ID, Passport)
   - Ask for account number
   - For **withdrawals above 1,000,000 XAF**: Notify Branch Manager

2. **In Fineract**:
   - Search for account by account number
   - Click **Withdraw**

3. **Enter Details**:
   - **Transaction Date**: Today
   - **Transaction Amount**: Withdrawal amount
   - **Payment Type**: Cash
   - Click **Submit**

4. **For Large Withdrawals (> 1,000,000 XAF)**:
   - Transaction goes to **Pending Approval**
   - Wait for Branch Manager approval
   - Once approved, proceed with cash disbursement

5. **Verify Balance**:
   - Check if account has sufficient balance
   - Check for minimum balance requirements
   - Check for withdrawal limits

6. **Disburse Cash**:
   - Count cash in front of client
   - Have client count and verify
   - Client signs withdrawal slip
   - Give receipt

**Accept Loan Repayment**

1. **Ask for Loan Account Number**

Client provides loan account number (e.g., `MICRO000001`)

2. **In Fineract**:
   - Navigate to: **Loans → Search by Account Number**
   - Click on loan account
   - Click **Make Repayment**

3. **Enter Details**:
   - **Transaction Date**: Today
   - **Transaction Amount**: Amount paid
   - **Payment Type**: Cash
   - Click **Submit**

4. **System Automatically Allocates**:
   - Penalties (if any)
   - Fees (if any)
   - Interest due
   - Principal

5. **Give Receipt**:
   - Print repayment receipt
   - Show remaining balance
   - Show next payment due date

#### Evening Routine (5:00 PM)

**1. Stop Taking Transactions**

- Close your teller window 30 minutes before end of day
- Finish processing pending transactions

**2. Count Your Cash**

- Count all cash in drawer
- Separate by denomination
- Total amount should match:
  - Opening allocation
  - Plus deposits received
  - Minus withdrawals paid
  - Minus loan disbursements

**3. Settle Teller in Fineract**

Your Branch Manager will initiate settlement:
- **If Cash Matches**: Perfect! No issues
- **If Short**: You are responsible for shortage (manager records GL 98)
- **If Over**: Report to manager (manager records GL 86)

**4. Return Cash to Vault**

- Place cash in sealed bag
- Give to Branch Manager for vault storage
- Sign settlement form

### Inter-Branch Transactions

Sometimes clients from other branches visit your branch.

**Scenario**: Client from Douala Branch visits Bamenda Branch

**How to Process**:

1. **Ask for Account Number**:
   - Client must provide savings or loan account number
   - Examples: `VOL00000001`, `MICRO000001`

2. **Search by Account Number**:
   - Go to **Savings Accounts → Search**
   - Enter account number
   - System will find account even if client is from another branch

3. **Verify Client Identity**:
   - Check ID card
   - Verify name matches account

4. **Process Transaction Normally**:
   - Deposit or withdrawal works the same way
   - System automatically handles inter-branch accounting

5. **System Posts Accounting**:
   - Your branch (Bamenda): DR Cash, CR GL 122 (Due from Douala)
   - Client's branch (Douala): DR GL 131 (Due to Bamenda), CR Customer Deposit

**Important**:
- ❌ **DO NOT** search by client name for inter-branch transactions
- ✅ **ALWAYS** search by account number
- ✅ **ALWAYS** verify client ID

### Security and Fraud Prevention

**Red Flags - Report to Manager Immediately**:

🚨 **Suspicious Transactions**:
- Client requests multiple large withdrawals in short time
- Client cannot answer basic questions about their account
- Client's ID looks fake or altered
- Client is nervous or evasive
- Third party trying to withdraw on behalf of client without proper authorization

🚨 **Suspected Fraud**:
- Account number doesn't match client name
- Client has multiple IDs with different names
- Large cash deposits from unknown sources
- Withdrawal patterns inconsistent with client profile

**What to Do**:
1. Stay calm and professional
2. Politely ask to verify details with manager
3. Do NOT accuse the client
4. Notify Branch Manager discreetly
5. Document the incident

---

## Common Workflows

### Workflow 1: New Client Opens Savings Account

| Step | Who | Action | System |
|------|-----|--------|--------|
| 1 | **Loan Officer** | Create client profile | Client status: Pending |
| 2 | **Loan Officer** | Submit for activation | Goes to Checker Inbox |
| 3 | **Branch Manager** | Approve activation | Client status: Active |
| 4 | **Loan Officer** | Create savings account | Savings status: Pending |
| 5 | **Loan Officer** | Approve and activate | Savings status: Active |
| 6 | **Cashier** | Accept initial deposit | Account has balance |

**Timeline**: Same day (if manager approves quickly)

### Workflow 2: Loan Application to Disbursement

| Step | Who | Action | System | Timeline |
|------|-----|--------|--------|----------|
| 1 | **Loan Officer** | Field visit and appraisal | - | Day 1 |
| 2 | **Loan Officer** | Create loan application | Loan status: Submitted | Day 2 |
| 3 | **Loan Officer** | Submit for approval | Goes to Checker Inbox | Day 2 |
| 4 | **Branch Manager** | Review and approve | Loan status: Approved | Day 3 |
| 5 | **Loan Officer** | Submit for disbursement | Goes to Checker Inbox (if > 5M) | Day 4 |
| 6 | **Branch Manager** | Approve disbursement (if > 5M) | Ready to disburse | Day 4 |
| 7 | **Cashier** | Disburse cash to client | Loan status: Active | Day 5 |

**Timeline**: 3-5 days (depending on amount and approvals)

### Workflow 3: Large Withdrawal Approval

| Step | Who | Action | System | Time |
|------|-----|--------|--------|------|
| 1 | **Client** | Visits branch, requests withdrawal | - | - |
| 2 | **Cashier** | Checks account, sees amount > 1M XAF | - | - |
| 3 | **Cashier** | Notifies Branch Manager | - | - |
| 4 | **Cashier** | Initiates withdrawal in system | Goes to Checker Inbox | 2 mins |
| 5 | **Branch Manager** | Verifies client ID and purpose | - | 5 mins |
| 6 | **Branch Manager** | Approves in Checker Inbox | Withdrawal executes | 1 min |
| 7 | **Cashier** | Disburses cash to client | Transaction complete | 5 mins |

**Timeline**: 10-15 minutes (if manager available)

### Workflow 4: Teller Daily Cash Cycle

| Time | Who | Action | System |
|------|-----|--------|--------|
| 8:00 AM | **Branch Manager** | Allocate cash to teller | DR Vault, CR Teller |
| 8:30 AM - 4:30 PM | **Cashier** | Process transactions | DR/CR per transaction |
| 5:00 PM | **Cashier** | Count cash and verify | - |
| 5:15 PM | **Branch Manager** | Settle teller | CR Vault, DR Teller |
| 5:20 PM | **Cashier** | Return cash to vault | Cash secured |

**Daily**: Every working day (Monday - Friday)

---

## Maker-Checker (Dual Authorization)

### What is Maker-Checker?

**Maker-Checker** (also called "4-eyes principle") is a security control that requires two people to complete sensitive transactions:

1. **MAKER**: Creates/initiates the transaction
2. **CHECKER**: Reviews and approves the transaction

### Why Do We Use It?

✅ **Prevents Fraud**: No single person can complete a fraudulent transaction alone
✅ **Reduces Errors**: Second person catches mistakes
✅ **Regulatory Compliance**: Required by banking regulations
✅ **Audit Trail**: Clear record of who did what

### Transactions Requiring Maker-Checker

| Transaction | Maker | Checker | Threshold |
|-------------|-------|---------|-----------|
| **Loan Approval** | Loan Officer | Branch Manager | > 2,000,000 XAF |
| **Loan Disbursement** | Loan Officer | Branch Manager | > 5,000,000 XAF |
| **Loan Write-off** | Loan Officer | Branch Manager | All amounts |
| **Loan Reschedule** | Loan Officer | Branch Manager | > 1,000,000 XAF |
| **Savings Withdrawal** | Cashier | Branch Manager | > 1,000,000 XAF |
| **Savings Account Closure** | Cashier | Branch Manager | Balance > 500,000 XAF |
| **Client Activation** | Loan Officer | Branch Manager | All |
| **Client Transfer** | Loan Officer | Branch Manager | All |
| **Manual Journal Entry** | Accountant/Manager | Branch Manager | > 500,000 XAF |
| **Inter-Office Transfer** | Branch Manager | Head Office | > 3,000,000 XAF |
| **Create User** | Branch Manager | Head Office | All |

### For Makers (Loan Officers, Cashiers)

**When You Create a Transaction**:

1. You enter the transaction details normally
2. Click **Submit**
3. You see: **"Transaction pending approval"**
4. Transaction goes to Checker Inbox
5. You **CANNOT** approve your own transaction
6. Wait for checker to approve

**Best Practices**:
- ✅ Enter accurate information
- ✅ Attach supporting documents
- ✅ Add notes explaining the transaction
- ✅ Notify checker that task is waiting
- ❌ Do NOT pressure checker to approve
- ❌ Do NOT try to bypass the system

### For Checkers (Branch Managers)

**When You Approve a Transaction**:

1. Go to **Admin → System → Checker Inbox & Tasks**
2. You see list of pending tasks:
   - Task type (Loan Approval, Withdrawal, etc.)
   - Maker name
   - Amount
   - Date submitted
3. Click on task to review details
4. Verify:
   - Information is accurate
   - Supporting documents are attached
   - Transaction makes business sense
   - Maker followed procedures
5. Decision:
   - **Approve**: Click **Approve** button
   - **Reject**: Click **Reject**, add reason

**Best Practices**:
- ✅ Review thoroughly (don't rubber-stamp)
- ✅ Ask questions if something unclear
- ✅ Check supporting documents
- ✅ Reject if not satisfied
- ✅ Clear inbox daily (don't let tasks pile up)
- ❌ Do NOT approve without proper review
- ❌ Do NOT approve your own tasks (if you accidentally create one)

### Example Scenario

**Large Withdrawal Request**:

**Client**: Madame Fotso wants to withdraw 1,500,000 XAF from her savings account.

**Step 1 - Cashier (MAKER)**:
- Cashier: "Madame, let me get my manager's approval for this amount."
- Cashier verifies client ID
- Cashier initiates withdrawal in Fineract
- Transaction status: **Pending Approval**
- Cashier: "Madame, please wait a moment while my manager approves this."

**Step 2 - Branch Manager (CHECKER)**:
- Manager checks Checker Inbox
- Manager sees: "Withdrawal - 1,500,000 XAF - Madame Fotso - Cashier Jean"
- Manager reviews:
  - Account balance: 2,000,000 XAF (sufficient)
  - Client ID matches (asks cashier to re-verify)
  - No recent suspicious activity
  - Withdrawal reason: "Business inventory purchase"
- Manager clicks **Approve**

**Step 3 - Cashier Completes**:
- Cashier sees approval notification
- Cashier counts 1,500,000 XAF cash
- Madame Fotso counts and verifies
- Madame Fotso signs withdrawal slip
- Cashier gives receipt
- Transaction complete

**Total Time**: 10-15 minutes

---

## Inter-Branch Transactions

### Why Inter-Branch Matters

Our organization has **4 branches**:
- Head Office (Yaounde)
- Douala Branch
- Bafoussam Branch
- Bamenda Branch

**Business Scenario**:
- Client has savings account at Douala Branch
- Client travels to Bamenda for business
- Client needs to withdraw cash in Bamenda

**Solution**: Inter-branch transactions allow clients to transact at any branch.

### How to Handle Inter-Branch Transactions (for Cashiers)

#### The Golden Rule: Use Account Numbers

🔑 **KEY PRINCIPLE**: Always search by account number, NEVER by client name for inter-branch transactions.

**Why?**
- You can only see clients from your own branch when searching by name
- Account numbers work across all branches
- This is how the system is designed for security

#### Step-by-Step Process

**Scenario**: Client from Douala wants to withdraw at Bamenda

**Step 1 - Client Arrival**:
- Client: "I have an account in Douala, but I'm visiting Bamenda. Can I withdraw?"
- You: "Yes, Madame/Sir! May I have your account number please?"

**Step 2 - Client Provides Account Number**:
- Client shows: `VOL00000003` (written on passbook or phone)
- If client doesn't know account number:
  - Ask for National ID number
  - Search by external ID or mobile number

**Step 3 - Search in Fineract**:
```
Navigate to: Savings Accounts → Search
Enter: VOL00000003
Click: Search
```

**Step 4 - Verify Account Found**:
- System shows account details
- Client name: Madame Fotso
- Home office: Douala Branch
- Current balance: 850,000 XAF

**Step 5 - Verify Client Identity**:
- Ask for National ID card
- Verify name matches: Fotso
- Verify photo matches client

**Step 6 - Process Transaction**:
- Client requests: 200,000 XAF withdrawal
- Click **Withdraw**
- Enter amount: 200,000
- Payment type: Cash
- Click **Submit**

**Step 7 - System Handles Inter-Branch Accounting**:

Automatically posts:

**Your Branch (Bamenda)**:
- DR GL 42 (Cash on Hand): -200,000 XAF *(you paid cash)*
- CR GL 122 (Due from Douala): 200,000 XAF *(Douala owes us)*

**Client's Home Branch (Douala)**:
- DR GL 131 (Due to Bamenda): 200,000 XAF *(Douala owes Bamenda)*
- CR GL 32 (Customer Deposits): -200,000 XAF *(client's balance reduced)*

**Step 8 - Complete Transaction**:
- Count cash: 200,000 XAF
- Client verifies
- Give receipt showing:
  - Transaction date
  - Amount withdrawn
  - Remaining balance: 650,000 XAF
  - Transaction location: Bamenda Branch

### Alternative Identifiers

If client doesn't have account number, search by:

| Identifier | How to Search | Example |
|------------|---------------|---------|
| **Mobile Number** | Clients → Search → Mobile No | +237670123456 |
| **External ID** | Clients → Search → External ID | CLI-DLA-001 |
| **National ID** | Clients → Search → Custom Field | 123456789 |

### What NOT to Do

❌ **WRONG**: Search by client name
- Why? You cannot see clients from other branches by name
- System security prevents cross-branch name search

❌ **WRONG**: Tell client you can't help them
- Inter-branch transactions are designed to work
- Always search by account number first

❌ **WRONG**: Process without verifying ID
- Always verify client identity
- Fraudsters may know account numbers

### Frequently Asked Questions

**Q: Why can't I find a client from another branch by name?**

A: For security reasons, the system only shows clients from your own branch when you search by name. This protects client privacy and prevents unauthorized access. Always search by account number for inter-branch transactions.

**Q: What if the client doesn't know their account number?**

A: Ask for:
1. Mobile number registered with the account
2. National ID number
3. External ID (if they know it)
4. Call their home branch to get account number

**Q: Is there a limit on inter-branch transactions?**

A: Same limits apply:
- Withdrawals > 1,000,000 XAF need manager approval
- Check product-specific withdrawal limits
- Verify sufficient balance

**Q: How does the accounting work?**

A: The system automatically creates inter-branch clearing entries using GL 122 (Due from Other Branches) and GL 131 (Due to Other Branches). These accounts are reconciled monthly by the accounting department.

---

## Reports and Monitoring

### Daily Reports (All Staff)

**1. My Tasks Summary**

Navigate to: **Dashboard**

View:
- Pending approvals (managers only)
- Clients assigned to you
- Overdue loans (loan officers)
- Today's transactions

**2. Transaction Journal**

Navigate to: **Reports → Transaction Reports → Daily Transaction Journal**

Shows all transactions for the day:
- Deposits
- Withdrawals
- Loan repayments
- Loan disbursements

### Weekly Reports

**For Loan Officers**:

**1. Loans in Arrears**

Navigate to: **Reports → Loans → Loans in Arrears**

Shows overdue loans:
- Client name
- Loan account number
- Days overdue
- Amount overdue

Action: Follow up with clients.

**2. Loan Disbursements**

Navigate to: **Reports → Loans → Loan Disbursement Report**

Shows all loans disbursed this week:
- Track against your weekly target
- Monitor product mix

**For Branch Managers**:

**1. Portfolio at Risk (PAR)**

Navigate to: **Reports → Loans → Portfolio at Risk**

Shows:
- PAR 30 (loans 30+ days overdue)
- PAR 60, PAR 90
- Total portfolio quality

Industry benchmark: PAR 30 should be < 5%

**2. Branch Performance Dashboard**

Navigate to: **Reports → Dashboard Reports → Branch Summary**

Shows:
- Total active loans
- Total savings balance
- Number of active clients
- Cash position

### Monthly Reports

**For All Staff**:

**1. Client List Report**

Navigate to: **Reports → Clients → Client Listing**

Filter by:
- Your office
- Active clients only
- Date range

Export to Excel for analysis.

**2. Product Performance**

Navigate to: **Reports → Products → Product Utilization**

Shows:
- Number of accounts per product
- Total balances per product
- Average loan size

### Generating Reports

**Step-by-Step**:

1. Navigate to **Reports** menu
2. Select report category
3. Select specific report
4. Set parameters:
   - Date range
   - Office
   - Product (optional)
   - Staff (optional)
5. Click **Run Report**
6. View on screen or export to:
   - PDF (for printing)
   - Excel (for analysis)
   - CSV (for import to other systems)

---

## Security Best Practices

### Password Security

**DO**:
- ✅ Use a strong, unique password (12+ characters, mixed case, numbers, symbols)
- ✅ Change password every 90 days
- ✅ Log out when leaving your desk
- ✅ Lock your computer screen (Windows: Ctrl+Alt+Del → Lock)

**DON'T**:
- ❌ Share your password with anyone (even colleagues, even managers)
- ❌ Write password on paper
- ❌ Use same password for multiple accounts
- ❌ Use simple passwords (name, birthday, "password123")
- ❌ Let someone watch you type your password

### System Access Security

**DO**:
- ✅ Log out at end of day
- ✅ Use only your assigned account
- ✅ Report suspicious activity immediately
- ✅ Keep browser up to date

**DON'T**:
- ❌ Let colleagues use your login
- ❌ Stay logged in on shared computers
- ❌ Access Fineract from public WiFi (use office network only)
- ❌ Access from personal devices (use office computers only)

### Transaction Security

**DO**:
- ✅ Verify client identity for every transaction
- ✅ Check account number matches client name
- ✅ Ask questions if transaction seems unusual
- ✅ Report suspicious patterns to manager
- ✅ Document everything in client notes

**DON'T**:
- ❌ Process transactions for clients you can't identify
- ❌ Accept photocopies of ID cards (original only)
- ❌ Process large transactions without manager approval
- ❌ Skip verification steps to save time
- ❌ Process transactions for family/friends without proper procedures

### Cash Security (for Cashiers)

**DO**:
- ✅ Count cash twice (once when receiving, once before giving)
- ✅ Keep cash drawer locked when not in use
- ✅ Limit cash in drawer to allocated amount
- ✅ Report shortages immediately
- ✅ Follow dual custody procedures (two people present for large amounts)

**DON'T**:
- ❌ Leave cash drawer open and unattended
- ❌ Store personal cash in teller drawer
- ❌ Accept torn or suspicious notes without manager approval
- ❌ Give cash before completing transaction in system
- ❌ Take cash home (all cash must be in vault overnight)

### Fraud Awareness

**Common Fraud Schemes**:

🚨 **Identity Theft**:
- Fraudster uses stolen ID to open account or withdraw
- **Prevention**: Verify ID carefully, ask security questions

🚨 **Account Takeover**:
- Fraudster changes client's mobile number, then withdraws
- **Prevention**: Verify ID for all profile changes

🚨 **Insider Fraud**:
- Staff member creates fake clients or loans
- **Prevention**: Maker-Checker, regular audits

🚨 **Loan Fraud**:
- Client provides fake documents or collateral
- **Prevention**: Field visits, verify collateral, check references

**If You Suspect Fraud**:
1. Stay calm and professional
2. Do NOT confront the suspected fraudster
3. Complete the transaction if safe to do so
4. Immediately report to Branch Manager
5. Document everything you observed
6. Do NOT discuss with other staff (confidentiality)

---

## Troubleshooting

### Common Issues and Solutions

#### "I can't log in"

**Possible Causes**:
1. Wrong username or password
2. Account locked (too many failed attempts)
3. Password expired

**Solutions**:
1. Double-check username and password (check caps lock)
2. If locked: Contact Branch Manager to unlock
3. If expired: Click "Reset Password" link, follow instructions

#### "I can't find a client"

**Possible Causes**:
1. Client is at different branch (and you searched by name)
2. Typo in search term
3. Client not yet created

**Solutions**:
1. For inter-branch: Search by account number, not name
2. Try searching by:
   - Account number
   - Mobile number
   - External ID
3. Check if Loan Officer created the client yet

#### "Transaction failed"

**Possible Causes**:
1. Insufficient balance
2. Account is locked/frozen
3. Network issue
4. Maker-Checker approval needed

**Solutions**:
1. Check account balance
2. Check account status (should be "Active")
3. Retry after a moment
4. Check Checker Inbox if you're a manager

#### "Cash doesn't balance at end of day"

**Possible Causes**:
1. Miscounted cash
2. Transaction not entered in system
3. Cash given without completing transaction
4. Incorrect denomination calculation

**Solutions**:
1. Recount cash carefully
2. Review today's transactions in system
3. Check for pending transactions
4. Separate by denomination and recount
5. Report to Branch Manager (don't try to hide shortage)

#### "Report won't generate"

**Possible Causes**:
1. Date range too large
2. No data for selected parameters
3. Missing required parameter

**Solutions**:
1. Reduce date range (e.g., one month at a time)
2. Check filters (maybe no data matches)
3. Fill all required fields (marked with *)

### Getting Help

**Level 1 - Colleague**:
- Ask a colleague at your branch
- Good for quick questions about procedures

**Level 2 - Branch Manager**:
- Escalate to Branch Manager for:
  - System issues
  - Approval questions
  - Complex transactions

**Level 3 - IT Support**:
- Contact IT Support for:
  - Login problems
  - System errors
  - Network issues
  - Report problems

**Level 4 - Head Office**:
- Escalate to Head Office for:
  - Policy questions
  - Major system issues
  - Security incidents

**How to Report Issues**:

📧 **Email**: support@your-organization.cm

📞 **Phone**: [Support Number]

**Include**:
- Your name and branch
- Date and time issue occurred
- What you were trying to do
- Error message (if any)
- Screenshots (if possible)

---

## Conclusion

### Key Takeaways

✅ **Know Your Role**: Understand your permissions and responsibilities

✅ **Security First**: Protect passwords, verify identities, report fraud

✅ **Maker-Checker**: Two people for sensitive transactions (prevents fraud)

✅ **Inter-Branch**: Use account numbers to find clients from other branches

✅ **Daily Routine**: Follow daily cash management procedures

✅ **Ask Questions**: Don't guess - ask your manager if unsure

### Training Completion Checklist

Before you start working independently, make sure you can:

**For All Staff**:
- [ ] Log in and change your password
- [ ] Navigate the main menu
- [ ] Search for clients
- [ ] View account details
- [ ] Generate basic reports
- [ ] Log out securely

**For Loan Officers**:
- [ ] Create a new client
- [ ] Activate a client (with manager approval)
- [ ] Create a loan application
- [ ] Process loan approval
- [ ] Disburse a loan
- [ ] Create a savings account
- [ ] Track loan repayments

**For Cashiers**:
- [ ] Process a savings deposit
- [ ] Process a savings withdrawal
- [ ] Accept a loan repayment
- [ ] Handle large withdrawal approvals
- [ ] Process inter-branch transactions
- [ ] Count and settle teller cash daily

**For Branch Managers**:
- [ ] Approve loan applications
- [ ] Approve large withdrawals
- [ ] Allocate cash to tellers
- [ ] Settle tellers at end of day
- [ ] Record cash shortage/overage
- [ ] Use Checker Inbox
- [ ] Generate branch reports

### Next Steps

1. **Shadow Training**: Work alongside experienced staff for 1-2 weeks
2. **Supervised Practice**: Perform transactions with manager supervision
3. **Independent Work**: Start working independently with manager support
4. **Ongoing Learning**: Attend monthly training sessions

### Questions?

Don't hesitate to ask questions! It's better to ask than to make a mistake.

**Your Branch Manager is here to help you succeed!**

---

**End of Staff Training Guide**

**Version**: 2.0
**Last Updated**: October 14, 2025
**Next Review**: January 2026

---

## Appendix: Quick Reference Cards

### Quick Reference: Cashier Daily Tasks

**Morning**:
- [ ] Receive cash allocation (count and verify)
- [ ] Sign allocation receipt
- [ ] Secure cash in drawer

**During Day**:
- [ ] Process deposits (count → enter → receipt)
- [ ] Process withdrawals (verify ID → enter → count cash → receipt)
- [ ] Accept repayments (account number → enter → receipt)
- [ ] Large withdrawals: get manager approval

**Evening**:
- [ ] Stop transactions 30 min before closing
- [ ] Count cash
- [ ] Report to manager for settlement
- [ ] Return cash to vault

### Quick Reference: Loan Officer Daily Tasks

**Morning**:
- [ ] Check pending approvals status
- [ ] Review today's scheduled field visits

**During Day**:
- [ ] Meet with clients/prospects
- [ ] Create new clients (with proper KYC)
- [ ] Process loan applications
- [ ] Follow up on overdue loans
- [ ] Conduct field appraisals

**Evening**:
- [ ] Update client notes in system
- [ ] Document field visits
- [ ] Review loan portfolio status

### Quick Reference: Branch Manager Daily Tasks

**Morning**:
- [ ] Check Checker Inbox
- [ ] Allocate cash to tellers
- [ ] Review yesterday's reports

**During Day**:
- [ ] Approve pending tasks
- [ ] Supervise staff
- [ ] Handle escalations
- [ ] Review large transactions

**Evening**:
- [ ] Settle tellers
- [ ] Generate daily reports
- [ ] Secure cash in vault
- [ ] Review tomorrow's schedule
