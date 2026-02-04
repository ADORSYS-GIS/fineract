"""
Permission mappings for Fineract Demo Data.
Maps simplified permission groups and shorthands to actual Fineract permission codes.
"""

def get_entity_name(group):
    """Maps permission group name to Fineract entity name suffix."""
    mapping = {
        'Client': 'CLIENT',
        'Loan': 'LOAN',
        'Savings': 'SAVINGSACCOUNT',
        'Office': 'OFFICE',
        'Staff': 'STAFF',
        'User': 'USER',
        'Role': 'ROLE',
        'Loan Product': 'LOANPRODUCT',
        'Savings Product': 'SAVINGSPRODUCT',
        'Charge': 'CHARGE',
        'PaymentType': 'PAYMENTTYPE',
        'Code': 'CODE',
        'Configuration': 'CONFIGURATION',
        'Accounting': 'GLACCOUNT', # Partial mapping, Accounting is complex
        'Report': 'REPORT',
        'Teller': 'TELLER',
        'Client Identifier': 'CLIENTIDENTIFIER',
        'Client Image': 'CLIENTIMAGE',
        'Staff Image': 'STAFFIMAGE',
    }
    return mapping.get(group)

def expand_permissions(group, shorthand):
    """
    Expands a shorthand permission code into a list of actual Fineract permission codes.
    
    Args:
        group (str): The permission group (e.g., 'Client', 'Loan')
        shorthand (str): The shorthand code (e.g., 'ALL_FUNCTIONS', 'CREATE_UPDATE_READ')
        
    Returns:
        list: List of valid permission strings.
    """

COMMON_READ_PERMISSIONS = [
    'READ_PAYMENTTYPE',
    'READ_CONFIGURATION',
    'READ_CALENDAR',
    'READ_CHARGE',
    'READ_EMAIL',
    'READ_OFFICE',
    'READ_STAFF',
    'READ_USER',
    'READ_ROLE',
    'READ_CODE',
    'READ_CODEVALUE',
    'READ_BUSINESS_DATE',
    'READ_CURRENCY',
    'READ_REPORT',
    'READ_CLIENTIDENTIFIER',
    'READ_CLIENTIMAGE',
    'READ_STAFFIMAGE',
    'REPORTING_SUPER_USER',
    'READ_Loan Transaction Receipt',
    'READ_Savings Transaction Receipt',
    # Financial Reports
    'READ_Income Statement (Profit & Loss)',
    'READ_Balance Sheet',
    'READ_General Ledger Report',
    'READ_Trial Balance',
    'READ_Income Statement',
    # Portfolio Reports
    'READ_Portfolio Performance Statistics',
    'READ_Portfolio Analysis by Product',
    'READ_Portfolio Analysis by Branch',
    'READ_Portfolio at Risk',
    'READ_Portfolio at Risk(Pentaho)',
    'READ_Portfolio at Risk by Branch',
    'READ_Portfolio at Risk by Branch(Pentaho)',
    'READ_Active Clients Summary',
    'READ_Active Clients - Email',
    'READ_Active Group Leaders - Email',
    'READ_Active Loan Clients - Email',
    'READ_Active Loan Summary per Branch',
    'READ_Active Loans - Details',
    'READ_Active Loans - Summary',
    'READ_Active Loans - Summary(Pentaho)',
    'READ_Active Loans by Disbursal Period',
    'READ_Active Loans by Disbursal Period(Pentaho)',
    'READ_Active Loans in last installment',
    'READ_Active Loans in last installment Summary',
    'READ_Active Loans in last installment Summary(Pentaho)',
    'READ_Active Loans in last installment(Pentaho)',
    'READ_Active Loans Passed Final Maturity',
    'READ_Active Loans Passed Final Maturity Summary',
    'READ_Active Loans Passed Final Maturity Summary(Pentaho)',
    'READ_Active Loans Passed Final Maturity(Pentaho)',
    'READ_Aging Detail',
    'READ_Aging Detail(Pentaho)',
    'READ_Aging Summary (Arrears in Months)',
    'READ_Aging Summary (Arrears in Months)(Pentaho)',
    'READ_Aging Summary (Arrears in Weeks)',
    'READ_Aging Summary (Arrears in Weeks)(Pentaho)',
    'READ_Balance Outstanding',
    'READ_Branch Expected Cash Flow',
    'READ_BranchManagerStats',
    'READ_ChildrenStaffList',
    'READ_Client Listing',
    'READ_Client Listing(Pentaho)',
    'READ_Client Loan Account Schedule',
    'READ_Client Loans Listing',
    'READ_Client Loans Listing(Pentaho)',
    'READ_Client Saving Transactions',
    'READ_Client Savings Summary',
    'READ_ClientSummary ',
    'READ_ClientTrendsByDay',
    'READ_ClientTrendsByMonth',
    'READ_ClientTrendsByWeek',
    'READ_Collection Report',
    'READ_CoordinatorStats',
    'READ_Demand_Vs_Collection',
    'READ_Disbursal Report',
    'READ_Disbursal_Vs_Awaitingdisbursal',
    'READ_Dormant Prospects - Email',
    'READ_Expected Payments By Date - Basic',
    'READ_Expected Payments By Date - Basic(Pentaho)',
    'READ_Expected Payments By Date - Formatted',
    'READ_FieldAgentPrograms',
    'READ_FieldAgentStats',
    'READ_Funds Disbursed Between Dates Summary',
    'READ_Funds Disbursed Between Dates Summary(Pentaho)',
    'READ_Funds Disbursed Between Dates Summary by Office',
    'READ_Funds Disbursed Between Dates Summary by Office(Pentaho)',
    'READ_GroupNamesByStaff',
    'READ_GroupSavingSummary',
    'READ_GroupSummaryAmounts',
    'READ_GroupSummaryCounts',
    'READ_Happy Birthday - Email',
    'READ_Loan Account Schedule',
    'READ_Loan Approved - Email',
    'READ_Loan Fully Repaid - Email',
    'READ_Loan payments due - Email',
    'READ_Loan Payments Due (Overdue Loans) - Email',
    'READ_Loan Payments Received (Active Loans) - Email',
    'READ_Loan Payments Received (Overdue Loans)  - Email',
    'READ_Loan Rejected - Email',
    'READ_Loan Repayment - Email',
    'READ_Loan Repayment Collections',
    'READ_LoanCyclePerProduct',
    'READ_Loans Awaiting Disbursal',
    'READ_Loans Awaiting Disbursal(Pentaho)',
    'READ_Loans Awaiting Disbursal Summary',
    'READ_Loans Awaiting Disbursal Summary(Pentaho)',
    'READ_Loans Awaiting Disbursal Summary by Month',
    'READ_Loans Awaiting Disbursal Summary by Month(Pentaho)',
    'READ_Loans disbursed to clients - Email',
    'READ_Loans in arrears - Email',
    'READ_Loans Outstanding after final instalment date - Email',
    'READ_Loans Pending Approval',
    'READ_Loans Pending Approval(Pentaho)',
    'READ_LoanTrendsByDay',
    'READ_LoanTrendsByMonth',
    'READ_LoanTrendsByWeek',
    'READ_Obligation Met Loans Details',
    'READ_Obligation Met Loans Details(Pentaho)',
    'READ_Obligation Met Loans Summary',
    'READ_Obligation Met Loans Summary(Pentaho)',
    'READ_Overdue Loans Report',
    'READ_ProgramDetails',
    'READ_ProgramDirectorStats',
    'READ_ProgramStats',
    'READ_Prospective Clients - Email',
    'READ_Rescheduled Loans',
    'READ_Rescheduled Loans(Pentaho)',
    'READ_Savings Accounts Summary',
    'READ_Savings Transactions',
    'READ_Staff Assignment History(Pentaho)',
    'READ_TxnRunningBalances',
    'READ_TxnRunningBalances(Pentaho)',
    'READ_Written-Off Loans',
    'READ_Written-Off Loans(Pentaho)',
    # Regulatory Reports (COBAC)
    'READ_COBAC R01 - Portfolio Quality',
    'READ_COBAC R02 - Loan Provisioning',
    'READ_COBAC R03 - Capital Adequacy',
    'READ_COBAC R04 - Liquidity Position',
    'READ_COBAC R05 - Large Exposures',
    'READ_COBAC R06 - Balance Sheet',
    'READ_COBAC R07 - Income Statement',
    'READ_COBAC R08 - Arrears Report',
    'READ_COBAC R09 - Portfolio at Risk',
    'READ_COBAC R10 - Related Party Transactions'
]

def expand_permissions(group, shorthand):
    """
    Expands a shorthand permission code into a list of actual Fineract permission codes.
    
    Args:
        group (str): The permission group (e.g., 'Client', 'Loan')
        shorthand (str): The shorthand code (e.g., 'ALL_FUNCTIONS', 'CREATE_UPDATE_READ')
        
    Returns:
        list: List of valid permission strings.
    """
    if not shorthand:
        return []

    # Global fix for known incorrect permissions
    if shorthand == 'READ_BUSINESSDATE':
        return ['READ_BUSINESS_DATE']
    if shorthand == 'UPDATE_BUSINESSDATE':
        return ['UPDATE_BUSINESS_DATE']
        
    # Special Role Mappings
    if group == 'Role':
        if shorthand == 'SUPER_USER':
            return ['ALL_FUNCTIONS']
            
        elif shorthand == 'BRANCH_MANAGER':
            perms = list(COMMON_READ_PERMISSIONS)
            # Clients
            perms.extend(['CREATE_CLIENT', 'READ_CLIENT', 'UPDATE_CLIENT', 'ACTIVATE_CLIENT', 'CLOSE_CLIENT', 'REJECT_CLIENT', 'READ_client_additional_info'])
            # Documents
            perms.extend(['READ_DOCUMENT'])
            # Loans
            perms.extend(['APPROVE_LOAN', 'DISBURSE_LOAN', 'REJECT_LOAN', 'WITHDRAW_LOAN', 'READ_LOAN', 'REPAYMENT_LOAN', "READ_loan_additional_info", "CREATE_loan_additional_info"])
            # Savings
            perms.extend([
                'READ_SAVINGSACCOUNT',
                'READ_SAVINGSPRODUCT',
                'APPROVE_SAVINGSACCOUNT',
                'REJECT_SAVINGSACCOUNT', 'WITHDRAW_SAVINGSACCOUNT',
                'READ_SAVINGSADDITIONALINFO', 'UPDATE_SAVINGSADDITIONALINFO', 'CREATE_SAVINGSADDITIONALINFO', "READ_savings_additional_info", "UPDATE_savings_additional_info"
            ])
            # Client Charges
            perms.extend(['READ_CLIENTCHARGE'])
            # Guarantor & Collateral (Read only)
            perms.extend(['READ_GUARANTOR', 'READ_COLLATERAL'])
            # Teller/Cashier Management
            perms.extend([
                'READ_TELLER',  
                'READ_CASHIER',
                'CREATE_TELLER', 'UPDATE_TELLER', 'DELETE_TELLER',
                'ALLOCATECASHIER_TELLER', 'ALLOCATECASHTOCASHIER_TELLER',
                'SETTLECASHFROMCASHIER_TELLER', 'UPDATECASHIERALLOCATION_TELLER',
                'DELETECASHIERALLOCATION_TELLER'
            ])
            # Reports
            perms.extend([
                'READ_Portfolio Performance Statistics',
                'READ_Portfolio Analysis by Product',
                'READ_Portfolio Analysis by Branch',
                'READ_Active Clients Summary',
                'READ_Balance Sheet',
                'READ_Income Statement (Profit & Loss)'
            ])
            return perms
            
        elif shorthand == 'LOAN_OFFICER':
            perms = list(COMMON_READ_PERMISSIONS)
            # Clients
            perms.extend(['CREATE_CLIENT', 'READ_CLIENT', 'UPDATE_CLIENT', 'ACTIVATE_CLIENT'])
            # Documents
            perms.extend(['READ_DOCUMENT'])
            # Loans
            perms.extend(['CREATE_LOAN', 'READ_LOAN', 'UPDATE_LOAN', 'REPAYMENT_LOAN', 'DISBURSE_LOAN', 'DISBURSETOSAVINGS_LOAN', 'READ_LOANNOTE'])
            # Savings
            perms.extend(['READ_SAVINGSACCOUNT', 'READ_SAVINGSPRODUCT', 'ACTIVATE_SAVINGSACCOUNT', 'CREATE_SAVINGSACCOUNT', 'UPDATE_SAVINGSACCOUNT'])
            # Client Charges
            perms.extend(['READ_CLIENTCHARGE'])
            # Guarantor (CRUD)
            perms.extend(['CREATE_GUARANTOR', 'READ_GUARANTOR', 'UPDATE_GUARANTOR', 'DELETE_GUARANTOR'])
            # Collateral (CRUD)
            perms.extend(['CREATE_COLLATERAL', 'READ_COLLATERAL', 'UPDATE_COLLATERAL', 'DELETE_COLLATERAL'])
            # Portfolio Reports
            perms.extend([
                'READ_Portfolio Performance Statistics',
                'READ_Portfolio Analysis by Product',
                'READ_Portfolio Analysis by Branch',
                'READ_Active Clients Summary',
                'READ_Client Listing', 
                'READ_Active Loans - Summary', 
                'READ_Loans Awaiting Disbursal', 
                'READ_Portfolio at Risk'
            ])
            return perms
            
        elif shorthand == 'CASHIER':
            perms = list(COMMON_READ_PERMISSIONS)
            # Clients
            perms.extend(['READ_CLIENT'])
            # Client Charges
            perms.extend(['READ_CLIENTCHARGE'])
            # Savings
            perms.extend(['DEPOSIT_SAVINGSACCOUNT', 'WITHDRAWAL_SAVINGSACCOUNT', 'READ_SAVINGSACCOUNT'])
            # Loans
            perms.extend(['REPAYMENT_LOAN', 'READ_LOAN'])
            # Teller Checker Permissions (for self-approval workflow)
            perms.extend(['ALLOCATECASHIER_TELLER_CHECKER', 'SETTLECASHFROMCASHIER_TELLER_CHECKER', 'UPDATECASHIERALLOCATION_TELLER_CHECKER'])
            return perms
            
        elif shorthand == 'ACCOUNTANT':
            perms = list(COMMON_READ_PERMISSIONS)
            # Accounting - ALL (Create, Read, Update, Delete, Journal Entries)
            perms.extend([
                'CREATE_GLACCOUNT', 'READ_GLACCOUNT', 'UPDATE_GLACCOUNT', 'DELETE_GLACCOUNT',
                'CREATE_JOURNALENTRY', 'READ_JOURNALENTRY', 'REVERSE_JOURNALENTRY', 
                'DEFINEOPENINGBALANCE_JOURNALENTRY', 'UPDATEOPENINGBALANCE_JOURNALENTRY',
                'CREATE_GLCLOSURE', 'READ_GLCLOSURE', 'UPDATE_GLCLOSURE', 'DELETE_GLCLOSURE',
                'CREATE_ACCOUNTINGRULE', 'READ_ACCOUNTINGRULE', 'UPDATE_ACCOUNTINGRULE', 'DELETE_ACCOUNTINGRULE'
            ])
            # Reports (Financials)
            perms.extend([
                'READ_Balance Sheet', 
                'READ_Income Statement (Profit & Loss)', 
                'READ_Trial Balance', 
                'READ_General Ledger Report'
            ])
            # Organization
            perms.extend(['READ_OFFICE', 'READ_STAFF', 'READ_CURRENCY'])
            # Audit
            perms.extend(['READ_AUDIT'])
            return perms
            
        elif shorthand == 'SUPERVISOR_ACCOUNTANT':
            perms = list(COMMON_READ_PERMISSIONS)
            # Accounting - ALL EXCEPT CREATE_JOURNALENTRY
            perms.extend([
                'CREATE_GLACCOUNT', 'READ_GLACCOUNT', 'UPDATE_GLACCOUNT', 'DELETE_GLACCOUNT',
                'READ_JOURNALENTRY', 'REVERSE_JOURNALENTRY', # NO CREATE_JOURNALENTRY
                'CREATE_JOURNALENTRY_CHECKER', # Checker permission to approve journal entries
                'DEFINEOPENINGBALANCE_JOURNALENTRY', 'UPDATEOPENINGBALANCE_JOURNALENTRY',
                'CREATE_GLCLOSURE', 'READ_GLCLOSURE', 'UPDATE_GLCLOSURE', 'DELETE_GLCLOSURE',
                'CREATE_ACCOUNTINGRULE', 'READ_ACCOUNTINGRULE', 'UPDATE_ACCOUNTINGRULE', 'DELETE_ACCOUNTINGRULE'
            ])
            # Reports
            perms.extend(['READ_Balance Sheet', 'READ_Income Statement', 'READ_Trial Balance', 'READ_General Ledger Report'])
            # Audit
            perms.extend(['READ_AUDIT'])
            return perms

    # specific direct mappings
    if group == 'special':
        # Pass through direct permissions
        return [shorthand]
    
    entity = get_entity_name(group)
    
    if group == 'Transaction':
        if shorthand == 'CREATE_READ':
            # Map to common transaction permissions
            return [
                'READ_LOAN', 
                'REPAYMENT_LOAN', 
                'READ_SAVINGSACCOUNT', 
                'DEPOSIT_SAVINGSACCOUNT', 
                'WITHDRAWAL_SAVINGSACCOUNT'
            ]
        elif shorthand == 'ALL_FUNCTIONS':
             return [
                'READ_LOAN', 'REPAYMENT_LOAN', 'UNDOREPAYMENT_LOAN', 'WAIVEINTEREST_LOAN', 'WRITEOFF_LOAN', 'CLOSE_LOAN',
                'READ_SAVINGSACCOUNT', 'DEPOSIT_SAVINGSACCOUNT', 'WITHDRAWAL_SAVINGSACCOUNT', 'UNDOWITHDRAWAL_SAVINGSACCOUNT'
            ]

    # Handle Accounting group specially
    if group == 'Accounting':
         if shorthand == 'CREATE_READ':
             return ['READ_GLACCOUNT', 'CREATE_JOURNALENTRY', 'READ_JOURNALENTRY']
         elif shorthand == 'ALL_FUNCTIONS':
             return ['CREATE_GLACCOUNT', 'READ_GLACCOUNT', 'UPDATE_GLACCOUNT', 'DELETE_GLACCOUNT',
                     'CREATE_JOURNALENTRY', 'READ_JOURNALENTRY', 'REVERSE_JOURNALENTRY']

    if not entity:
        # Fallback: check constants
        if shorthand == 'COMMON_READ':
            return COMMON_READ_PERMISSIONS
        if '_' in shorthand: 
             return [shorthand]
        return [shorthand]

    # Action mappings
    actions = []
    
    if shorthand == 'ALL_FUNCTIONS':
        actions = ['CREATE', 'READ', 'UPDATE', 'DELETE']
        if group == 'Client':
            actions.extend(['ACTIVATE', 'CLOSE', 'REJECT', 'WITHDRAW', 'REACTIVATE'])
        elif group == 'Loan':
            actions.extend(['APPROVE', 'DISBURSE', 'REPAYMENT', 'UNDOREPAYMENT', 'WAIVEINTEREST', 'WRITEOFF', 'CLOSE', 'REJECT', 'WITHDRAW', 'RECOVERGUARANTEE', 'DISBURSETOSAVINGS'])
        elif group == 'Savings':
            actions.extend(['APPROVE', 'ACTIVATE', 'DEPOSIT', 'WITHDRAWAL', 'REJECT', 'WITHDRAW', 'CLOSE', 'UNDOWITHDRAWAL'])
        elif group == 'Code':
            actions.extend(['CREATE_CODEVALUE', 'READ_CODEVALUE', 'UPDATE_CODEVALUE', 'DELETE_CODEVALUE'])

    elif shorthand == 'CREATE_UPDATE_READ':
        actions = ['CREATE', 'READ', 'UPDATE']
        
    elif shorthand == 'CREATE_READ':
        actions = ['CREATE', 'READ']
        
    elif shorthand == 'READ':
        actions = ['READ']
        
    elif shorthand == 'TRANSACTION':
         if group == 'Savings':
             actions = ['DEPOSIT', 'WITHDRAWAL']
             
    elif shorthand == 'DELETE_FUNCTIONS':
        actions = ['DELETE']
        
    elif shorthand == 'DISBURSE_REPAYMENT':
        if group == 'Loan':
             actions = ['DISBURSE', 'REPAYMENT', 'DISBURSETOSAVINGS']
             
    else:
         # Assume shorthand is a direct action or action suffix
         if '_' in shorthand:
             return [shorthand]
         actions = [shorthand]

    # Construct full permission strings
    permissions = []
    for action in actions:
        # Handle special cases where action includes entity or is custom
        if '_' in action and action not in ['CREATE_CODEVALUE', 'READ_CODEVALUE', 'UPDATE_CODEVALUE', 'DELETE_CODEVALUE']: # Exclude code values from duplicate check
             if action.endswith(entity): # Already has suffix
                 permissions.append(action)
             else:
                 permissions.append(f"{action}_{entity}")
        else:
            permissions.append(f"{action}_{entity}")
            
    return permissions
