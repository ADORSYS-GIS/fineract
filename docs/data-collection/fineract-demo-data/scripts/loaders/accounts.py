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
