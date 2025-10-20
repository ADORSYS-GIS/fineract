#!/usr/bin/env python3
"""
Account Loader
Handles loading of loan and savings accounts
"""

import logging
import time
from typing import Dict
import pandas as pd

logger = logging.getLogger(__name__)


class AccountLoader:
    """Loads loan and savings accounts into Fineract"""

    def __init__(self, client, excel_file: str, config: Dict):
        self.client = client
        self.excel_file = excel_file
        self.config = config

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        df = df.where(pd.notnull(df), None)
        return df

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
                    'transactionProcessingStrategyCode': 'mifos-standard-strategy',
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

    def load_savings_deposits(self):
        """Process savings deposit transactions from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING SAVINGS DEPOSITS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Savings Deposits')

        for idx, row in df.iterrows():
            try:
                # Find savings account ID by account number
                savings_id = self.client.created_savings_accounts.get(row['savings_account_number'])

                if not savings_id:
                    logger.warning(f"  ⚠ Savings account not found: {row['savings_account_number']}")
                    continue

                # Get payment type ID
                payment_type_id = self.client.created_payment_types.get(row['payment_type'], 1)

                # Create deposit transaction
                deposit_data = {
                    'transactionDate': row['transaction_date'],
                    'transactionAmount': float(row['transaction_amount']),
                    'paymentTypeId': payment_type_id,
                    'receiptNumber': row.get('receipt_number', ''),
                    'note': row.get('note', ''),
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                self.client.post(f'/savingsaccounts/{savings_id}/transactions?command=deposit',
                               deposit_data)

                logger.info(f"  ✓ Deposited {row['transaction_amount']:,.0f} XAF to {row['savings_account_number']} "
                          f"({row['client_name']}) - Receipt: {row.get('receipt_number', 'N/A')}")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"  ✗ Failed to process deposit for {row.get('savings_account_number', 'unknown')}: {str(e)}")

        logger.info(f"\n✓ Completed loading {len(df)} deposit transactions")

    def load_savings_withdrawals(self):
        """Process savings withdrawal transactions from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING SAVINGS WITHDRAWALS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Savings Withdrawals')

        for idx, row in df.iterrows():
            try:
                # Find savings account ID by account number
                savings_id = self.client.created_savings_accounts.get(row['savings_account_number'])

                if not savings_id:
                    logger.warning(f"  ⚠ Savings account not found: {row['savings_account_number']}")
                    continue

                # Get payment type ID
                payment_type_id = self.client.created_payment_types.get(row['payment_type'], 1)

                # Create withdrawal transaction
                withdrawal_data = {
                    'transactionDate': row['transaction_date'],
                    'transactionAmount': float(row['transaction_amount']),
                    'paymentTypeId': payment_type_id,
                    'receiptNumber': row.get('receipt_number', ''),
                    'note': row.get('note', ''),
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                self.client.post(f'/savingsaccounts/{savings_id}/transactions?command=withdrawal',
                               withdrawal_data)

                logger.info(f"  ✓ Withdrew {row['transaction_amount']:,.0f} XAF from {row['savings_account_number']} "
                          f"({row['client_name']}) - Receipt: {row.get('receipt_number', 'N/A')}")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"  ✗ Failed to process withdrawal for {row.get('savings_account_number', 'unknown')}: {str(e)}")

        logger.info(f"\n✓ Completed loading {len(df)} withdrawal transactions")

    def load_loan_repayments(self):
        """Process loan repayment transactions from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN REPAYMENTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Loan Repayments')

        for idx, row in df.iterrows():
            try:
                # Find loan account ID by account number
                loan_id = self.client.created_loan_accounts.get(row['loan_account_number'])

                if not loan_id:
                    logger.warning(f"  ⚠ Loan account not found: {row['loan_account_number']}")
                    continue

                # Get payment type ID
                payment_type_id = self.client.created_payment_types.get(row['payment_type'], 1)

                # Calculate total repayment amount
                total_amount = (float(row['principal_amount']) +
                              float(row['interest_amount']) +
                              float(row.get('fee_amount', 0)) +
                              float(row.get('penalty_amount', 0)))

                # Create repayment transaction
                repayment_data = {
                    'transactionDate': row['transaction_date'],
                    'transactionAmount': total_amount,
                    'paymentTypeId': payment_type_id,
                    'receiptNumber': row.get('receipt_number', ''),
                    'note': row.get('note', ''),
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                self.client.post(f'/loans/{loan_id}/transactions?command=repayment',
                               repayment_data)

                logger.info(f"  ✓ Repayment {total_amount:,.0f} XAF for {row['loan_account_number']} "
                          f"({row['client_name']}) - Principal: {row['principal_amount']:,.0f}, "
                          f"Interest: {row['interest_amount']:,.0f} - Receipt: {row.get('receipt_number', 'N/A')}")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"  ✗ Failed to process repayment for {row.get('loan_account_number', 'unknown')}: {str(e)}")

        logger.info(f"\n✓ Completed loading {len(df)} repayment transactions")

    def load_inter_branch_transfers(self):
        """Process inter-branch cash transfer transactions from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING INTER-BRANCH TRANSFERS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Inter-Branch Transfers')

        logger.info(f"\nProcessing {len(df)} inter-branch transfers...")
        logger.info("Note: Inter-branch transfers use GL journal entries for accounting.\n")

        for idx, row in df.iterrows():
            try:
                # Get office IDs
                from_office_id = self.client.created_offices.get(row['from_office'])
                to_office_id = self.client.created_offices.get(row['to_office'])

                if not from_office_id or not to_office_id:
                    logger.warning(f"  ⚠ Office not found: {row['from_office']} or {row['to_office']}")
                    continue

                # Get GL account IDs for inter-branch transfers
                # Using "Transfer in Suspense" accounts (typically GL 15 for asset transfer, GL 25 for liability)
                asset_transfer_gl = self.client.created_gl_accounts.get('15')  # Inter-branch Transfer Asset
                liability_transfer_gl = self.client.created_gl_accounts.get('25')  # Inter-branch Transfer Liability

                if not asset_transfer_gl or not liability_transfer_gl:
                    logger.warning(f"  ⚠ Inter-branch transfer GL accounts not configured (15, 25)")
                    logger.info(f"  Creating manual journal entry for: {row['reference_number']}")

                # Create a journal entry for the inter-branch transfer
                # Debit: From Office (decrease cash)
                # Credit: To Office (increase cash)
                journal_entry_data = {
                    'officeId': from_office_id,
                    'transactionDate': row['transfer_date'],
                    'referenceNumber': row['reference_number'],
                    'comments': f"Inter-branch transfer: {row['description']}",
                    'currencyCode': 'XAF',
                    'credits': [
                        {
                            'glAccountId': asset_transfer_gl if asset_transfer_gl else 1,
                            'amount': float(row['transfer_amount'])
                        }
                    ],
                    'debits': [
                        {
                            'glAccountId': liability_transfer_gl if liability_transfer_gl else 2,
                            'amount': float(row['transfer_amount'])
                        }
                    ],
                    'dateFormat': 'yyyy-MM-dd',
                    'locale': 'en'
                }

                # Post the journal entry
                response = self.client.post('/journalentries', journal_entry_data)
                transaction_id = response.get('transactionId') or response.get('resourceId')

                logger.info(f"  ✓ Transfer: {row['from_office']} → {row['to_office']}")
                logger.info(f"    Amount: {row['transfer_amount']:,.0f} XAF | Ref: {row['reference_number']}")
                logger.info(f"    Description: {row['description']}")
                logger.info(f"    Journal Entry ID: {transaction_id}")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"  ✗ Failed to process transfer {row.get('reference_number', 'unknown')}: {str(e)}")

        logger.info(f"\n✓ Completed loading {len(df)} inter-branch transfers")
        logger.info("\nNote: Inter-branch transfers are recorded as GL journal entries.")
        logger.info("      View: Accounting → Frequent Postings → View Journal Entries")

    def load_loan_collateral(self):
        """Assign collateral to loans from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN COLLATERAL ASSIGNMENTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Loan Collateral')

        logger.info("NOTE: Loan collateral management requires additional configuration:")
        logger.info("      1. Collateral must be set up as Code Values under 'LoanCollateral' code")
        logger.info("      2. Or use the Collateral Management module (if enabled)")
        logger.info("")
        logger.info(f"      Skipping {len(df)} collateral assignments for now.")
        logger.info("      Collateral can be manually added via: Loans → Loan Account → Collateral Tab")
        logger.info("")
        logger.info("      To enable collateral, configure code values:")
        logger.info("      Admin → System → Manage Codes → LoanCollateral → Add Code Value")
        logger.info("")

        # Log the collateral data for reference
        logger.info("Collateral assignments in demo data:")
        for idx, row in df.iterrows():
            logger.info(f"  • {row['loan_account_number']} ({row['client_name']})")
            logger.info(f"    {row['collateral_type']}: {row['collateral_value']:,.0f} XAF")
            logger.info(f"    {row['description']}")

        logger.info("")
        logger.info("✓ Collateral loading skipped (requires manual configuration)")
        logger.info("  View documentation for collateral setup instructions")

    def load_loan_guarantors(self):
        """Assign guarantors to loans from Excel"""
        logger.info("=" * 80)
        logger.info("LOADING LOAN GUARANTOR ASSIGNMENTS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Loan Guarantors')

        for idx, row in df.iterrows():
            try:
                # Find loan account ID
                loan_id = self.client.created_loan_accounts.get(row['loan_account_number'])

                if not loan_id:
                    logger.warning(f"  ⚠ Loan account not found: {row['loan_account_number']}")
                    continue

                # Use External guarantor type (3) for all guarantors
                # This allows standalone guarantors without requiring them to be existing clients
                guarantor_type_id = 3  # External guarantor

                # Parse name into firstname/lastname for individuals
                if row['guarantor_type'] in ['Individual', 'Corporate']:
                    name_parts = row['guarantor_name'].split()
                    firstname = name_parts[0] if name_parts else ''
                    lastname = ' '.join(name_parts[1:]) if len(name_parts) > 1 else row['guarantor_name']
                else:
                    firstname = row['guarantor_name']
                    lastname = ''

                # Create guarantor data
                guarantor_data = {
                    'guarantorTypeId': guarantor_type_id,
                    'firstname': firstname,
                    'lastname': lastname,
                    'addressLine1': row['guarantor_address'],
                    'mobileNumber': row['guarantor_phone'],
                    'amount': float(row['guaranteed_amount']),
                    'locale': 'en'
                }

                # Post guarantor assignment
                response = self.client.post(f'/loans/{loan_id}/guarantors', guarantor_data)
                guarantor_id = response.get('resourceId')

                logger.info(f"  ✓ Guarantor assigned to {row['loan_account_number']} ({row['borrower_name']})")
                logger.info(f"    Guarantor: {row['guarantor_name']} ({row['guarantor_type']})")
                logger.info(f"    Relationship: {row['relationship']} | Amount: {row['guaranteed_amount']:,.0f} XAF")
                if row['guarantor_type'] == 'Individual':
                    logger.info(f"    Employment: {row['employment']} | Income: {row['monthly_income']:,.0f} XAF/month")
                logger.info(f"    Guarantor ID: {guarantor_id}")
                time.sleep(0.3)

            except Exception as e:
                logger.error(f"  ✗ Failed to assign guarantor for {row.get('loan_account_number', 'unknown')}: {str(e)}")

        logger.info(f"\n✓ Completed loading {len(df)} guarantor assignments")
        logger.info("      View: Loans → Loan Account → Guarantors Tab")
