#!/usr/bin/env python3
"""
Product Loader
Handles loading of financial products like loans, savings, charges, etc.
"""

import logging
import time
from typing import Dict
import pandas as pd

logger = logging.getLogger(__name__)


class ProductLoader:
    """Loads financial products into Fineract"""

    def __init__(self, client, excel_file: str, config: Dict):
        self.client = client
        self.excel_file = excel_file
        self.config = config

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        df = df.where(pd.notnull(df), None)
        return df

    def load_gl_accounts(self):
        """Create chart of accounts"""
        logger.info("=" * 80)
        logger.info("LOADING CHART OF ACCOUNTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Chart of Accounts')

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

                # Use upsert to update existing GL account or create new one
                response = self.client.upsert('/glaccounts', data, 'glCode', str(row['gl_code']))
                account_id = response.get('resourceId') or response.get('glAccountId') or response.get('changes', {}).get('id')

                self.client.created_gl_accounts[str(row['gl_code'])] = account_id

                logger.info(f"✓ Upserted GL Account: {row['gl_code']} - {row['gl_name']} (ID: {account_id})")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create GL account {row['gl_code']}: {str(e)}")

    def load_charges(self):
        """Create charges/fees with fee frequency support"""
        logger.info("=" * 80)
        logger.info("LOADING CHARGES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Charges')

        charge_applies_to = {
            'Loan': 1,
            'Savings': 2,
            'Client': 3
        }

        charge_time_type = {
            'Disbursement': 1,
            'Specified Due Date': 2,
            'Installment Fee': 8,
            'Overdue Installment': 9,
            'Tranche Disbursement': 12,
            'Savings Activation': 3,
            'Withdrawal': 5,
            'Withdrawal Fee': 5,
            'ATM Fee': 5,
            'Overdraft Fee': 10,
            'Saving No Activity Fee': 16,
            'Share Account Activate': 13,
            'Share Purchase': 14,
            'Share Redeem': 15,
            'Activation': 3,
            'On Demand': 2
        }

        # Fee frequency mapping
        fee_frequency_type = {
            'Weekly': 1,       # WEEKS
            'Monthly': 4,      # MONTHS
            'Yearly': 3,       # YEARS
            'Daily': 0         # DAYS (if needed)
        }

        savings_valid_charge_times = {2, 3, 5, 10, 16}  # Specified Due Date, Activation, Withdrawal, Overdraft Fee, No Activity Fee

        charge_calculation_type = {
            'Flat': 1,
            'Percentage of Amount': 2,
            'Percentage of Interest': 3
        }

        for idx, row in df.iterrows():
            try:
                charge_time_val = charge_time_type.get(row['charge_time'])

                if not charge_time_val:
                    logger.warning(f"⚠ Skipping charge {row['charge_name']}: unsupported charge_time '{row['charge_time']}'")
                    continue

                if row['charge_type'] == 'Savings' and charge_time_val not in savings_valid_charge_times:
                    logger.warning(f"⚠ Skipping charge {row['charge_name']}: chargeTimeType {charge_time_val} not valid for Savings")
                    continue

                data = {
                    'name': row['charge_name'],
                    'currencyCode': row['currency'],
                    'chargeAppliesTo': int(charge_applies_to.get(row['charge_type'])),
                    'chargeTimeType': int(charge_time_val),
                    'chargeCalculationType': int(charge_calculation_type.get(row['calculation_type'])),
                    'chargePaymentMode': 0,
                    'amount': float(row['amount']),
                    'active': row['active'] == 'Yes',
                    'locale': 'en'
                }

                # Handle penalty flag
                if 'Penalty' in row['charge_name'] and row['charge_time'] == 'Overdue Installment':
                    data['penalty'] = True

                # Handle fee frequency for recurring charges
                fee_frequency = row.get('fee_frequency')
                if fee_frequency and pd.notna(fee_frequency):
                    frequency_val = fee_frequency_type.get(fee_frequency)
                    if frequency_val is not None:
                        data['feeFrequency'] = frequency_val

                        # Add fee interval
                        fee_interval = row.get('fee_interval')
                        if fee_interval and pd.notna(fee_interval):
                            data['feeInterval'] = int(fee_interval)

                        # Add feeOnMonthDay based on frequency type
                        if fee_frequency == 'Monthly':
                            # Monthly: [day] (e.g., [1] for 1st of month)
                            fee_on_day = row.get('fee_on_day')
                            if fee_on_day and pd.notna(fee_on_day):
                                data['feeOnMonthDay'] = [int(fee_on_day)]

                        elif fee_frequency == 'Yearly':
                            # Yearly: [month, day] (e.g., [1, 1] for January 1st)
                            fee_on_month = row.get('fee_on_month')
                            fee_on_day = row.get('fee_on_day')
                            if fee_on_month and pd.notna(fee_on_month) and fee_on_day and pd.notna(fee_on_day):
                                data['feeOnMonthDay'] = [int(fee_on_month), int(fee_on_day)]

                        elif fee_frequency == 'Weekly':
                            # Weekly: [day of week] (e.g., [1] for Monday)
                            fee_on_day = row.get('fee_on_day')
                            if fee_on_day and pd.notna(fee_on_day):
                                data['feeOnMonthDay'] = [int(fee_on_day)]

                # Use upsert to update existing charge or create new one
                response = self.client.upsert('/charges', data, 'name', row['charge_name'])
                charge_id = response.get('resourceId') or response.get('chargeId') or response.get('changes', {}).get('id')

                self.client.created_charges[row['charge_name']] = charge_id

                logger.info(f"✓ Upserted charge: {row['charge_name']} (ID: {charge_id})")
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

    def load_loan_products(self):
        """Create loan products with accounting mappings"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN PRODUCTS")
        logger.info("=" * 80)

        df_products = self._read_excel_sheet('Loan Products')
        df_accounting = self._read_excel_sheet('Loan Product Accounting')
        df_payment_channels = self._read_excel_sheet('Payment Type Accounting')

        for idx, row in df_products.iterrows():
            try:
                # Get accounting mappings for this product
                product_mappings = df_accounting[df_accounting['product_short_name'] == row['short_name']]

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
                    'repaymentFrequencyType': 2,
                    'interestRatePerPeriod': row['interest_rate_default'],
                    'minInterestRatePerPeriod': row['interest_rate_min'],
                    'maxInterestRatePerPeriod': row['interest_rate_max'],
                    'interestRateFrequencyType': 2,
                    'amortizationType': 1,
                    'interestType': 0,
                    'interestCalculationPeriodType': 1,
                    'transactionProcessingStrategyCode': 'mifos-standard-strategy',
                    'accountingRule': 2,
                    'locale': 'en',
                    'dateFormat': 'yyyy-MM-dd',
                    'graceOnPrincipalPayment': row['grace_on_principal_periods'],
                    'graceOnInterestPayment': row['grace_on_interest_periods'],
                    'daysInYearType': 365,
                    'daysInMonthType': 30,
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
                write_off_id = self.client.created_gl_accounts.get('93')
                overpayment_liability_id = self.client.created_gl_accounts.get('61')

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

                    # Add payment channel to fund source mappings
                    payment_channel_mappings = []
                    for _, payment_row in df_payment_channels.iterrows():
                        payment_type_id = self.client.created_payment_types.get(payment_row['payment_type'])
                        gl_code = str(payment_row['gl_code'])
                        fund_source_account_id = self.client.created_gl_accounts.get(gl_code)

                        if payment_type_id and fund_source_account_id:
                            payment_channel_mappings.append({
                                'paymentTypeId': payment_type_id,
                                'fundSourceAccountId': fund_source_account_id
                            })

                    if payment_channel_mappings:
                        data['paymentChannelToFundSourceMappings'] = payment_channel_mappings
                        logger.info(f"  Added {len(payment_channel_mappings)} payment channel mappings")
                else:
                    logger.warning(f"  ⚠ Some GL accounts not found for {row['short_name']}, using accountingRule=1 (None)")
                    data['accountingRule'] = 1

                # Use upsert to update existing product or create new one
                response = self.client.upsert('/loanproducts', data, 'shortName', short_name)
                product_id = response.get('resourceId') or response.get('loanProductId') or response.get('changes', {}).get('id')

                self.client.created_loan_products[row['short_name']] = product_id

                logger.info(f"✓ Upserted loan product: {row['product_name']} (ID: {product_id})")
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
        df_payment_channels = self._read_excel_sheet('Payment Type Accounting')

        for idx, row in df_products.iterrows():
            try:
                # Get accounting mappings for this product
                product_mappings = df_accounting[df_accounting['product_short_name'] == row['short_name']]

                def get_gl_id(mapping_type):
                    mapping = product_mappings[product_mappings['mapping_type'] == mapping_type]
                    if len(mapping) > 0:
                        gl_code = str(mapping.iloc[0]['gl_code'])
                        return self.client.created_gl_accounts.get(gl_code)
                    return None

                # Shorten the shortName to max 4 characters
                short_name = row['short_name']
                if len(short_name) > 4:
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
                    'interestCompoundingPeriodType': 4,
                    'interestPostingPeriodType': 4,
                    'interestCalculationType': 1,
                    'interestCalculationDaysInYearType': 365,
                    'minRequiredOpeningBalance': row['minimum_opening_balance'],
                    'accountingRule': 2,
                    'locale': 'en',
                    'withdrawalFeeForTransfers': row['withdrawal_fee_for_transfers'] == 'Yes',
                    'allowOverdraft': row['overdraft_allowed'] == 'Yes',
                    'withHoldTax': False
                }

                # Add GL account mappings
                savings_reference_id = get_gl_id('Savings Reference')
                savings_control_id = get_gl_id('Savings Control')
                interest_expense_id = get_gl_id('Interest on Savings')
                income_from_fees_id = get_gl_id('Income from Fees')
                transfer_suspense_id = get_gl_id('Transfer in Suspense')
                overdraft_portfolio_id = get_gl_id('Overdraft Portfolio Control') or self.client.created_gl_accounts.get('51')
                income_from_interest_id = get_gl_id('Overdraft Interest Income') or self.client.created_gl_accounts.get('81')
                write_off_id = get_gl_id('Overdraft Write-off') or self.client.created_gl_accounts.get('93')

                if all([savings_reference_id, savings_control_id, interest_expense_id,
                        income_from_fees_id, transfer_suspense_id, overdraft_portfolio_id,
                        income_from_interest_id, write_off_id]):
                    data['savingsReferenceAccountId'] = savings_reference_id
                    data['savingsControlAccountId'] = savings_control_id
                    data['interestOnSavingsAccountId'] = interest_expense_id
                    data['incomeFromFeeAccountId'] = income_from_fees_id
                    data['incomeFromPenaltyAccountId'] = income_from_fees_id
                    data['transfersInSuspenseAccountId'] = transfer_suspense_id
                    data['overdraftPortfolioControlId'] = overdraft_portfolio_id
                    data['incomeFromInterestId'] = income_from_interest_id
                    data['writeOffAccountId'] = write_off_id

                    # Add payment channel to fund source mappings
                    payment_channel_mappings = []
                    for _, payment_row in df_payment_channels.iterrows():
                        payment_type_id = self.client.created_payment_types.get(payment_row['payment_type'])
                        gl_code = str(payment_row['gl_code'])
                        fund_source_account_id = self.client.created_gl_accounts.get(gl_code)

                        if payment_type_id and fund_source_account_id:
                            payment_channel_mappings.append({
                                'paymentTypeId': payment_type_id,
                                'fundSourceAccountId': fund_source_account_id
                            })

                    if payment_channel_mappings:
                        data['paymentChannelToFundSourceMappings'] = payment_channel_mappings
                        logger.info(f"  Added {len(payment_channel_mappings)} payment channel mappings")
                else:
                    logger.warning(f"  ⚠ Some GL accounts not found for {row['short_name']}, using accountingRule=1 (None)")
                    data['accountingRule'] = 1

                # Use upsert to update existing product or create new one
                response = self.client.upsert('/savingsproducts', data, 'shortName', short_name)
                product_id = response.get('resourceId') or response.get('savingsProductId') or response.get('changes', {}).get('id')

                # Store with both original and shortened names
                self.client.created_savings_products[row['short_name']] = product_id
                self.client.created_savings_products[short_name] = product_id

                logger.info(f"✓ Created savings product: {row['product_name']} (ID: {product_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create savings product {row['product_name']}: {str(e)}")

    def load_financial_activity_mappings(self):
        """Configure Financial Activity to GL Account mappings"""
        logger.info("=" * 80)
        logger.info("LOADING FINANCIAL ACTIVITY MAPPINGS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Financial Activity Mapping')

        activity_mapping = {
            'Asset Transfer': 100,
            'Liability Transfer': 200,
            'Cash at Mainvault': 101,
            'Cash at Teller': 102,
            'Opening Balances Contra': 300,
            'Fund Source': 103,
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
        """Enable Maker-Checker (4-eyes principle) for critical operations

        NOTE: Fineract's maker-checker is boolean-only (enabled/disabled).
        When enabled, ALL operations of that type require approval.
        Amount-based thresholds are NOT supported by Fineract's native API.
        """
        logger.info("=" * 80)
        logger.info("ENABLING MAKER-CHECKER (DUAL AUTHORIZATION)")
        logger.info("=" * 80)

        # Fetch available permissions from Fineract
        try:
            permissions_response = self.client.get('/permissions')
            available_permissions = {perm['code'] for perm in permissions_response if perm.get('code')}
            logger.info(f"Found {len(available_permissions)} permissions in Fineract")
            logger.debug(f"Sample permissions: {list(available_permissions)[:20]}")
        except Exception as e:
            logger.warning(f"Could not fetch available permissions: {str(e)}")
            available_permissions = set()

        df = self._read_excel_sheet('Maker Checker Config')

        # Filter only enabled permissions
        enabled_df = df[df['enabled'] == True]
        logger.info(f"Enabling {len(enabled_df)} out of {len(df)} configured maker-checker permissions")
        logger.info("")

        # Try to enable each permission individually for better error handling
        successful_count = 0
        failed_permissions = []
        skipped_count = 0

        for idx, row in df.iterrows():
            # Skip if not enabled
            if not row.get('enabled', False):
                logger.debug(f"  Skipping disabled: {row['task_name']}")
                skipped_count += 1
                continue

            entity = row['entity'].upper()
            action = row['action'].upper()
            permission_code = f"{action}_{entity}"

            logger.info(f"  Enabling: {row['task_name']}")
            logger.info(f"    Permission code: {permission_code}")

            # Check if permission exists in available permissions
            if available_permissions and permission_code not in available_permissions:
                logger.warning(f"    ⚠ Permission {permission_code} not found in available permissions")
                logger.info(f"    Searching for similar permissions...")

                # Search for similar permissions
                similar = [p for p in available_permissions if entity in p or any(word in p for word in action.split('_'))]
                if similar:
                    logger.info(f"    Similar permissions found: {similar[:5]}")

                failed_permissions.append((row['task_name'], permission_code, "not available"))
                continue

            try:
                # Enable this single permission
                data = {
                    'permissions': {permission_code: True}
                }

                self.client.put('/permissions', data)
                successful_count += 1
                logger.info(f"    ✓ Enabled successfully")
                logger.info(f"      Maker: {row['maker_role']}, Checker: {row['checker_role']}")
                logger.info(f"      Note: ALL {action} operations on {entity} will require approval")

                time.sleep(0.2)

            except Exception as e:
                error_str = str(e).lower()
                if ('404' in error_str and 'does not exist' in error_str) or 'permission with code' in error_str:
                    logger.warning(f"    ⚠ Permission not available in this Fineract version: {permission_code}")
                    failed_permissions.append((row['task_name'], permission_code, "not available"))
                elif 'already enabled' in error_str:
                    logger.info(f"    ⊙ Already enabled: {permission_code}")
                    successful_count += 1
                else:
                    logger.warning(f"    ⚠ Failed: {str(e)}")
                    failed_permissions.append((row['task_name'], permission_code, "error"))

        # Summary
        logger.info("")
        logger.info(f"✓ Successfully enabled maker-checker for {successful_count}/{len(enabled_df)} requested operations")
        if skipped_count > 0:
            logger.info(f"⊙ Skipped {skipped_count} disabled permissions")

        if failed_permissions:
            logger.info("")
            logger.info(f"⚠ {len(failed_permissions)} permission(s) could not be enabled:")
            for task_name, perm_code, reason in failed_permissions:
                if reason == "not available":
                    logger.info(f"  • {task_name} ({perm_code}) - Not available in this Fineract version")
                else:
                    logger.info(f"  • {task_name} ({perm_code}) - Configuration error")

        logger.info("")
        logger.info("IMPORTANT: Fineract's maker-checker is BOOLEAN (on/off).")
        logger.info("           When enabled, ALL operations of that type require approval.")
        logger.info("           Amount-based thresholds are NOT supported by Fineract's native API.")
        logger.info("")
        logger.info("      For unavailable permissions, check:")
        logger.info("        - Your Fineract version may not support all permission codes")
        logger.info("        - Some features may require plugins or newer versions")
        logger.info("        - Manual configuration via Admin UI may be needed")

    def load_loan_provisioning(self):
        """Create loan provisioning criteria (COBAC standards) using existing Fineract categories"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN PROVISIONING CRITERIA")
        logger.info("=" * 80)

        # Check if any loan products were created
        if not self.client.created_loan_products:
            logger.warning("⚠ No loan products found. Skipping loan provisioning criteria.")
            logger.warning("  Loan provisioning requires at least one loan product to be created first.")
            return

        df = self._read_excel_sheet('Loan Provisioning')

        # Fineract comes with 4 pre-existing provisioning categories: STANDARD, SUB-STANDARD, DOUBTFUL, LOSS
        # We use these existing categories instead of creating new ones
        logger.info("Using Fineract's pre-existing provisioning categories:")
        logger.info("  • STANDARD (ID: 1)")
        logger.info("  • SUB-STANDARD (ID: 2)")
        logger.info("  • DOUBTFUL (ID: 3)")
        logger.info("  • LOSS (ID: 4)")
        logger.info("")

        # Build provisioning criteria definitions
        provisioning_criteria = []
        for idx, row in df.iterrows():
            # Get GL account IDs from the Excel sheet
            liability_gl_code = str(row['liability_gl_code'])
            expense_gl_code = str(row['expense_gl_code'])

            liability_account_id = self.client.created_gl_accounts.get(liability_gl_code)
            expense_account_id = self.client.created_gl_accounts.get(expense_gl_code)

            if not liability_account_id or not expense_account_id:
                logger.warning(f"  ⚠ Skipping {row['category_name']}: GL accounts not found")
                logger.warning(f"    Liability GL {liability_gl_code}: {liability_account_id}")
                logger.warning(f"    Expense GL {expense_gl_code}: {expense_account_id}")
                continue

            criterion = {
                'categoryId': int(row['category_id']),
                'categoryName': row['category_name'],
                'minAge': int(row['min_days_overdue']),
                'maxAge': int(row['max_days_overdue']),
                'provisioningPercentage': float(row['provision_percentage']),
                'liabilityAccount': liability_account_id,
                'expenseAccount': expense_account_id
            }
            provisioning_criteria.append(criterion)

        if not provisioning_criteria:
            logger.error("✗ No provisioning criteria definitions created. Cannot proceed.")
            return

        # Format loan products as required by API
        loan_products = []
        for product_name, product_id in self.client.created_loan_products.items():
            loan_products.append({
                'id': product_id,
                'name': product_name,
                'includeInBorrowerCycle': False
            })

        # Create the provisioning criteria
        data = {
            'criteriaName': 'COBAC Provisioning Standards',
            'loanProducts': loan_products,
            'definitions': provisioning_criteria,
            'locale': 'en'
        }

        logger.info(f"Creating provisioning criteria for {len(loan_products)} loan products")
        logger.info(f"Loan product IDs: {[p['id'] for p in loan_products]}")
        logger.info(f"Definitions: {len(provisioning_criteria)} categories")
        logger.info("")

        try:
            response = self.client.post('/provisioningcriteria', data)
            criteria_id = response.get('resourceId')

            logger.info(f"✓ Created provisioning criteria: COBAC Provisioning Standards (ID: {criteria_id})")
            logger.info(f"  Applied to {len(loan_products)} loan products")
            logger.info("")

            for criterion in provisioning_criteria:
                logger.info(f"  • {criterion['categoryName']}: {criterion['minAge']}-{criterion['maxAge']} days, "
                          f"{criterion['provisioningPercentage']}% provision")

            time.sleep(0.5)

        except Exception as e:
            logger.error(f"✗ Failed to create loan provisioning criteria: {str(e)}")
            logger.error(f"  Payload sent: {data}")

    def load_collateral_types(self):
        """Create collateral types"""
        logger.info("=" * 80)
        logger.info("LOADING COLLATERAL TYPES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Collateral Types')

        # Initialize storage for collateral type IDs (for code values, used by loan collateral API)
        if not hasattr(self.client, 'created_collateral_types'):
            self.client.created_collateral_types = {}

        # Also store collateral management IDs separately if needed
        if not hasattr(self.client, 'created_collateral_management_ids'):
            self.client.created_collateral_management_ids = {}

        # First, ensure LoanCollateral code exists
        try:
            codes_list = self.client.get('/codes')
            loan_collateral_code = next((c for c in codes_list if c['name'] == 'LoanCollateral'), None)
            if not loan_collateral_code:
                code_response = self.client.post('/codes', {'name': 'LoanCollateral'})
                loan_collateral_code_id = code_response.get('resourceId')
                logger.info(f"✓ Created LoanCollateral code (ID: {loan_collateral_code_id})")
            else:
                loan_collateral_code_id = loan_collateral_code['id']
                logger.info(f"⊙ LoanCollateral code already exists (ID: {loan_collateral_code_id})")
        except Exception as e:
            logger.error(f"✗ Failed to create/get LoanCollateral code: {str(e)}")
            loan_collateral_code_id = None

        for idx, row in df.iterrows():
            try:
                data = {
                    'name': row['collateral_type'],
                    'quality': 'EXCELLENT',  # Default quality
                    'basePrice': 100.0,  # Default base price
                    'pctToBase': 100.0,  # 100% of base price
                    'unitType': 'UNIT',  # Unit type
                    'currency': 'XAF',
                    'locale': 'en'
                }

                response = self.client.post('/collateral-management', data)
                collateral_mgmt_id = response.get('resourceId')

                # Store the collateral management ID separately
                self.client.created_collateral_management_ids[row['collateral_type']] = collateral_mgmt_id

                # Also create as code value under LoanCollateral (this is what the loan collateral API uses)
                if loan_collateral_code_id:
                    try:
                        code_value_data = {
                            'name': row['collateral_type'],
                            'position': idx + 1,
                            'isActive': True,
                            'description': row.get('description', '')
                        }
                        cv_response = self.client.post(f'/codes/{loan_collateral_code_id}/codevalues', code_value_data)
                        code_value_id = cv_response.get('resourceId')

                        # Store the CODE VALUE ID - this is what the loan collateral API uses
                        self.client.created_collateral_types[row['collateral_type']] = code_value_id

                        logger.info(f"✓ Created collateral type: {row['collateral_type']} (Code Value ID: {code_value_id}, Mgmt ID: {collateral_mgmt_id})")
                    except Exception as cv_error:
                        if 'already exists' in str(cv_error).lower():
                            # Try to get the existing code value ID
                            try:
                                code_values = self.client.get(f'/codes/{loan_collateral_code_id}/codevalues')
                                existing_cv = next((cv for cv in code_values if cv['name'] == row['collateral_type']), None)
                                if existing_cv:
                                    self.client.created_collateral_types[row['collateral_type']] = existing_cv['id']
                                    logger.info(f"✓ Collateral type exists: {row['collateral_type']} (Code Value ID: {existing_cv['id']})")
                            except:
                                logger.warning(f"  ⚠ Could not get existing code value ID for: {row['collateral_type']}")
                        else:
                            logger.warning(f"  ⚠ Could not create code value: {str(cv_error)}")
                            logger.info(f"✓ Created collateral type (mgmt only): {row['collateral_type']} (ID: {collateral_mgmt_id})")
                else:
                    logger.warning(f"  ⚠ LoanCollateral code not available, created mgmt type only: {row['collateral_type']}")

                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create collateral type {row['collateral_type']}: {str(e)}")

    def load_teller_accounting_rules(self):
        """Create accounting rules for teller cash shortage and overage"""
        logger.info("=" * 80)
        logger.info("LOADING TELLER ACCOUNTING RULES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Teller Cashier Mapping')

        rules_created = 0

        for idx, row in df.iterrows():
            office_name = row['office_name']
            teller_name = row['teller_name']

            # Get office ID
            office_id = self.client.created_offices.get(office_name)
            if not office_id:
                logger.warning(f"  ⚠ Office not found: {office_name}, skipping rules for {teller_name}")
                continue

            # Get GL account IDs
            cash_gl_id = self.client.created_gl_accounts.get(str(row['cash_gl_code']))
            shortage_gl_id = self.client.created_gl_accounts.get(str(row['shortage_gl_code']))
            overage_gl_id = self.client.created_gl_accounts.get(str(row['overage_gl_code']))

            if not all([cash_gl_id, shortage_gl_id, overage_gl_id]):
                logger.warning(f"  ⚠ Some GL accounts not found for {teller_name}, skipping rules")
                logger.debug(f"    Cash GL: {cash_gl_id}, Shortage GL: {shortage_gl_id}, Overage GL: {overage_gl_id}")
                continue

            try:
                # Rule 1: Cash Shortage (Debit: Shortage Expense, Credit: Cash)
                shortage_rule_data = {
                    'name': f'Cash Shortage - {teller_name}',
                    'officeId': office_id,
                    'accountToDebit': shortage_gl_id,
                    'accountToCredit': cash_gl_id,
                    'description': f'Record cash shortage at {teller_name} in {office_name}'
                }

                response = self.client.post('/accountingrules', shortage_rule_data)
                shortage_rule_id = response.get('resourceId')
                logger.info(f"✓ Created shortage rule: {shortage_rule_data['name']} (ID: {shortage_rule_id})")
                logger.info(f"  Debit: GL {row['shortage_gl_code']}, Credit: GL {row['cash_gl_code']}")
                time.sleep(0.3)
                rules_created += 1

                # Rule 2: Cash Overage (Debit: Cash, Credit: Overage Income)
                overage_rule_data = {
                    'name': f'Cash Overage - {teller_name}',
                    'officeId': office_id,
                    'accountToDebit': cash_gl_id,
                    'accountToCredit': overage_gl_id,
                    'description': f'Record cash overage at {teller_name} in {office_name}'
                }

                response = self.client.post('/accountingrules', overage_rule_data)
                overage_rule_id = response.get('resourceId')
                logger.info(f"✓ Created overage rule: {overage_rule_data['name']} (ID: {overage_rule_id})")
                logger.info(f"  Debit: GL {row['cash_gl_code']}, Credit: GL {row['overage_gl_code']}")
                time.sleep(0.3)
                rules_created += 1

            except Exception as e:
                logger.error(f"✗ Failed to create accounting rules for {teller_name}: {str(e)}")

        logger.info("")
        logger.info(f"✓ Created {rules_created} accounting rules for teller cash management")
        logger.info("  Cashiers can use these rules to record cash shortages/overages")
        logger.info("  Access: Admin → Accounting → Frequent Postings → Accounting Rules")

    def load_floating_rates(self):
        """Create floating interest rates"""
        logger.info("=" * 80)
        logger.info("LOADING FLOATING RATES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Floating Rates')

        for idx, row in df.iterrows():
            try:
                # Create floating rate
                rate_data = {
                    'name': row['rate_name'],
                    'isBaseLendingRate': row['is_base_rate'] == 'Yes',
                    'isActive': row['is_active'] == 'Yes'
                }

                response = self.client.post('/floatingrates', rate_data)
                rate_id = response.get('resourceId')

                logger.info(f"✓ Created floating rate: {row['rate_name']} (ID: {rate_id})")

                # Create rate period
                period_data = {
                    'fromDate': row['from_date'],
                    'interestRate': float(row['rate_value']),
                    'isDifferentialToBaseLendingRate': False,
                    'isActive': True,
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                self.client.post(f'/floatingrates/{rate_id}/floatingrateperiods', period_data)
                logger.info(f"  Added rate period: {row['rate_value']}% from {row['from_date']}")

                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create floating rate {row['rate_name']}: {str(e)}")

    def load_delinquency_buckets(self):
        """Create delinquency buckets for arrears classification"""
        logger.info("=" * 80)
        logger.info("LOADING DELINQUENCY BUCKETS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Delinquency Buckets')

        # Step 1: Create delinquency ranges first
        range_ids = []
        logger.info("Creating delinquency ranges...")

        for idx, row in df.iterrows():
            try:
                range_data = {
                    'classification': row['classification'],
                    'minimumAgeDays': int(row['min_days_overdue']),
                    'maximumAgeDays': int(row['max_days_overdue']),
                    'locale': 'en'
                }

                response = self.client.post('/delinquency/ranges', range_data)
                range_id = response.get('resourceId')
                range_ids.append(range_id)

                logger.info(f"  ✓ Created delinquency range: {row['classification']} "
                          f"({row['min_days_overdue']}-{row['max_days_overdue']} days, ID: {range_id})")
                time.sleep(0.2)

            except Exception as e:
                logger.error(f"  ✗ Failed to create delinquency range {row['classification']}: {str(e)}")

        if not range_ids:
            logger.error("✗ No delinquency ranges were created. Cannot create bucket.")
            return

        # Step 2: Create the delinquency bucket with the range IDs
        try:
            bucket_name = "Loan Arrears Classification"
            bucket_data = {
                'name': bucket_name,
                'ranges': range_ids  # Array of range IDs, not range objects
            }

            logger.info(f"\nCreating delinquency bucket with {len(range_ids)} ranges...")
            response = self.client.post('/delinquency/buckets', bucket_data)
            bucket_id = response.get('resourceId')

            # Store bucket ID for later mapping to loan products
            self.client.created_delinquency_bucket_id = bucket_id

            logger.info(f"✓ Created delinquency bucket: {bucket_name} (ID: {bucket_id})")
            logger.info(f"  Configured with {len(range_ids)} delinquency ranges")

            # Now map this bucket to all existing loan products
            if self.client.created_loan_products:
                logger.info(f"\nMapping delinquency bucket to {len(self.client.created_loan_products)} loan products...")
                for product_name, product_id in self.client.created_loan_products.items():
                    try:
                        # Update loan product with delinquency bucket
                        update_data = {
                            'delinquencyBucketId': bucket_id,
                            'locale': 'en'
                        }
                        self.client.put(f'/loanproducts/{product_id}', update_data)
                        logger.info(f"  ✓ Mapped to loan product: {product_name}")
                        time.sleep(0.2)
                    except Exception as e:
                        logger.warning(f"  ⚠ Failed to map to {product_name}: {str(e)}")
            else:
                logger.warning("  ⚠ No loan products found to map delinquency bucket")

        except Exception as e:
            logger.error(f"✗ Failed to create delinquency bucket: {str(e)}")

    def load_tax_groups(self):
        """Create tax groups and tax components"""
        logger.info("=" * 80)
        logger.info("LOADING TAX GROUPS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Tax Groups')

        # Group by tax_group_name
        tax_groups = df.groupby('tax_group_name')

        # Store tax group IDs by type for later mapping
        if not hasattr(self.client, 'created_tax_groups'):
            self.client.created_tax_groups = {}

        for group_name, group_data in tax_groups:
            try:
                # Get credit GL account
                first_row = group_data.iloc[0]
                credit_gl_code = str(first_row['credit_gl_code'])
                credit_gl_id = self.client.created_gl_accounts.get(credit_gl_code)

                if not credit_gl_id:
                    logger.warning(f"  ⚠ GL account not found: {credit_gl_code}, skipping tax group {group_name}")
                    continue

                # Create tax components first
                tax_component_ids = []
                for _, row in group_data.iterrows():
                    component_data = {
                        'name': row['tax_component_name'],
                        'percentage': float(row['tax_percentage']),
                        'startDate': row['start_date'],
                        'creditAccountType': row['credit_account_type'],
                        'creditAcountId': credit_gl_id,  # Note: Fineract API typo
                        'dateFormat': 'yyyy-MM-dd',
                        'locale': 'en'
                    }

                    comp_response = self.client.post('/taxes/component', component_data)
                    comp_id = comp_response.get('resourceId')
                    tax_component_ids.append(comp_id)
                    logger.info(f"  ✓ Created tax component: {row['tax_component_name']} ({row['tax_percentage']}%)")

                # Create tax group with components
                group_data_payload = {
                    'name': group_name,
                    'taxComponents': tax_component_ids,
                    'locale': 'en'
                }

                group_response = self.client.post('/taxes/group', group_data_payload)
                group_id = group_response.get('resourceId')

                # Store tax group ID by tax type
                tax_type = first_row['tax_type']
                self.client.created_tax_groups[tax_type] = group_id

                logger.info(f"✓ Created tax group: {group_name} (ID: {group_id})")
                logger.info(f"  Type: {tax_type}")
                logger.info(f"  Components: {len(tax_component_ids)}")

                time.sleep(0.3)

            except Exception as e:
                logger.error(f"✗ Failed to create tax group {group_name}: {str(e)}")

        # Map Savings Interest Tax to all savings products
        savings_tax_id = self.client.created_tax_groups.get('Savings Interest')
        if savings_tax_id and self.client.created_savings_products:
            logger.info(f"\nMapping Savings Interest Tax to {len(self.client.created_savings_products)} savings products...")
            for product_name, product_id in self.client.created_savings_products.items():
                try:
                    # Update savings product with tax group
                    update_data = {
                        'withHoldTax': True,
                        'taxGroupId': savings_tax_id,
                        'locale': 'en'
                    }
                    self.client.put(f'/savingsproducts/{product_id}', update_data)
                    logger.info(f"  ✓ Enabled 15% WHT on: {product_name}")
                    time.sleep(0.2)
                except Exception as e:
                    logger.warning(f"  ⚠ Failed to map tax to {product_name}: {str(e)}")
        elif not savings_tax_id:
            logger.warning("  ⚠ Savings Interest Tax group not found")
        elif not self.client.created_savings_products:
            logger.warning("  ⚠ No savings products found to map tax group")
