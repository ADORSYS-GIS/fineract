#!/usr/bin/env python3
"""
Excel Template Validation Script
Validates Fineract demo data Excel template before loading
"""

import pandas as pd
import sys
import argparse
from typing import List, Dict, Tuple
from pathlib import Path


class ExcelValidator:
    """Validates Fineract demo data Excel template"""

    def __init__(self, excel_file: str):
        self.excel_file = excel_file
        self.errors = []
        self.warnings = []
        self.sheets = {}

    def validate(self) -> bool:
        """Run all validation checks"""
        print("=" * 80)
        print("VALIDATING FINERACT DEMO DATA EXCEL TEMPLATE")
        print("=" * 80)
        print(f"File: {self.excel_file}\n")

        # Check file exists
        if not Path(self.excel_file).exists():
            self.errors.append(f"File not found: {self.excel_file}")
            return False

        try:
            # Load all sheets
            self._load_sheets()

            # Run validation checks
            self._validate_maker_checker_config()
            self._validate_loan_accounts()
            self._validate_roles_permissions()
            self._validate_external_ids()
            self._validate_references()

            # Print results
            self._print_results()

            return len(self.errors) == 0

        except Exception as e:
            self.errors.append(f"Validation failed: {str(e)}")
            self._print_results()
            return False

    def _load_sheets(self):
        """Load required sheets from Excel"""
        required_sheets = [
            'Maker Checker Config',
            'Loan Accounts',
            'Roles Permissions',
            'Offices',
            'Staff',
            'Clients',
            'Loan Products',
            'Savings Products'
        ]

        for sheet_name in required_sheets:
            try:
                self.sheets[sheet_name] = pd.read_excel(self.excel_file, sheet_name=sheet_name)
                print(f"✓ Loaded sheet: {sheet_name}")
            except Exception as e:
                self.errors.append(f"Could not load sheet '{sheet_name}': {str(e)}")

        print()

    def _validate_maker_checker_config(self):
        """Validate Maker Checker Config sheet"""
        print("Validating Maker Checker Config sheet...")

        if 'Maker Checker Config' not in self.sheets:
            return

        df = self.sheets['Maker Checker Config']

        # Check required columns exist
        required_columns = ['task_name', 'entity', 'action', 'enabled', 'maker_role', 'checker_role', 'description']
        for col in required_columns:
            if col not in df.columns:
                self.errors.append(f"Maker Checker Config: Missing required column '{col}'")

        # Check removed columns don't exist
        forbidden_columns = ['threshold_amount', 'threshold_currency']
        for col in forbidden_columns:
            if col in df.columns:
                self.errors.append(f"Maker Checker Config: Column '{col}' should be removed (not supported by Fineract)")

        # Validate enabled column values
        if 'enabled' in df.columns:
            invalid_enabled = df[~df['enabled'].isin([True, False, 'True', 'False'])]
            if len(invalid_enabled) > 0:
                self.errors.append(f"Maker Checker Config: {len(invalid_enabled)} rows have invalid 'enabled' values (must be True/False)")

        # Validate entity values
        if 'entity' in df.columns:
            valid_entities = ['Loan', 'SAVINGSACCOUNT', 'Client', 'JOURNALENTRY', 'User', 'RESCHEDULELOAN', 'OFFICETRANSACTION']
            invalid_entities = df[~df['entity'].isin(valid_entities)]
            if len(invalid_entities) > 0:
                self.warnings.append(f"Maker Checker Config: {len(invalid_entities)} rows have non-standard entity values")

        # Validate action values
        if 'action' in df.columns:
            valid_actions = ['APPROVE', 'DISBURSE', 'WRITEOFF', 'CREATE', 'ACTIVATE', 'UPDATE', 'CLOSE', 'PROPOSETRANSFER']
            invalid_actions = df[~df['action'].isin(valid_actions)]
            if len(invalid_actions) > 0:
                self.warnings.append(f"Maker Checker Config: {len(invalid_actions)} rows have non-standard action values")

        # Check that enabled permissions have valid roles
        if 'enabled' in df.columns and 'maker_role' in df.columns:
            enabled_df = df[df['enabled'] == True]
            missing_maker_role = enabled_df[enabled_df['maker_role'].isna()]
            if len(missing_maker_role) > 0:
                self.errors.append(f"Maker Checker Config: {len(missing_maker_role)} enabled permissions have missing maker_role")

            missing_checker_role = enabled_df[enabled_df['checker_role'].isna()]
            if len(missing_checker_role) > 0:
                self.errors.append(f"Maker Checker Config: {len(missing_checker_role)} enabled permissions have missing checker_role")

        print(f"  ✓ Validated {len(df)} maker-checker configurations\n")

    def _validate_loan_accounts(self):
        """Validate Loan Accounts sheet"""
        print("Validating Loan Accounts sheet...")

        if 'Loan Accounts' not in self.sheets:
            return

        df = self.sheets['Loan Accounts']

        # Check workflow_state column exists
        if 'workflow_state' not in df.columns:
            self.errors.append("Loan Accounts: Missing 'workflow_state' column")
            return

        # Validate workflow_state values
        valid_states = ['active', 'pending_approval', 'pending_disbursal']
        invalid_states = df[~df['workflow_state'].isin(valid_states)]
        if len(invalid_states) > 0:
            self.errors.append(f"Loan Accounts: {len(invalid_states)} rows have invalid workflow_state values")
            for idx, row in invalid_states.iterrows():
                self.errors.append(f"  → Row {idx+2}: '{row['workflow_state']}' (should be: active, pending_approval, or pending_disbursal)")

        # Count workflow states
        state_counts = df['workflow_state'].value_counts()
        print(f"  Workflow state distribution:")
        for state, count in state_counts.items():
            print(f"    - {state}: {count}")

        # Check that at least one loan demonstrates maker-checker
        pending_count = len(df[df['workflow_state'].isin(['pending_approval', 'pending_disbursal'])])
        if pending_count == 0:
            self.warnings.append("Loan Accounts: No loans in pending state - consider adding 1-2 for demonstration")

        print(f"  ✓ Validated {len(df)} loan accounts\n")

    def _validate_roles_permissions(self):
        """Validate Roles Permissions sheet"""
        print("Validating Roles Permissions sheet...")

        if 'Roles Permissions' not in self.sheets or 'Maker Checker Config' not in self.sheets:
            return

        roles_df = self.sheets['Roles Permissions']
        mc_df = self.sheets['Maker Checker Config']

        # Get unique roles from both sheets
        roles_from_sheet = set(roles_df['role_name'].unique())

        # Get roles referenced in maker-checker config
        enabled_mc = mc_df[mc_df['enabled'] == True]
        maker_roles = set(enabled_mc['maker_role'].dropna().unique())
        checker_roles = set(enabled_mc['checker_role'].dropna().unique())
        mc_roles = maker_roles | checker_roles

        # Check if all maker-checker roles exist in Roles Permissions sheet
        missing_roles = mc_roles - roles_from_sheet
        if missing_roles:
            self.warnings.append(f"Roles Permissions: {len(missing_roles)} roles referenced in Maker Checker Config but not defined:")
            for role in missing_roles:
                self.warnings.append(f"  → '{role}' will be auto-created by loader")

        print(f"  ✓ Validated {len(roles_df)} role permission mappings")
        print(f"  ✓ {len(mc_roles)} roles referenced in maker-checker config\n")

    def _validate_external_ids(self):
        """Validate external ID uniqueness"""
        print("Validating external IDs...")

        sheets_with_external_ids = {
            'Offices': 'external_id',
            'Staff': 'external_id',
            'Clients': 'external_id',
            'Loan Accounts': 'external_id',
            'Savings Accounts': 'external_id'
        }

        for sheet_name, col_name in sheets_with_external_ids.items():
            if sheet_name not in self.sheets:
                continue

            df = self.sheets[sheet_name]
            if col_name not in df.columns:
                self.warnings.append(f"{sheet_name}: Missing '{col_name}' column")
                continue

            # Check for duplicates
            duplicates = df[df[col_name].duplicated(keep=False)]
            if len(duplicates) > 0:
                self.errors.append(f"{sheet_name}: {len(duplicates)} duplicate external IDs found")
                unique_dupes = duplicates[col_name].unique()
                for dup_id in unique_dupes[:5]:  # Show first 5
                    self.errors.append(f"  → Duplicate: '{dup_id}'")

            # Check for null values
            nulls = df[df[col_name].isna()]
            if len(nulls) > 0:
                self.warnings.append(f"{sheet_name}: {len(nulls)} rows have null external_id")

        print(f"  ✓ Validated external ID uniqueness\n")

    def _validate_references(self):
        """Validate references between sheets"""
        print("Validating cross-sheet references...")

        # Validate loan accounts reference clients
        if 'Loan Accounts' in self.sheets and 'Clients' in self.sheets:
            loans_df = self.sheets['Loan Accounts']
            clients_df = self.sheets['Clients']

            if 'client_external_id' in loans_df.columns and 'external_id' in clients_df.columns:
                client_ids = set(clients_df['external_id'].dropna())
                loan_client_refs = loans_df['client_external_id'].dropna()

                invalid_refs = loan_client_refs[~loan_client_refs.isin(client_ids)]
                if len(invalid_refs) > 0:
                    self.errors.append(f"Loan Accounts: {len(invalid_refs)} loans reference non-existent clients")

        # Validate loan accounts reference products
        if 'Loan Accounts' in self.sheets and 'Loan Products' in self.sheets:
            loans_df = self.sheets['Loan Accounts']
            products_df = self.sheets['Loan Products']

            if 'product' in loans_df.columns and 'short_name' in products_df.columns:
                product_names = set(products_df['short_name'].dropna())
                loan_product_refs = loans_df['product'].dropna()

                invalid_refs = loan_product_refs[~loan_product_refs.isin(product_names)]
                if len(invalid_refs) > 0:
                    self.errors.append(f"Loan Accounts: {len(invalid_refs)} loans reference non-existent products")

        # Validate staff references
        if 'Loan Accounts' in self.sheets and 'Staff' in self.sheets:
            loans_df = self.sheets['Loan Accounts']
            staff_df = self.sheets['Staff']

            if 'loan_officer' in loans_df.columns and 'username' in staff_df.columns:
                staff_usernames = set(staff_df['username'].dropna())
                loan_officer_refs = loans_df['loan_officer'].dropna()

                invalid_refs = loan_officer_refs[~loan_officer_refs.isin(staff_usernames)]
                if len(invalid_refs) > 0:
                    self.errors.append(f"Loan Accounts: {len(invalid_refs)} loans reference non-existent loan officers")

        print(f"  ✓ Validated cross-sheet references\n")

    def _print_results(self):
        """Print validation results"""
        print("=" * 80)
        print("VALIDATION RESULTS")
        print("=" * 80)

        if self.warnings:
            print(f"\n⚠️  {len(self.warnings)} WARNING(S):")
            for warning in self.warnings:
                print(f"  {warning}")

        if self.errors:
            print(f"\n❌ {len(self.errors)} ERROR(S):")
            for error in self.errors:
                print(f"  {error}")
            print("\n❌ VALIDATION FAILED - Please fix errors before loading")
        else:
            print("\n✅ VALIDATION PASSED")
            if self.warnings:
                print("   (Some warnings - review before loading)")
            else:
                print("   (No errors or warnings)")

        print("=" * 80)


def main():
    parser = argparse.ArgumentParser(
        description='Validate Fineract demo data Excel template',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python validate_excel.py ../output/fineract_demo_data_20240115.xlsx
  python validate_excel.py --file my_custom_data.xlsx

Exit codes:
  0 - Validation passed
  1 - Validation failed (errors found)
  2 - File not found or other error
        """
    )

    parser.add_argument(
        'excel_file',
        nargs='?',
        help='Path to Excel file to validate'
    )

    parser.add_argument(
        '--file', '-f',
        dest='excel_file_alt',
        help='Alternative way to specify Excel file'
    )

    args = parser.parse_args()

    # Get excel file from either positional or named argument
    excel_file = args.excel_file or args.excel_file_alt

    if not excel_file:
        parser.print_help()
        print("\nError: Please specify an Excel file to validate")
        sys.exit(2)

    # Run validation
    validator = ExcelValidator(excel_file)
    success = validator.validate()

    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
