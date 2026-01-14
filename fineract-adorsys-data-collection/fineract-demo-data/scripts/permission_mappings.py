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
    'READ_BUSINESS_DATE'
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
            perms.extend(['CREATE_CLIENT', 'READ_CLIENT', 'UPDATE_CLIENT', 'ACTIVATE_CLIENT', 'CLOSE_CLIENT', 'REJECT_CLIENT'])
            # Loans
            perms.extend(['APPROVE_LOAN', 'DISBURSE_LOAN', 'REJECT_LOAN', 'WITHDRAW_LOAN', 'READ_LOAN', 'REPAYMENT_LOAN'])
            # Savings
            perms.extend(['APPROVE_SAVINGSACCOUNT', 'ACTIVATE_SAVINGSACCOUNT', 'REJECT_SAVINGSACCOUNT', 'WITHDRAW_SAVINGSACCOUNT', 'READ_SAVINGSACCOUNT'])
            # Client Charges
            perms.extend(['READ_CLIENTCHARGE'])
            # Teller/Cashier Management
            perms.extend([
                'READ_TELLER',  # Required for menu visibility in UI
                'READ_CASHIER', # Required for cashier data access
                'CREATE_TELLER', 'UPDATE_TELLER', 'DELETE_TELLER',
                'ALLOCATECASHIER_TELLER', 'ALLOCATECASHTOCASHIER_TELLER',
                'SETTLECASHFROMCASHIER_TELLER', 'UPDATECASHIERALLOCATION_TELLER',
                'DELETECASHIERALLOCATION_TELLER'
            ])
            # Reports (All Read)
            perms.extend(['READ_REPORT']) # This might be too broad if specific reports needed, but "READ (All)" usually implies this + specific report reads
            # We add ALL read reports just in case, or rely on ALL_FUNCTIONS_READ for reports? 
            # Fineract has specific report permissions e.g. 'READ_Client Listing'
            # For simplicity, we might leave specific report permissions for now or map 'READ_REPORT'
            return perms
            
        elif shorthand == 'LOAN_OFFICER':
            perms = list(COMMON_READ_PERMISSIONS)
            # Clients
            perms.extend(['CREATE_CLIENT', 'READ_CLIENT', 'UPDATE_CLIENT'])
            # Loans
            perms.extend(['CREATE_LOAN', 'READ_LOAN', 'UPDATE_LOAN', 'REPAYMENT_LOAN', 'DISBURSE_LOAN', 'DISBURSETOSAVINGS_LOAN', 'READ_LOANNOTE'])
            # Savings
            perms.extend(['READ_SAVINGSACCOUNT'])
            # Client Charges
            perms.extend(['READ_CLIENTCHARGE'])
            # Portfolio Reports (Sample list)
            perms.extend(['READ_Client Listing', 'READ_Active Loans - Summary', 'READ_Loans Awaiting Disbursal', 'READ_Portfolio at Risk'])
            return perms
            
        elif shorthand == 'CASHIER':
            perms = list(COMMON_READ_PERMISSIONS)
            # Clients
            perms.extend(['READ_CLIENT'])
            # Savings
            perms.extend(['DEPOSIT_SAVINGSACCOUNT', 'WITHDRAWAL_SAVINGSACCOUNT', 'READ_SAVINGSACCOUNT'])
            # Loans
            perms.extend(['REPAYMENT_LOAN', 'READ_LOAN'])
            # Teller
            perms.extend(['CREATE_TELLER', 'UPDATE_TELLER', 'ALLOCATECASHIER_TELLER', 'ALLOCATECASHTOCASHIER_TELLER', 'SETTLECASHFROMCASHIER_TELLER'])
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
            # Reports
            perms.extend(['READ_Balance Sheet', 'READ_Income Statement', 'READ_Trial Balance', 'READ_General Ledger Report'])
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
