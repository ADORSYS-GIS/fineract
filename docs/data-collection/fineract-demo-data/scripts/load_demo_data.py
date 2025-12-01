#!/usr/bin/env python3
"""
Fineract Demo Data Loader (Refactored)
Reads Excel template and loads data via Fineract APIs
"""

import json
import time
import logging
import sys
import os
from datetime import datetime

from fineract_client import FineractAPIClient
from loaders.system_config import SystemConfigLoader
from loaders.entities import EntityLoader
from loaders.products import ProductLoader
from loaders.accounts import AccountLoader
from loaders.roles_permissions import RolesPermissionsLoader
from loaders.reports import ReportsLoader

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


class FineractDemoDataLoader:
    """Main class for loading demo data into Fineract"""

    def __init__(self, excel_file: str, config_file: str):
        self.excel_file = excel_file
        self.config_file = config_file
        self.config = self._load_config()

        # Initialize API client
        self.client = FineractAPIClient(
            base_url=self.config['fineract_url'],
            username=self.config['username'],
            password=self.config['password'],
            tenant=self.config.get('tenant', 'default')
        )

        # Initialize loaders
        self.system_config_loader = SystemConfigLoader(self.client, excel_file, self.config)
        self.entity_loader = EntityLoader(self.client, excel_file, self.config)
        self.product_loader = ProductLoader(self.client, excel_file, self.config)
        self.account_loader = AccountLoader(self.client, excel_file, self.config)
        self.roles_permissions_loader = RolesPermissionsLoader(self.client, excel_file, self.config)
        self.reports_loader = ReportsLoader(self.client, excel_file, self.config)

    def _load_config(self):
        """Load configuration from JSON file"""
        with open(self.config_file, 'r') as f:
            return json.load(f)

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
            self.system_config_loader.load_currency_config()
            self.system_config_loader.load_working_days()
            self.system_config_loader.load_global_configuration()
            self.system_config_loader.load_codes_and_values()
            self.system_config_loader.load_account_number_preferences()
            self.system_config_loader.load_sms_email_config()
            self.system_config_loader.load_notification_templates()
            self.system_config_loader.load_data_tables()

            # Load roles and permissions FIRST (before staff so users can be assigned to custom roles)
            self.roles_permissions_loader.load_roles_permissions()

            # Load entities
            self.entity_loader.load_offices()
            self.entity_loader.load_staff()
            self.entity_loader.load_tellers()

            # Load products and configurations
            self.product_loader.load_gl_accounts()
            #TODO: self.product_loader.load_floating_rates()  # After GL accounts, before loan products
            #TODO:  self.product_loader.load_tax_groups()      # After GL accounts, before savings products
            self.product_loader.load_charges()
            self.product_loader.load_fund_sources()
            self.product_loader.load_payment_types()
            self.entity_loader.load_holidays()
            self.product_loader.load_financial_activity_mappings()
            self.product_loader.load_teller_accounting_rules()
            self.product_loader.enable_maker_checker()
            self.system_config_loader.load_scheduler_jobs()
            self.product_loader.load_loan_products()
            self.product_loader.load_delinquency_buckets()  # After loan products
            self.product_loader.load_savings_products()
            self.product_loader.load_loan_provisioning()
            self.product_loader.load_collateral_types()

            # Load accounts
            self.entity_loader.load_clients()
            self.account_loader.load_savings_accounts()
            self.account_loader.load_loan_accounts()

            # Assign collateral and guarantors to loans
            self.account_loader.load_loan_collateral()
            self.account_loader.load_loan_guarantors()

            # Load transactions
            self.account_loader.load_savings_deposits()
            self.account_loader.load_savings_withdrawals()
            self.account_loader.load_loan_repayments()
            self.account_loader.load_inter_branch_transfers()

            # Note: Reports are skipped - they are just views of existing data
            # Users can generate reports on-demand from Fineract UI: Reports → Run Reports

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
        print("Usage: python load_demo_data_new.py <excel_file> [config_file]")
        print("\nExample:")
        print("  python load_demo_data_new.py ../output/fineract_demo_data_20241010.xlsx ../config/fineract_config.json")
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
