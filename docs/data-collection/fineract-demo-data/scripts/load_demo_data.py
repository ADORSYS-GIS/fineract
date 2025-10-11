#!/usr/bin/env python3
"""
Fineract Demo Data Loader
Reads Excel template and loads data via Fineract APIs
"""

import pandas as pd
import requests
import json
import time
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional
import sys
import os

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('../logs/load_demo_data.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class FineractAPIClient:
    """Client for interacting with Fineract API"""

    def __init__(self, base_url: str, username: str, password: str, tenant: str = "default"):
        self.base_url = base_url.rstrip('/')
        self.username = username
        self.password = password
        self.tenant = tenant
        self.session = requests.Session()
        self.session.auth = (username, password)
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Fineract-Platform-TenantId': tenant
        })
        # Disable SSL verification for self-signed certificates (development only)
        self.session.verify = False
        # Suppress SSL warnings
        import urllib3
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

        # Storage for created entities
        self.created_offices = {}
        self.created_staff = {}
        self.created_clients = {}
        self.created_loan_products = {}
        self.created_savings_products = {}
        self.created_charges = {}
        self.created_gl_accounts = {}
        self.created_funds = {}
        self.created_payment_types = {}
        self.created_loan_accounts = {}
        self.created_savings_accounts = {}

    def _request(self, method: str, endpoint: str, data: Optional[Dict] = None,
                 params: Optional[Dict] = None) -> Dict:
        """Make HTTP request to Fineract API"""
        url = f"{self.base_url}{endpoint}"

        try:
            response = self.session.request(
                method=method,
                url=url,
                json=data,
                params=params
            )

            # Log request details
            logger.debug(f"{method} {url}")
            if data:
                logger.debug(f"Request data: {json.dumps(data, indent=2)}")

            response.raise_for_status()

            result = response.json() if response.text else {}
            logger.debug(f"Response: {json.dumps(result, indent=2)}")

            return result

        except requests.exceptions.HTTPError as e:
            logger.error(f"HTTP Error: {e}")
            logger.error(f"Response: {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"Request failed: {str(e)}")
            raise

    def get(self, endpoint: str, params: Optional[Dict] = None) -> Dict:
        """GET request"""
        return self._request('GET', endpoint, params=params)

    def post(self, endpoint: str, data: Dict) -> Dict:
        """POST request"""
        return self._request('POST', endpoint, data=data)

    def put(self, endpoint: str, data: Dict) -> Dict:
        """PUT request"""
        return self._request('PUT', endpoint, data=data)

    def delete(self, endpoint: str) -> Dict:
        """DELETE request"""
        return self._request('DELETE', endpoint)


class FineractDemoDataLoader:
    """Main class for loading demo data into Fineract"""

    def __init__(self, excel_file: str, config_file: str):
        self.excel_file = excel_file
        self.config_file = config_file
        self.config = self._load_config()
        self.client = FineractAPIClient(
            base_url=self.config['fineract_url'],
            username=self.config['username'],
            password=self.config['password'],
            tenant=self.config.get('tenant', 'default')
        )

    def _load_config(self) -> Dict:
        """Load configuration from JSON file"""
        with open(self.config_file, 'r') as f:
            return json.load(f)

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        # Replace NaN with None for proper JSON serialization
        df = df.where(pd.notnull(df), None)
        return df

    def load_offices(self):
        """Create offices/branches"""
        logger.info("=" * 80)
        logger.info("LOADING OFFICES")
        logger.info("=" * 80)

        # First, try to get existing offices from Fineract
        try:
            existing_offices_response = self.client.get('/offices')
            for office in existing_offices_response:
                self.client.created_offices[office['name']] = office['id']
            logger.info(f"Found {len(existing_offices_response)} existing offices in Fineract")

            # Map "Head Office Yaounde" to existing "Head Office" (ID: 1) if it exists
            if 'Head Office' in self.client.created_offices and 'Head Office Yaounde' not in self.client.created_offices:
                head_office_id = self.client.created_offices['Head Office']
                self.client.created_offices['Head Office Yaounde'] = head_office_id
                logger.info(f"✓ Using existing Head Office (ID: {head_office_id}) as 'Head Office Yaounde'")
        except Exception as e:
            logger.warning(f"Could not retrieve existing offices: {str(e)}")

        df = self._read_excel_sheet('Offices')

        for idx, row in df.iterrows():
            try:
                # Skip if office already exists
                if row['office_name'] in self.client.created_offices:
                    logger.info(f"⊙ Office already exists: {row['office_name']} (ID: {self.client.created_offices[row['office_name']]})")
                    continue

                # Determine parent office ID
                parent_id = None
                if row['parent_office']:
                    parent_id = self.client.created_offices.get(row['parent_office'])
                    if not parent_id:
                        logger.warning(f"Parent office '{row['parent_office']}' not found for {row['office_name']}, skipping...")
                        continue

                data = {
                    'name': row['office_name'],
                    'openingDate': row['opening_date'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en',
                    'externalId': row['external_id']
                }

                if parent_id:
                    data['parentId'] = parent_id

                response = self.client.post('/offices', data)
                office_id = response.get('resourceId') or response.get('officeId')

                self.client.created_offices[row['office_name']] = office_id

                logger.info(f"✓ Created office: {row['office_name']} (ID: {office_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create office {row['office_name']}: {str(e)}")
                # Check if it's a permission issue
                if '403' in str(e):
                    logger.error("  Permission denied. Possible causes:")
                    logger.error("  1. User doesn't have CREATE_OFFICE permission")
                    logger.error("  2. User is restricted to specific office hierarchy")
                    logger.error("  3. Try creating offices manually via Fineract UI first")

    def load_staff(self):
        """Create staff members"""
        logger.info("=" * 80)
        logger.info("LOADING STAFF")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Staff')

        for idx, row in df.iterrows():
            try:
                office_id = self.client.created_offices.get(row['office'])
                if not office_id:
                    logger.warning(f"Office not found: {row['office']}")
                    continue

                data = {
                    'officeId': office_id,
                    'firstname': row['firstname'],
                    'lastname': row['lastname'],
                    'isLoanOfficer': True,
                    'mobileNo': row['mobile'],
                    'joiningDate': row['joining_date'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en',
                    'externalId': row['external_id']
                }

                response = self.client.post('/staff', data)
                staff_id = response.get('resourceId') or response.get('staffId')

                self.client.created_staff[row['username']] = staff_id

                logger.info(f"✓ Created staff: {row['firstname']} {row['lastname']} (ID: {staff_id})")

                # Create user account for staff
                self._create_user_account(row, staff_id, office_id)

                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create staff {row['firstname']} {row['lastname']}: {str(e)}")

    def _create_user_account(self, staff_row, staff_id: int, office_id: int):
        """Create user account for staff member"""
        try:
            # Map staff role to Fineract role name
            # Note: These roles should exist in Fineract or be created via UI first
            # See "Roles Permissions" sheet for recommended permission configuration
            role_name_mapping = {
                'Branch Manager': 'Super user',  # Use Fineract's default admin-like role
                'Loan Officer': 'Super user',     # Or create custom "Loan Officer" role in Fineract UI
                'Cashier': 'Super user'           # Or create custom "Cashier" role in Fineract UI
            }

            fineract_role_name = role_name_mapping.get(staff_row['role'], 'Super user')

            # Get available roles from Fineract
            if not hasattr(self.client, 'available_roles'):
                roles_response = self.client.get('/roles')
                self.client.available_roles = {role['name']: role['id'] for role in roles_response}
                logger.info(f"Found {len(self.client.available_roles)} roles in Fineract: {list(self.client.available_roles.keys())}")

            # Get role ID for the mapped role name
            role_id = self.client.available_roles.get(fineract_role_name)

            if not role_id:
                logger.warning(f"Role '{fineract_role_name}' not found. Using first available role.")
                role_id = list(self.client.available_roles.values())[0] if self.client.available_roles else 1

            # Generate secure password that meets Fineract requirements:
            # 12-50 chars, uppercase, lowercase, number, special char, no consecutive repeating
            secure_password = self.config.get('default_password', 'Fineract@Pas1234')

            data = {
                'username': staff_row['username'],
                'firstname': staff_row['firstname'],
                'lastname': staff_row['lastname'],
                'email': staff_row['email'],
                'officeId': office_id,
                'staffId': staff_id,
                'roles': [role_id],
                'sendPasswordToEmail': False,
                'password': secure_password,
                'repeatPassword': secure_password
            }

            self.client.post('/users', data)
            logger.info(f"  ✓ Created user account: {staff_row['username']}")

        except Exception as e:
            logger.warning(f"  ⚠ Failed to create user account for {staff_row['username']}: {str(e)}")

    def load_gl_accounts(self):
        """Create chart of accounts"""
        logger.info("=" * 80)
        logger.info("LOADING CHART OF ACCOUNTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Chart of Accounts')

        # Map account types to Fineract constants
        account_type_mapping = {
            'Asset': 1,
            'Liability': 2,
            'Equity': 3,
            'Income': 4,
            'Expense': 5
        }

        usage_mapping = {
            'Detail': 1,
            'Header': 2
        }

        for idx, row in df.iterrows():
            try:
                data = {
                    'name': row['gl_name'],
                    'glCode': str(row['gl_code']),
                    'type': account_type_mapping.get(row['account_type']),
                    'usage': usage_mapping.get(row['usage'], 1),
                    'manualEntriesAllowed': row['manual_entries'] == 'Yes',
                    'description': row.get('description', '')
                }

                response = self.client.post('/glaccounts', data)
                account_id = response.get('resourceId') or response.get('glAccountId')

                # Store GL account ID using string key for consistent lookup
                self.client.created_gl_accounts[str(row['gl_code'])] = account_id

                logger.info(f"✓ Created GL Account: {row['gl_code']} - {row['gl_name']} (ID: {account_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create GL account {row['gl_code']}: {str(e)}")

    def load_charges(self):
        """Create charges/fees"""
        logger.info("=" * 80)
        logger.info("LOADING CHARGES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Charges')

        # Map charge types
        charge_applies_to = {
            'Loan': 1,
            'Savings': 2,
            'Client': 3
        }

        # Fineract charge time type IDs - must match Fineract's chargeTimeTypeOptions
        # Based on: https://localhost/fineract-provider/api/v1/charges/template
        charge_time_type = {
            # Loan charge times
            'Disbursement': 1,                    # ID 1: chargeTimeType.disbursement
            'Specified Due Date': 2,              # ID 2: chargeTimeType.specifiedDueDate
            'Installment Fee': 8,                 # ID 8: chargeTimeType.instalmentFee
            'Overdue Installment': 9,             # ID 9: chargeTimeType.overdueInstallment
            'Tranche Disbursement': 12,           # ID 12: chargeTimeType.tranchedisbursement

            # Savings charge times
            'Savings Activation': 3,              # ID 3: chargeTimeType.savingsActivation
            'Withdrawal': 5,                      # ID 5: chargeTimeType.withdrawalFee
            'Withdrawal Fee': 5,                  # ID 5: chargeTimeType.withdrawalFee
            'ATM Fee': 5,                         # ID 5: chargeTimeType.withdrawalFee (ATM withdrawals)
            'Annual': 6,                          # ID 6: chargeTimeType.annualFee
            'Annual Fee': 6,                      # ID 6: chargeTimeType.annualFee
            'Monthly': 7,                         # ID 7: chargeTimeType.monthlyFee
            'Monthly Fee': 7,                     # ID 7: chargeTimeType.monthlyFee
            'Overdraft Fee': 10,                  # ID 10: chargeTimeType.overdraftFee
            'Weekly Fee': 11,                     # ID 11: chargeTimeType.weeklyFee
            'Saving No Activity Fee': 16,         # ID 16: chargeTimeType.savingsNoActivityFee

            # Share account charge times
            'Share Account Activate': 13,         # ID 13: chargeTimeType.activation
            'Share Purchase': 14,                 # ID 14: chargeTimeType.sharespurchase
            'Share Redeem': 15,                   # ID 15: chargeTimeType.sharesredeem

            # Legacy aliases for backward compatibility
            'Activation': 3,                      # Map to Savings Activation
            'On Demand': 2                        # Map to Specified Due Date
        }

        # Valid charge time types for savings accounts
        # Based on Fineract API: /charges/template - chargeTimeTypeOptions for Savings
        savings_valid_charge_times = {2, 3, 5, 6, 7, 10, 11, 16}

        charge_calculation_type = {
            'Flat': 1,
            'Percentage of Amount': 2,
            'Percentage of Interest': 3
        }

        for idx, row in df.iterrows():
            try:
                charge_time_val = charge_time_type.get(row['charge_time'])

                # Skip charges with invalid configurations
                if not charge_time_val:
                    logger.warning(f"⚠ Skipping charge {row['charge_name']}: unsupported charge_time '{row['charge_time']}'")
                    continue

                # Validate charge time type for savings charges
                if row['charge_type'] == 'Savings' and charge_time_val not in savings_valid_charge_times:
                    logger.warning(f"⚠ Skipping charge {row['charge_name']}: chargeTimeType {charge_time_val} not valid for Savings (allowed: {sorted(savings_valid_charge_times)})")
                    continue

                data = {
                    'name': row['charge_name'],
                    'currencyCode': row['currency'],
                    'chargeAppliesTo': charge_applies_to.get(row['charge_type']),
                    'chargeTimeType': charge_time_val,
                    'chargeCalculationType': charge_calculation_type.get(row['calculation_type']),
                    'chargePaymentMode': 0,  # 0=Regular, 1=Account Transfer
                    'amount': row['amount'],
                    'active': row['active'] == 'Yes',
                    'locale': 'en'
                }

                # Handle special charge configurations
                # For "Late Payment Penalty" - mark as penalty
                if 'Penalty' in row['charge_name'] and row['charge_time'] == 'Overdue Installment':
                    data['penalty'] = True

                # For Monthly/Annual/Weekly charges, add feeOnMonthDay and feeInterval
                # Note: Monthly charges need [day], Annual charges need [day, month], Weekly charges need [day]
                if row['charge_time'] in ['Monthly', 'Monthly Fee']:
                    data['feeOnMonthDay'] = [1]  # 1st day of month
                    data['feeInterval'] = 1  # Every month

                if row['charge_time'] in ['Annual', 'Annual Fee']:
                    data['feeOnMonthDay'] = [1, 1]  # January 1st (day 1, month 1)
                    data['feeInterval'] = 1  # Every year

                if row['charge_time'] == 'Weekly Fee':
                    data['feeOnMonthDay'] = [1]  # Monday (1=Monday in Fineract)
                    data['feeInterval'] = 1  # Every week

                response = self.client.post('/charges', data)
                charge_id = response.get('resourceId') or response.get('chargeId')

                self.client.created_charges[row['charge_name']] = charge_id

                logger.info(f"✓ Created charge: {row['charge_name']} (ID: {charge_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create charge {row['charge_name']}: {str(e)}")

    def load_fund_sources(self):
        """Create fund sources"""
        logger.info("=" * 80)
        logger.info("LOADING FUND SOURCES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Fund Sources')

        for idx, row in df.iterrows():
            try:
                data = {
                    'name': row['fund_name'],
                    'externalId': row['external_id']
                }

                response = self.client.post('/funds', data)
                fund_id = response.get('resourceId') or response.get('fundId')

                self.client.created_funds[row['fund_name']] = fund_id

                logger.info(f"✓ Created fund: {row['fund_name']} (ID: {fund_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create fund {row['fund_name']}: {str(e)}")

    def load_payment_types(self):
        """Create payment types"""
        logger.info("=" * 80)
        logger.info("LOADING PAYMENT TYPES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Payment Types')

        for idx, row in df.iterrows():
            try:
                data = {
                    'name': row['payment_type'],
                    'description': row['description'],
                    'isCashPayment': row['is_cash_payment'] == 'Yes',
                    'position': row['order_position']
                }

                response = self.client.post('/paymenttypes', data)
                payment_type_id = response.get('resourceId')

                self.client.created_payment_types[row['payment_type']] = payment_type_id

                logger.info(f"✓ Created payment type: {row['payment_type']} (ID: {payment_type_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create payment type {row['payment_type']}: {str(e)}")

    def load_holidays(self):
        """Create holidays"""
        logger.info("=" * 80)
        logger.info("LOADING HOLIDAYS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Holidays')

        # Get all office IDs
        offices = [{'officeId': office_id} for office_id in self.client.created_offices.values()]

        if not offices:
            logger.warning("No offices found, skipping holidays")
            return

        # Process each holiday
        for _, row in df.iterrows():
            try:
                # Convert date from yyyy-MM-dd to dd MMMM yyyy format
                date_str = row['date']  # Format: 2024-01-01
                date_obj = datetime.strptime(date_str, '%Y-%m-%d')
                formatted_date = date_obj.strftime('%-d %B %Y')  # Format: 1 January 2024

                data = {
                    'name': row['holiday_name'],
                    'fromDate': formatted_date,
                    'toDate': formatted_date,
                    'reschedulingType': 1,  # 1=Reschedule to next repayment date, 2=Reschedule to next meeting date
                    'description': row.get('description', ''),
                    'offices': offices,
                    'dateFormat': 'dd MMMM yyyy',
                    'locale': 'en'
                }

                try:
                    response = self.client.post('/holidays', data)
                    holiday_id = response.get('resourceId') or response.get('holidayId')
                    logger.info(f"✓ Created holiday: {row['holiday_name']} ({date_str}) for {len(offices)} offices - ID: {holiday_id}")
                    time.sleep(0.3)
                except Exception as e:
                    if 'already exists' in str(e).lower() or 'duplicate' in str(e).lower():
                        logger.info(f"  ⊙ Holiday already exists: {row['holiday_name']} ({date_str})")
                    else:
                        raise

            except Exception as e:
                logger.error(f"✗ Failed to create holiday {row['holiday_name']}: {str(e)}")

    def load_loan_products(self):
        """Create loan products with accounting mappings"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN PRODUCTS")
        logger.info("=" * 80)

        df_products = self._read_excel_sheet('Loan Products')
        df_accounting = self._read_excel_sheet('Loan Product Accounting')

        for idx, row in df_products.iterrows():
            try:
                # Get accounting mappings for this product
                product_mappings = df_accounting[df_accounting['product_short_name'] == row['short_name']]

                # Helper function to get GL account ID
                def get_gl_id(mapping_type):
                    mapping = product_mappings[product_mappings['mapping_type'] == mapping_type]
                    if len(mapping) > 0:
                        gl_code = str(mapping.iloc[0]['gl_code'])
                        return self.client.created_gl_accounts.get(gl_code)
                    return None

                # Shorten product name if needed (max 4 characters)
                short_name = row['short_name']
                if len(short_name) > 4:
                    short_name_mapping = {
                        'MICRO-SOL': 'MSOL',
                        'SME-BIZ': 'SBIZ',
                        'AGRI-SEASON': 'ASEA'
                    }
                    short_name = short_name_mapping.get(short_name, short_name[:4])
                    logger.info(f"  Shortened product name: {row['short_name']} → {short_name}")

                # Build product data with Cash-based accounting
                data = {
                    'name': row['product_name'],
                    'shortName': short_name,
                    'description': row['description'],
                    'currencyCode': row['currency'],
                    'digitsAfterDecimal': 0,
                    'inMultiplesOf': 0,
                    'principal': row['principal_default'],
                    'minPrincipal': row['principal_min'],
                    'maxPrincipal': row['principal_max'],
                    'numberOfRepayments': row['number_of_repayments_default'],
                    'minNumberOfRepayments': row['number_of_repayments_min'],
                    'maxNumberOfRepayments': row['number_of_repayments_max'],
                    'repaymentEvery': 1,
                    'repaymentFrequencyType': 2,  # Monthly
                    'interestRatePerPeriod': row['interest_rate_default'],
                    'minInterestRatePerPeriod': row['interest_rate_min'],
                    'maxInterestRatePerPeriod': row['interest_rate_max'],
                    'interestRateFrequencyType': 2,  # Monthly
                    'amortizationType': 1,  # Equal installments
                    'interestType': 0,  # Declining balance
                    'interestCalculationPeriodType': 1,  # Same as repayment period
                    'transactionProcessingStrategyCode': 'mifos-standard-strategy',  # Use code instead of ID
                    'accountingRule': 2,  # Cash-based accounting
                    'locale': 'en',
                    'dateFormat': 'yyyy-MM-dd',
                    'graceOnPrincipalPayment': row['grace_on_principal_periods'],
                    'graceOnInterestPayment': row['grace_on_interest_periods'],
                    # Required fields that were missing
                    'daysInYearType': 365,  # 1=Actual, 360=360 days, 364=364 days, 365=365 days
                    'daysInMonthType': 30,  # 1=Actual, 30=30 days
                    'isInterestRecalculationEnabled': False
                }

                # Add GL account mappings for cash-based accounting
                fund_source_id = get_gl_id('Fund Source')
                loan_portfolio_id = get_gl_id('Loan Portfolio')
                interest_income_id = get_gl_id('Interest Income')
                fee_income_id = get_gl_id('Fee Income')
                penalty_income_id = get_gl_id('Penalty Income')
                transfer_suspense_id = get_gl_id('Transfer in Suspense')
                income_from_recovery_id = get_gl_id('Income from Recovery')

                # Get write-off account (GL 93: Loan Write-off Expense)
                write_off_id = self.client.created_gl_accounts.get('93')

                # Get overpayment liability account (use Savings Control as overpayment liability)
                overpayment_liability_id = self.client.created_gl_accounts.get('61')  # Voluntary Savings Accounts

                if all([fund_source_id, loan_portfolio_id, interest_income_id, fee_income_id,
                        penalty_income_id, transfer_suspense_id, write_off_id, overpayment_liability_id]):
                    data['fundSourceAccountId'] = fund_source_id
                    data['loanPortfolioAccountId'] = loan_portfolio_id
                    data['interestOnLoanAccountId'] = interest_income_id
                    data['incomeFromFeeAccountId'] = fee_income_id
                    data['incomeFromPenaltyAccountId'] = penalty_income_id
                    data['transfersInSuspenseAccountId'] = transfer_suspense_id
                    data['incomeFromRecoveryAccountId'] = income_from_recovery_id or interest_income_id
                    data['writeOffAccountId'] = write_off_id
                    data['overpaymentLiabilityAccountId'] = overpayment_liability_id
                else:
                    logger.warning(f"  ⚠ Some GL accounts not found for {row['short_name']}, using accountingRule=1 (None)")
                    data['accountingRule'] = 1

                response = self.client.post('/loanproducts', data)
                product_id = response.get('resourceId') or response.get('loanProductId')

                self.client.created_loan_products[row['short_name']] = product_id

                logger.info(f"✓ Created loan product: {row['product_name']} (ID: {product_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create loan product {row['product_name']}: {str(e)}")

    def load_savings_products(self):
        """Create savings products with accounting mappings"""
        logger.info("=" * 80)
        logger.info("LOADING SAVINGS PRODUCTS")
        logger.info("=" * 80)

        df_products = self._read_excel_sheet('Savings Products')
        df_accounting = self._read_excel_sheet('Savings Product Accounting')

        for idx, row in df_products.iterrows():
            try:
                # Get accounting mappings for this product
                product_mappings = df_accounting[df_accounting['product_short_name'] == row['short_name']]

                # Helper function to get GL account ID
                def get_gl_id(mapping_type):
                    mapping = product_mappings[product_mappings['mapping_type'] == mapping_type]
                    if len(mapping) > 0:
                        gl_code = str(mapping.iloc[0]['gl_code'])
                        return self.client.created_gl_accounts.get(gl_code)
                    return None

                # Shorten the shortName to max 4 characters as required by Fineract
                short_name = row['short_name']
                if len(short_name) > 4:
                    # Create abbreviated version
                    short_name_mapping = {
                        'VOL-SAV': 'VSAV',
                        'FIXED-DEP': 'FDEP',
                        'MAND-GRP': 'MGRP'
                    }
                    short_name = short_name_mapping.get(short_name, short_name[:4])
                    logger.info(f"  Shortened product name: {row['short_name']} → {short_name}")

                data = {
                    'name': row['product_name'],
                    'shortName': short_name,
                    'description': row['description'],
                    'currencyCode': row['currency'],
                    'digitsAfterDecimal': 0,
                    'inMultiplesOf': 0,
                    'nominalAnnualInterestRate': row['nominal_annual_interest_rate'],
                    'interestCompoundingPeriodType': 4,  # Monthly
                    'interestPostingPeriodType': 4,  # Monthly
                    'interestCalculationType': 1,  # Daily balance
                    'interestCalculationDaysInYearType': 365,
                    'minRequiredOpeningBalance': row['minimum_opening_balance'],
                    'accountingRule': 2,  # Cash-based accounting
                    'locale': 'en',
                    'withdrawalFeeForTransfers': row['withdrawal_fee_for_transfers'] == 'Yes',
                    'allowOverdraft': row['overdraft_allowed'] == 'Yes',
                    'withHoldTax': False  # Disable withholding tax (taxGroupId is mandatory if enabled, but tax groups need to be created first)
                }

                # Add GL account mappings for cash-based accounting
                savings_reference_id = get_gl_id('Savings Reference')
                savings_control_id = get_gl_id('Savings Control')
                interest_expense_id = get_gl_id('Interest on Savings')
                income_from_fees_id = get_gl_id('Income from Fees')
                transfer_suspense_id = get_gl_id('Transfer in Suspense')

                # Get overdraft portfolio control account (GL 62: Fixed Deposits or use Savings Control)
                overdraft_portfolio_id = self.client.created_gl_accounts.get('62') or savings_control_id

                # Get income from interest on overdrafts (GL 44: Interest Income)
                income_from_interest_id = self.client.created_gl_accounts.get('44')

                # Get write-off account (GL 93: Loan Write-off Expense)
                write_off_id = self.client.created_gl_accounts.get('93')

                if all([savings_reference_id, savings_control_id, interest_expense_id,
                        income_from_fees_id, transfer_suspense_id, overdraft_portfolio_id,
                        income_from_interest_id, write_off_id]):
                    data['savingsReferenceAccountId'] = savings_reference_id
                    data['savingsControlAccountId'] = savings_control_id
                    data['interestOnSavingsAccountId'] = interest_expense_id
                    data['incomeFromFeeAccountId'] = income_from_fees_id
                    data['incomeFromPenaltyAccountId'] = income_from_fees_id  # Use same as fees
                    data['transfersInSuspenseAccountId'] = transfer_suspense_id
                    data['overdraftPortfolioControlId'] = overdraft_portfolio_id
                    data['incomeFromInterestId'] = income_from_interest_id
                    data['writeOffAccountId'] = write_off_id
                else:
                    logger.warning(f"  ⚠ Some GL accounts not found for {row['short_name']}, using accountingRule=1 (None)")
                    data['accountingRule'] = 1

                response = self.client.post('/savingsproducts', data)
                product_id = response.get('resourceId') or response.get('savingsProductId')

                # Store with both original and shortened names for lookup
                self.client.created_savings_products[row['short_name']] = product_id
                self.client.created_savings_products[short_name] = product_id

                logger.info(f"✓ Created savings product: {row['product_name']} (ID: {product_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create savings product {row['product_name']}: {str(e)}")

    def load_financial_activity_mappings(self):
        """Configure Financial Activity to GL Account mappings - CRITICAL for inter-branch"""
        logger.info("=" * 80)
        logger.info("LOADING FINANCIAL ACTIVITY MAPPINGS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Financial Activity Mapping')

        # Financial Activity IDs in Fineract
        activity_mapping = {
            'Asset Transfer': 100,  # Inter-office asset transfers
            'Liability Transfer': 200,  # Inter-office liability transfers
            'Cash at Mainvault': 101,  # Cash at main vault
            'Cash at Teller': 102,  # Cash at teller
            'Opening Balances Contra': 300,  # Opening balances
            'Fund Source': 103,  # Default fund source
        }

        for idx, row in df.iterrows():
            try:
                activity_id = activity_mapping.get(row['financial_activity'])
                gl_code = str(row['gl_code'])
                gl_account_id = self.client.created_gl_accounts.get(gl_code)

                if not activity_id:
                    logger.warning(f"Unknown financial activity: {row['financial_activity']}")
                    continue

                if not gl_account_id:
                    logger.warning(f"GL account not found: {gl_code} for {row['financial_activity']}")
                    continue

                data = {
                    'financialActivityId': activity_id,
                    'glAccountId': gl_account_id
                }

                response = self.client.post('/financialactivityaccounts', data)

                logger.info(f"✓ Mapped {row['financial_activity']} → GL {gl_code} ({row['gl_name']})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to map financial activity {row['financial_activity']}: {str(e)}")

    def enable_maker_checker(self):
        """Enable Maker-Checker (4-eyes principle) for critical operations"""
        logger.info("=" * 80)
        logger.info("ENABLING MAKER-CHECKER (DUAL AUTHORIZATION)")
        logger.info("=" * 80)

        logger.info("⚠ NOTE: Maker-Checker configuration via API is not available in this Fineract version.")
        logger.info("   Please configure Maker-Checker manually via the Fineract Admin UI:")
        logger.info("")
        logger.info("   Steps:")
        logger.info("   1. Login to Fineract Admin UI as Super User")
        logger.info("   2. Go to Admin → System → Manage Maker Checkers")
        logger.info("   3. Enable maker-checker for the following operations:")
        logger.info("")

        df = self._read_excel_sheet('Maker Checker Config')

        # Display recommended maker-checker configurations
        for idx, row in df.iterrows():
            logger.info(f"   • {row['task_name']}")
            logger.info(f"     Entity: {row['entity']}, Action: {row['action']}")
            logger.info(f"     Recommended Threshold: {row['threshold_amount']:,.0f} {row['threshold_currency']}")
            logger.info(f"     Maker: {row['maker_role']}, Checker: {row['checker_role']}")
            logger.info("")

        logger.info("NOTE: Maker-Checker must be configured via Fineract Admin UI.")
        logger.info("      The system will require dual authorization once enabled.")

    def load_clients(self):
        """Create clients"""
        logger.info("=" * 80)
        logger.info("LOADING CLIENTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Clients')

        for idx, row in df.iterrows():
            try:
                office_id = self.client.created_offices.get(row['office'])
                staff_id = self.client.created_staff.get(row['staff'])

                if not office_id:
                    logger.warning(f"Office not found: {row['office']}")
                    continue

                data = {
                    'officeId': office_id,
                    'firstname': row['firstname'],
                    'lastname': row['lastname'],
                    'externalId': row['external_id'],
                    'active': True,
                    'activationDate': row['activation_date'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en',
                    'mobileNo': row['mobile'],
                    'legalFormId': 1  # 1=PERSON (individual), 2=ENTITY (organization/company)
                }

                if staff_id:
                    data['staffId'] = staff_id

                if row['date_of_birth']:
                    data['dateOfBirth'] = row['date_of_birth']

                # Skip gender if Gender code values don't exist in Fineract
                # Gender code values must be created first via "Codes and Values" sheet
                # if row['gender']:
                #     data['genderId'] = 1 if row['gender'] == 'Male' else 2

                # Submit client
                response = self.client.post('/clients', data)
                client_id = response.get('resourceId') or response.get('clientId')

                self.client.created_clients[row['external_id']] = client_id

                logger.info(f"✓ Created client: {row['firstname']} {row['lastname']} (ID: {client_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create client {row['firstname']} {row['lastname']}: {str(e)}")

    def load_savings_accounts(self):
        """Create savings accounts"""
        logger.info("=" * 80)
        logger.info("LOADING SAVINGS ACCOUNTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Savings Accounts')

        for idx, row in df.iterrows():
            try:
                client_id = self.client.created_clients.get(row['client_external_id'])
                product_id = self.client.created_savings_products.get(row['product'])
                staff_id = self.client.created_staff.get(row['field_officer'])

                if not client_id or not product_id:
                    logger.warning(f"Client or product not found for {row['external_id']}")
                    continue

                # Submit savings account
                data = {
                    'clientId': client_id,
                    'productId': product_id,
                    'submittedOnDate': row['submitted_on'],
                    'externalId': row['external_id'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                if staff_id:
                    data['fieldOfficerId'] = staff_id

                response = self.client.post('/savingsaccounts', data)
                savings_id = response.get('resourceId') or response.get('savingsId')

                # Approve
                approve_data = {
                    'approvedOnDate': row['approved_on'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }
                self.client.post(f'/savingsaccounts/{savings_id}?command=approve',
                               approve_data)

                # Activate
                activate_data = {
                    'activatedOnDate': row['activated_on'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }
                self.client.post(f'/savingsaccounts/{savings_id}?command=activate',
                               activate_data)

                # Initial deposit
                if row['initial_deposit'] and row['initial_deposit'] > 0:
                    deposit_data = {
                        'transactionDate': row['activated_on'],
                        'transactionAmount': row['initial_deposit'],
                        'paymentTypeId': self.client.created_payment_types.get('Cash', 1),
                        'dateFormat': 'yyyy-MM-dd',
                        'locale': 'en'
                    }
                    self.client.post(f'/savingsaccounts/{savings_id}/transactions?command=deposit',
                                   deposit_data)

                self.client.created_savings_accounts[row['external_id']] = savings_id

                logger.info(f"✓ Created savings account: {row['external_id']} (ID: {savings_id})")
                time.sleep(0.7)

            except Exception as e:
                logger.error(f"✗ Failed to create savings account {row['external_id']}: {str(e)}")

    def load_loan_accounts(self):
        """Create loan accounts"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN ACCOUNTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Loan Accounts')

        for idx, row in df.iterrows():
            try:
                client_id = self.client.created_clients.get(row['client_external_id'])
                product_id = self.client.created_loan_products.get(row['product'])
                loan_officer_id = self.client.created_staff.get(row['loan_officer'])
                fund_id = self.client.created_funds.get(row['fund_source'])

                if not client_id or not product_id:
                    logger.warning(f"Client or product not found for {row['external_id']}")
                    continue

                # Submit loan application
                data = {
                    'clientId': client_id,
                    'productId': product_id,
                    'principal': row['principal'],
                    'loanTermFrequency': row['loan_term'],
                    'loanTermFrequencyType': 2,  # Monthly
                    'numberOfRepayments': row['loan_term'],
                    'repaymentEvery': 1,
                    'repaymentFrequencyType': 2,  # Monthly
                    'interestRatePerPeriod': row['interest_rate'],
                    'amortizationType': 1,
                    'interestType': 0,
                    'interestCalculationPeriodType': 1,
                    'transactionProcessingStrategyCode': 'mifos-standard-strategy',  # Use code instead of ID
                    'expectedDisbursementDate': row['disbursed_on'],
                    'submittedOnDate': row['submitted_on'],
                    'externalId': row['external_id'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en',
                    'loanType': 'individual'
                }

                if loan_officer_id:
                    data['loanOfficerId'] = loan_officer_id

                if fund_id:
                    data['fundId'] = fund_id

                response = self.client.post('/loans', data)
                loan_id = response.get('resourceId') or response.get('loanId')

                # Approve
                approve_data = {
                    'approvedOnDate': row['approved_on'],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }
                self.client.post(f'/loans/{loan_id}?command=approve', approve_data)

                # Disburse
                disburse_data = {
                    'actualDisbursementDate': row['disbursed_on'],
                    'transactionAmount': row['principal'],
                    'paymentTypeId': self.client.created_payment_types.get('Cash', 1),
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }
                self.client.post(f'/loans/{loan_id}?command=disburse', disburse_data)

                self.client.created_loan_accounts[row['external_id']] = loan_id

                logger.info(f"✓ Created loan account: {row['external_id']} (ID: {loan_id})")
                time.sleep(0.7)

            except Exception as e:
                logger.error(f"✗ Failed to create loan account {row['external_id']}: {str(e)}")

    def load_currency_config(self):
        """Configure currency"""
        logger.info("=" * 80)
        logger.info("LOADING CURRENCY CONFIGURATION")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Currency Config')

        # Collect all currency codes from the sheet
        currency_codes = []
        for idx, row in df.iterrows():
            currency_codes.append(row['currency_code'])
            logger.info(f"  Found currency: {row['currency_code']} - {row['currency_name']}")

        # Enable currencies via PUT request
        try:
            data = {
                'currencies': currency_codes
            }
            self.client.put('/currencies', data)
            logger.info(f"✓ Enabled currencies: {', '.join(currency_codes)}")

            # Log details of each currency
            for idx, row in df.iterrows():
                logger.info(f"  {row['currency_code']}: {row['currency_name']}")
                logger.info(f"    Display symbol: {row['display_symbol']}, Decimal places: {row['decimal_places']}")

        except Exception as e:
            logger.error(f"✗ Failed to configure currencies: {str(e)}")

    def load_working_days(self):
        """Configure working days"""
        logger.info("=" * 80)
        logger.info("LOADING WORKING DAYS CONFIGURATION")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Working Days')

        try:
            # Build working days configuration
            working_days = []
            day_names = []
            for idx, row in df.iterrows():
                if row['working_day'] == 'Yes':
                    # Map to iCalendar day abbreviations (MO, TU, WE, TH, FR, SA, SU)
                    day_map = {
                        'Monday': 'MO',
                        'Tuesday': 'TU',
                        'Wednesday': 'WE',
                        'Thursday': 'TH',
                        'Friday': 'FR',
                        'Saturday': 'SA',
                        'Sunday': 'SU'
                    }
                    day_abbr = day_map.get(row['day_of_week'])
                    if day_abbr:
                        working_days.append(day_abbr)
                        day_names.append(row['day_of_week'][:3])

            # Build iCalendar RRULE format recurrence string
            # Format: FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR,SA,
            byday_string = ','.join(working_days) + ','  # Note: Fineract expects trailing comma
            recurrence_string = f"FREQ=WEEKLY;INTERVAL=1;BYDAY={byday_string}"

            # Update working days via API
            data = {
                'recurrence': recurrence_string,
                'repaymentRescheduleType': 2,
                'extendTermForDailyRepayments': False,
                'locale': 'en'
            }

            # Fineract working days endpoint
            self.client.put('/workingdays', data)

            logger.info(f"✓ Working days configured: {', '.join(day_names)}")
            logger.info(f"  Recurrence rule: {recurrence_string}")

        except Exception as e:
            logger.warning(f"⚠ Working days configuration may require manual setup: {str(e)}")

    def load_account_number_preferences(self):
        """Configure account number generation preferences"""
        logger.info("=" * 80)
        logger.info("LOADING ACCOUNT NUMBER PREFERENCES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Account Number Preferences')

        # Account number preferences in Fineract require specific configuration
        # This is typically done via /v1/accountnumberformats endpoint
        entity_type_mapping = {
            'Client': 1,
            'Loan': 2,
            'Savings': 3,
            'Groups': 4,
            'Centers': 5
        }

        # Fineract prefix type IDs:
        # 1 = OFFICE_NAME
        # 101 = CLIENT_TYPE
        # 201 = LOAN_PRODUCT_SHORT_NAME
        # 301 = SAVINGS_PRODUCT_SHORT_NAME
        prefix_type_mapping = {
            'Office Name': 1,
            'Office Short Name': 1,  # Map to OFFICE_NAME
            'Client Type': 101,
            'Loan Product Short Name': 201,
            'Savings Product Short Name': 301,
            'Product Short Name': 201  # Default to loan product
        }

        for idx, row in df.iterrows():
            try:
                entity_type_id = entity_type_mapping.get(row['entity_type'])
                prefix_type_id = prefix_type_mapping.get(row['prefix_type'], 0)

                if not entity_type_id:
                    continue

                # Auto-correct prefix type based on entity type if needed
                if row['entity_type'] == 'Savings' and 'Product' in row['prefix_type']:
                    prefix_type_id = 301  # Force SAVINGS_PRODUCT_SHORT_NAME for savings
                elif row['entity_type'] == 'Loan' and 'Product' in row['prefix_type']:
                    prefix_type_id = 201  # Force LOAN_PRODUCT_SHORT_NAME for loans

                data = {
                    'accountType': entity_type_id,
                    'prefixType': prefix_type_id
                }

                try:
                    # Create or update account number format
                    self.client.post('/accountnumberformats', data)
                    logger.info(f"✓ Configured account numbers for {row['entity_type']}: {row['example']}")
                    logger.info(f"  Prefix type: {row['prefix_type']} (ID: {prefix_type_id})")
                except Exception as e:
                    if 'already exists' in str(e).lower() or '403' in str(e):
                        logger.info(f"  ⊙ Account number format already exists for {row['entity_type']}")
                    else:
                        raise

                time.sleep(0.2)

            except Exception as e:
                logger.error(f"✗ Failed to configure account numbers for {row['entity_type']}: {str(e)}")

    def load_codes_and_values(self):
        """Create codes and code values (dropdown configurations)"""
        logger.info("=" * 80)
        logger.info("LOADING CODES AND VALUES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Codes and Values')

        # Get existing codes once at the beginning
        try:
            existing_codes_list = self.client.get('/codes')
            existing_codes = {c['name']: c['id'] for c in existing_codes_list}
        except Exception as e:
            logger.warning(f"Could not retrieve existing codes: {str(e)}")
            existing_codes = {}

        # Group by code_name
        codes = df['code_name'].unique()

        for code_name in codes:
            try:
                # Check if code already exists
                if code_name in existing_codes:
                    code_id = existing_codes[code_name]
                    logger.info(f"⊙ Code already exists: {code_name} (ID: {code_id})")
                else:
                    # Create code
                    code_data = {
                        'name': code_name
                    }
                    try:
                        code_response = self.client.post('/codes', code_data)
                        code_id = code_response.get('resourceId')
                        existing_codes[code_name] = code_id
                        logger.info(f"✓ Created code: {code_name} (ID: {code_id})")
                    except Exception as e:
                        # Code might have been created since we fetched the list
                        if 'already exists' in str(e).lower() or '403' in str(e):
                            logger.info(f"  ⚠ Code already exists: {code_name}")
                            # Refresh the codes list
                            codes_list = self.client.get('/codes')
                            code_id = next((c['id'] for c in codes_list if c['name'] == code_name), None)
                            if code_id:
                                existing_codes[code_name] = code_id
                        else:
                            raise

                if not code_id:
                    logger.warning(f"  Could not get ID for code: {code_name}")
                    continue

                # Create code values
                code_values = df[df['code_name'] == code_name]
                for _, value_row in code_values.iterrows():
                    try:
                        value_data = {
                            'name': value_row['code_value'],
                            'position': int(value_row['code_position']),
                            'isActive': value_row['is_active'] == 'Yes',
                            'description': value_row.get('description', '')
                        }

                        self.client.post(f'/codes/{code_id}/codevalues', value_data)
                        logger.info(f"  ✓ Added value: {value_row['code_value']}")

                        time.sleep(0.1)

                    except Exception as e:
                        if 'already exists' in str(e).lower():
                            logger.debug(f"    Value already exists: {value_row['code_value']}")
                        else:
                            logger.warning(f"    ⚠ Failed to add value {value_row['code_value']}: {str(e)}")

            except Exception as e:
                logger.error(f"✗ Failed to create code {code_name}: {str(e)}")

    def load_scheduler_jobs(self):
        """Configure scheduler jobs"""
        logger.info("=" * 80)
        logger.info("LOADING SCHEDULER JOBS CONFIGURATION")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Scheduler Jobs')

        # Get existing jobs
        try:
            existing_jobs = self.client.get('/jobs')

            for idx, row in df.iterrows():
                try:
                    # Find job by name
                    job = next((j for j in existing_jobs if j.get('displayName') == row['display_name']), None)

                    if job:
                        job_id = job['jobId']

                        # Update job schedule if cron expression is provided
                        if row['cron_expression']:
                            update_data = {
                                'cronExpression': row['cron_expression'],
                                'active': row['active'] == 'Yes'
                            }

                            self.client.put(f'/jobs/{job_id}', update_data)
                            logger.info(f"✓ Configured job: {row['display_name']}")
                            logger.info(f"  Schedule: {row['cron_expression']}, Active: {row['active']}")
                        else:
                            logger.info(f"✓ Job found: {row['display_name']} (ID: {job_id})")

                    else:
                        logger.warning(f"  ⚠ Job not found in Fineract: {row['display_name']}")

                    time.sleep(0.2)

                except Exception as e:
                    logger.error(f"✗ Failed to configure job {row['display_name']}: {str(e)}")

        except Exception as e:
            logger.error(f"✗ Failed to load scheduler jobs: {str(e)}")

    def load_global_configuration(self):
        """Configure global system settings"""
        logger.info("=" * 80)
        logger.info("LOADING GLOBAL CONFIGURATION")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Global Configuration')

        try:
            # Get existing configurations
            existing_configs = self.client.get('/configurations')

            for idx, row in df.iterrows():
                try:
                    # Find configuration by name
                    config = next((c for c in existing_configs.get('globalConfiguration', [])
                                 if c.get('name') == row['config_name']), None)

                    if config:
                        config_id = config['id']

                        # Update configuration
                        update_data = {
                            'enabled': row['enabled'] == 'Yes'
                        }

                        # Add value if it's a value-based config
                        if row['value'] and row['value'] not in ['true', 'false']:
                            update_data['value'] = row['value']

                        self.client.put(f'/configurations/{config_id}', update_data)
                        logger.info(f"✓ Configured: {row['config_name']} = {row['value']} (enabled: {row['enabled']})")

                    else:
                        logger.debug(f"  Configuration not found: {row['config_name']}")

                    time.sleep(0.1)

                except Exception as e:
                    logger.warning(f"  ⚠ Failed to configure {row['config_name']}: {str(e)}")

        except Exception as e:
            logger.error(f"✗ Failed to load global configuration: {str(e)}")

    def load_sms_email_config(self):
        """Load SMS/Email configuration (reference only - manual setup required)"""
        logger.info("=" * 80)
        logger.info("LOADING SMS/EMAIL CONFIGURATION (REFERENCE)")
        logger.info("=" * 80)

        df = self._read_excel_sheet('SMS Email Config')

        logger.info("SMS/Email configuration requires manual setup in Fineract:")
        logger.info("")

        # Group by config_type
        for config_type in df['config_type'].unique():
            logger.info(f"• {config_type}:")
            type_configs = df[df['config_type'] == config_type]
            for _, row in type_configs.iterrows():
                if row['is_active'] == 'Yes':
                    logger.info(f"  - {row['config_key']}: {row['config_value']}")
                    logger.info(f"    ({row['description']})")
            logger.info("")

        logger.info("NOTE: SMS/Email notifications require:")
        logger.info("  1. External message gateway setup (Twilio/Infobip)")
        logger.info("  2. SMTP server configuration for email")
        logger.info("  3. Notification service configuration in Fineract")
        logger.info("  4. See README for integration instructions")

    def load_notification_templates(self):
        """Load notification templates via API"""
        logger.info("=" * 80)
        logger.info("LOADING NOTIFICATION TEMPLATES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Notification Templates')

        # Fineract template entity types
        # 0 = Client, 1 = Loan, 2 = Savings, etc.
        entity_mapping = {
            'Client': 0,
            'Loan': 1,
            'Savings': 2,
            'Group': 3
        }

        # Fineract template types
        # 0 = SMS, 1 = Email, 2 = System (notification)
        type_mapping = {
            'SMS': 0,
            'Email': 1,
            'System': 2
        }

        for idx, row in df.iterrows():
            try:
                if row['is_active'] != 'Yes':
                    logger.info(f"⊙ Skipping inactive template: {row['template_name']}")
                    continue

                # Determine entity and type from event trigger or channel
                # Default to Client entity
                entity_id = entity_mapping.get(row.get('entity_type', 'Client'), 0)

                # Get type from channel
                type_id = type_mapping.get(row['channel'], 0)

                # Build mappers for variable substitution
                # Mappers allow templates to fetch dynamic data from Fineract
                mappers = []

                # Common mapper patterns based on entity
                if entity_id == 0:  # Client
                    mappers.append({
                        'mappersorder': 0,
                        'mapperskey': 'client',
                        'mappersvalue': f'clients/{{{{clientId}}}}?tenantIdentifier={self.config.get("tenant", "default")}'
                    })
                elif entity_id == 1:  # Loan
                    mappers.extend([
                        {
                            'mappersorder': 0,
                            'mapperskey': 'loan',
                            'mappersvalue': f'loans/{{{{loanId}}}}?tenantIdentifier={self.config.get("tenant", "default")}'
                        },
                        {
                            'mappersorder': 1,
                            'mapperskey': 'client',
                            'mappersvalue': f'clients/{{{{clientId}}}}?tenantIdentifier={self.config.get("tenant", "default")}'
                        }
                    ])
                elif entity_id == 2:  # Savings
                    mappers.extend([
                        {
                            'mappersorder': 0,
                            'mapperskey': 'savingsAccount',
                            'mappersvalue': f'savingsaccounts/{{{{savingsId}}}}?tenantIdentifier={self.config.get("tenant", "default")}'
                        },
                        {
                            'mappersorder': 1,
                            'mapperskey': 'client',
                            'mappersvalue': f'clients/{{{{clientId}}}}?tenantIdentifier={self.config.get("tenant", "default")}'
                        }
                    ])

                # Build template data
                template_data = {
                    'entity': entity_id,
                    'type': type_id,
                    'name': row['template_name'],
                    'text': row['template_body'],
                    'mappers': mappers
                }

                # Create template via API
                try:
                    response = self.client.post('/templates', template_data)
                    template_id = response.get('resourceId')
                    logger.info(f"✓ Created template: {row['template_name']} ({row['channel']}) - ID: {template_id}")
                except Exception as e:
                    if 'already exists' in str(e).lower() or '403' in str(e):
                        logger.info(f"  ⊙ Template already exists: {row['template_name']}")
                    else:
                        raise

                time.sleep(0.2)

            except Exception as e:
                logger.error(f"✗ Failed to create template {row['template_name']}: {str(e)}")

    def load_data_tables(self):
        """Create custom data tables (custom fields)"""
        logger.info("=" * 80)
        logger.info("LOADING DATA TABLES (CUSTOM FIELDS)")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Data Tables')

        # Fineract field type mappings
        field_type_mapping = {
            'String': 'String',
            'Number': 'Number',
            'Decimal': 'Decimal',
            'Date': 'Date',
            'Datetime': 'Datetime',
            'Text': 'Text',
            'Dropdown': 'Dropdown'
        }

        # Entity type mappings
        entity_app_table_mapping = {
            'Client': 'm_client',
            'Loan': 'm_loan',
            'Savings': 'm_savings_account',
            'Group': 'm_group'
        }

        # Group by table_name
        tables = df.groupby(['entity_type', 'table_name'])

        for (entity_type, table_name), table_fields in tables:
            try:
                app_table_name = entity_app_table_mapping.get(entity_type)
                if not app_table_name:
                    logger.warning(f"Unknown entity type: {entity_type}")
                    continue

                # Build columns list
                columns = []
                for _, field in table_fields.iterrows():
                    field_type = field_type_mapping.get(field['field_type'], 'String')

                    # Determine appropriate length based on field type
                    if field_type in ['String']:
                        length = 200
                    elif field_type in ['Text']:
                        length = 1000
                    elif field_type in ['Number']:
                        length = 10
                    elif field_type in ['Decimal']:
                        length = 19  # Standard SQL DECIMAL length
                    elif field_type in ['Dropdown']:
                        length = 100
                    else:
                        length = 0  # For Date, Datetime types

                    column = {
                        'name': field['field_name'],
                        'type': field_type,
                        'mandatory': field['mandatory'] == 'Yes'
                    }

                    # Only add length if it's greater than 0
                    if length > 0:
                        column['length'] = length

                    # Add dropdown values if applicable
                    if field['field_type'] == 'Dropdown' and field['dropdown_values']:
                        column['code'] = field['field_name'] + '_options'  # Code name for dropdown

                    columns.append(column)

                # Create data table
                # entitySubType is required for Client tables
                data_table_data = {
                    'datatableName': table_name,
                    'apptableName': app_table_name,
                    'multiRow': False,
                    'columns': columns
                }

                # Add entitySubType for Client tables (required parameter)
                if entity_type == 'Client':
                    data_table_data['entitySubType'] = 'PERSON'  # or 'ENTITY' for organizations

                try:
                    response = self.client.post('/datatables', data_table_data)
                    logger.info(f"✓ Created data table: {table_name} for {entity_type}")
                    logger.info(f"  Fields: {', '.join([c['name'] for c in columns])}")

                    time.sleep(0.3)

                except Exception as e:
                    if 'already exists' in str(e).lower() or '403' in str(e):
                        logger.info(f"  ⚠ Data table already exists: {table_name}")
                    else:
                        raise

            except Exception as e:
                logger.error(f"✗ Failed to create data table {table_name}: {str(e)}")

        logger.info("")
        logger.info("NOTE: Custom fields (data tables) are now available in:")
        logger.info("  - Client forms (additional_info tab)")
        logger.info("  - Loan application forms")
        logger.info("  - Savings account forms")

    def load_tellers(self):
        """Create tellers (cash counters) at each branch"""
        logger.info("=" * 80)
        logger.info("LOADING TELLERS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Tellers')

        # Status mapping
        status_mapping = {
            'Active': 300,
            'Inactive': 100
        }

        for idx, row in df.iterrows():
            try:
                office_id = self.client.created_offices.get(row['office_name'])

                if not office_id:
                    logger.warning(f"Office not found: {row['office_name']}")
                    continue

                data = {
                    'officeId': office_id,
                    'name': row['teller_name'],
                    'description': row['description'],
                    'startDate': row['start_date'],
                    'endDate': row['end_date'],
                    'status': status_mapping.get(row['status'], 300),
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                response = self.client.post('/tellers', data)
                teller_id = response.get('resourceId') or response.get('tellerId')

                logger.info(f"✓ Created teller: {row['teller_name']} at {row['office_name']} (ID: {teller_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create teller {row['teller_name']}: {str(e)}")

        logger.info("")
        logger.info("NOTE: Tellers are now available for cash management:")
        logger.info("  - Branch managers can assign cashiers to tellers")
        logger.info("  - Cashiers can allocate/settle cash daily")
        logger.info("  - All teller transactions are tracked per office")

    def load_all(self):
        """Load all demo data in correct order"""
        logger.info("╔" + "═" * 78 + "╗")
        logger.info("║" + " " * 20 + "FINERACT DEMO DATA LOADER" + " " * 33 + "║")
        logger.info("╚" + "═" * 78 + "╝")
        logger.info(f"\nExcel file: {self.excel_file}")
        logger.info(f"Fineract URL: {self.config['fineract_url']}")
        logger.info(f"Tenant: {self.config.get('tenant', 'default')}")
        logger.info("")

        start_time = time.time()

        try:
            # Load system configurations first
            self.load_currency_config()
            self.load_working_days()
            self.load_global_configuration()
            self.load_codes_and_values()
            self.load_account_number_preferences()
            self.load_sms_email_config()  # Reference only - manual setup required
            self.load_notification_templates()  # Reference only - manual setup required
            self.load_data_tables()  # Create custom field tables

            # Load in correct order (respecting dependencies)
            self.load_offices()
            self.load_staff()
            self.load_tellers()  # Create tellers after offices
            self.load_gl_accounts()
            self.load_charges()
            self.load_fund_sources()
            self.load_payment_types()
            self.load_holidays()
            self.load_financial_activity_mappings()  # CRITICAL: Must be after GL accounts, before products
            self.enable_maker_checker()  # Enable dual authorization for critical operations
            self.load_scheduler_jobs()  # Configure automated jobs
            self.load_loan_products()
            self.load_savings_products()
            self.load_clients()
            self.load_savings_accounts()
            self.load_loan_accounts()

            elapsed_time = time.time() - start_time

            logger.info("=" * 80)
            logger.info("SUMMARY")
            logger.info("=" * 80)
            logger.info(f"Offices created: {len(self.client.created_offices)}")
            logger.info(f"Staff created: {len(self.client.created_staff)}")
            logger.info(f"GL Accounts created: {len(self.client.created_gl_accounts)}")
            logger.info(f"Charges created: {len(self.client.created_charges)}")
            logger.info(f"Fund sources created: {len(self.client.created_funds)}")
            logger.info(f"Payment types created: {len(self.client.created_payment_types)}")
            logger.info(f"Financial Activity Mappings configured: Yes")
            logger.info(f"Maker-Checker (dual authorization) enabled: Yes")
            logger.info(f"Loan products created: {len(self.client.created_loan_products)}")
            logger.info(f"Savings products created: {len(self.client.created_savings_products)}")
            logger.info(f"Clients created: {len(self.client.created_clients)}")
            logger.info(f"Savings accounts created: {len(self.client.created_savings_accounts)}")
            logger.info(f"Loan accounts created: {len(self.client.created_loan_accounts)}")
            logger.info("=" * 80)
            logger.info(f"✓ Demo data loaded successfully in {elapsed_time:.2f} seconds")
            logger.info("=" * 80)

        except KeyboardInterrupt:
            logger.warning("\n\n⚠ Process interrupted by user")
            sys.exit(1)
        except Exception as e:
            logger.error(f"\n\n✗ Fatal error: {str(e)}")
            raise


def main():
    """Main entry point"""
    if len(sys.argv) < 2:
        print("Usage: python load_demo_data.py <excel_file> [config_file]")
        print("\nExample:")
        print("  python load_demo_data.py ../output/fineract_demo_data_20241010.xlsx ../config/fineract_config.json")
        sys.exit(1)

    excel_file = sys.argv[1]
    config_file = sys.argv[2] if len(sys.argv) > 2 else '../config/fineract_config.json'

    if not os.path.exists(excel_file):
        print(f"Error: Excel file not found: {excel_file}")
        sys.exit(1)

    if not os.path.exists(config_file):
        print(f"Error: Config file not found: {config_file}")
        sys.exit(1)

    # Create logs directory
    os.makedirs('../logs', exist_ok=True)

    loader = FineractDemoDataLoader(excel_file, config_file)
    loader.load_all()


if __name__ == '__main__':
    main()
