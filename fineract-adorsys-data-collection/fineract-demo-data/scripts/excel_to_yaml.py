#!/usr/bin/env python3
"""
Excel to YAML Converter for Fineract Config CLI
Converts Fineract demo data Excel template (36 sheets) to YAML format
"""

import argparse
import logging
import sys
import os
from datetime import datetime
from typing import Dict, List, Any, Optional
import pandas as pd
import yaml

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)


class ExcelToYamlConverter:
    """Converts Fineract Excel template to YAML configuration"""

    def __init__(self, excel_file: str):
        self.excel_file = excel_file
        self.config = {'tenant': 'default'}

    def _read_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read Excel sheet and handle NaN values"""
        try:
            df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
            # Replace NaN with None for cleaner YAML
            df = df.where(pd.notnull(df), None)
            return df
        except Exception as e:
            logger.warning(f"Sheet '{sheet_name}' not found or empty: {str(e)}")
            return pd.DataFrame()

    def _parse_date(self, date_value) -> Optional[List[int]]:
        """Convert Excel date to [year, month, day] format"""
        if date_value is None or pd.isna(date_value):
            return None

        try:
            if isinstance(date_value, str):
                dt = pd.to_datetime(date_value)
            else:
                dt = pd.to_datetime(date_value)
            return [dt.year, dt.month, dt.day]
        except:
            logger.warning(f"Could not parse date: {date_value}")
            return None

    def _parse_boolean(self, value) -> bool:
        """Convert various boolean representations to Python bool"""
        if value is None or pd.isna(value):
            return False
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.lower() in ['yes', 'true', '1', 'y']
        return bool(value)

    def convert_system_config(self):
        """Convert Phase 1: System Configuration sheets"""
        logger.info("Converting Phase 1: System Configuration...")
        system_config = {}

        # 1. Currency Configuration
        df = self._read_sheet('Currency Config')
        if not df.empty and len(df) > 0:
            row = df.iloc[0]
            system_config['currency'] = {
                'code': row['currency_code'],
                'name': row['currency_name'],
                'decimalPlaces': int(row['decimal_places']) if row['decimal_places'] else 2
            }
            logger.info(f"  ✓ Currency: {row['currency_code']}")

        # 2. Working Days
        df = self._read_sheet('Working Days')
        if not df.empty:
            working_days = []
            day_map = {
                'Monday': 'MO', 'Tuesday': 'TU', 'Wednesday': 'WE',
                'Thursday': 'TH', 'Friday': 'FR', 'Saturday': 'SA', 'Sunday': 'SU'
            }
            for _, row in df.iterrows():
                if self._parse_boolean(row['working_day']):
                    working_days.append(day_map.get(row['day_of_week'], ''))

            system_config['workingDays'] = {
                'recurrence': f"FREQ=WEEKLY;BYDAY={','.join(working_days)}",
                'repaymentReschedulingType': 'MOVE_TO_NEXT_WORKING_DAY',
                'extendTermForDailyRepayments': False
            }
            logger.info(f"  ✓ Working Days: {','.join(working_days)}")

        # 3. Global Configuration
        df = self._read_sheet('Global Configuration')
        if not df.empty:
            global_config = []
            for _, row in df.iterrows():
                config_item = {
                    'name': row['config_name'],
                    'enabled': self._parse_boolean(row['enabled'])
                }
                # Handle value based on value_type
                value_type = row.get('value_type', 'boolean') if 'value_type' in row else 'boolean'
                if pd.isna(value_type):
                    value_type = 'boolean'

                if value_type == 'numeric':
                    # Numeric configs need a Long value
                    if 'value' in row and row['value'] is not None and not pd.isna(row['value']):
                        config_item['value'] = int(float(row['value']))
                elif value_type == 'string':
                    # String configs use stringValue
                    if 'value' in row and row['value'] is not None and not pd.isna(row['value']):
                        config_item['stringValue'] = str(row['value'])
                elif value_type == 'date':
                    # Date configs use dateValue
                    if 'value' in row and row['value'] is not None and not pd.isna(row['value']):
                        config_item['dateValue'] = str(row['value'])
                # For 'boolean' type, we only use enabled field - no value field needed
                # This is the default behavior for toggle configs

                global_config.append(config_item)
            system_config['globalConfig'] = global_config
            logger.info(f"  ✓ Global Config: {len(global_config)} configurations")

        # 4. Codes and Code Values (combined in one sheet)
        df = self._read_sheet('Codes and Values')
        if not df.empty:
            # Group by code name
            codes_dict = {}
            for _, row in df.iterrows():
                code_name = row.get('code_name')
                if not code_name or pd.isna(code_name):
                    continue

                if code_name not in codes_dict:
                    codes_dict[code_name] = {'name': code_name, 'values': []}

                # Add value if it exists
                value_name = row.get('code_value') or row.get('value_name') or row.get('code_value_name')
                if value_name and not pd.isna(value_name):
                    # Get position from code_position or position column
                    position_value = row.get('code_position') or row.get('position')
                    position = int(position_value) if position_value and not pd.isna(position_value) else 1

                    codes_dict[code_name]['values'].append({
                        'name': value_name,
                        'position': position,
                        'isActive': self._parse_boolean(row.get('is_active', True))
                    })

            system_config['codes'] = list(codes_dict.values())
            logger.info(f"  ✓ Codes: {len(codes_dict)} code types")

        # 5. Account Number Preferences
        df = self._read_sheet('Account Number Preferences')
        if not df.empty:
            preferences = []
            for _, row in df.iterrows():
                # Use entity_type or account_type
                entity_type = row.get('entity_type') or row.get('account_type')
                if entity_type and not pd.isna(entity_type):
                    # Use prefix_type_id if available, otherwise fall back to mapping
                    prefix_type_id = row.get('prefix_type_id')
                    if prefix_type_id and not pd.isna(prefix_type_id):
                        prefix_type_num = int(prefix_type_id)
                    else:
                        # Legacy mapping for backwards compatibility
                        prefix_type = row.get('prefix_type')
                        prefix_type_map = {
                            'Office Name': 1,
                            'Office Short Name': 1,  # fallback
                            'Client Type': 101,
                            'Loan Product Short Name': 201,
                            'Savings Product Short Name': 301,
                            'Custom Prefix': 401,
                        }
                        prefix_type_num = 1  # Default to Office Name
                        if prefix_type and not pd.isna(prefix_type):
                            prefix_type_num = prefix_type_map.get(prefix_type, 1)

                    preferences.append({
                        'accountType': entity_type.upper(),
                        'prefixType': prefix_type_num
                    })
            if preferences:
                system_config['accountNumberPreferences'] = preferences
                logger.info(f"  ✓ Account Number Preferences: {len(preferences)} types")

        # 6. Notification Templates (optional - skip if not formatted correctly)
        df = self._read_sheet('Notification Templates')
        if not df.empty and 'template_name' in df.columns:
            try:
                templates = []
                for _, row in df.iterrows():
                    template = {}
                    if row.get('template_name'):
                        template['name'] = row['template_name']

                    # Channel (SMS/Email)
                    if row.get('channel'):
                        template['channel'] = row['channel']

                    # Event trigger
                    if row.get('event_trigger'):
                        template['eventTrigger'] = row['event_trigger']

                    # Type/Entity fallbacks for backward compatibility
                    if row.get('type') or row.get('template_type'):
                        template['type'] = row.get('type') or row.get('template_type')
                    if row.get('entity') or row.get('entity_type'):
                        template['entity'] = row.get('entity') or row.get('entity_type')

                    # Subject (for email templates)
                    subject = row.get('subject')
                    if subject and not pd.isna(subject):
                        template['subject'] = str(subject)

                    # Message body - check multiple possible column names
                    body = row.get('message_body') or row.get('body') or row.get('content')
                    if body and not pd.isna(body):
                        template['messageBody'] = str(body)

                    # Active status
                    if row.get('is_active'):
                        template['isActive'] = self._parse_boolean(row['is_active'])

                    if template.get('name'):
                        templates.append(template)

                if templates:
                    system_config['notificationTemplates'] = templates
                    logger.info(f"  ✓ Notification Templates: {len(templates)} templates")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Notification Templates: {str(e)}")

        # 7. Data Tables (optional)
        df = self._read_sheet('Data Tables')
        if not df.empty and 'table_name' in df.columns:
            try:
                # Group by table name since each row is a field/column
                tables_dict = {}
                for _, row in df.iterrows():
                    table_name = row.get('table_name')
                    if not table_name or pd.isna(table_name):
                        continue

                    # Create table entry if it doesn't exist
                    if table_name not in tables_dict:
                        # Map entity_type to appTableName
                        entity_type = row.get('entity_type', 'm_client')
                        entity_map = {
                            'Client': 'm_client',
                            'm_client': 'm_client',
                            'Loan': 'm_loan',
                            'm_loan': 'm_loan',
                            'Savings': 'm_savings_account',
                            'm_savings_account': 'm_savings_account',
                            'Group': 'm_group',
                            'm_group': 'm_group',
                            'Center': 'm_center',
                            'm_center': 'm_center',
                            'Office': 'm_office',
                            'm_office': 'm_office'
                        }
                        app_table_name = entity_map.get(entity_type, 'm_client')

                        table_entry = {
                            'name': table_name,
                            'appTableName': app_table_name,
                            'multiRow': False,  # Default to False
                            'columns': []
                        }

                        # Add entitySubType for m_client tables (required by Fineract API)
                        entity_sub_type = row.get('entity_sub_type')
                        if entity_sub_type and not pd.isna(entity_sub_type):
                            table_entry['entitySubType'] = str(entity_sub_type).upper()
                        elif app_table_name == 'm_client':
                            # Default to PERSON for client tables
                            table_entry['entitySubType'] = 'PERSON'

                        tables_dict[table_name] = table_entry

                    # Add column/field definition
                    field_name = row.get('field_name')
                    if field_name and not pd.isna(field_name):
                        column = {
                            'name': field_name,
                            'type': row.get('field_type', 'String')
                        }

                        # Add mandatory flag
                        if row.get('mandatory'):
                            column['mandatory'] = self._parse_boolean(row['mandatory'])

                        # Add dropdown values if present
                        dropdown_values = row.get('dropdown_values')
                        if dropdown_values and not pd.isna(dropdown_values):
                            # Split comma-separated values
                            column['values'] = [v.strip() for v in str(dropdown_values).split(',')]

                        # Add length for String types if available
                        if row.get('length') and not pd.isna(row.get('length')):
                            column['length'] = int(row['length'])

                        tables_dict[table_name]['columns'].append(column)

                if tables_dict:
                    system_config['dataTables'] = list(tables_dict.values())
                    total_columns = sum(len(t['columns']) for t in tables_dict.values())
                    logger.info(f"  ✓ Data Tables: {len(tables_dict)} tables with {total_columns} columns")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Data Tables: {str(e)}")

        # 8. Scheduler Jobs
        df = self._read_sheet('Scheduler Jobs')
        if not df.empty:
            try:
                scheduler_jobs = []
                for _, row in df.iterrows():
                    if not row.get('job_name') or pd.isna(row.get('job_name')):
                        continue

                    job = {
                        'name': row['job_name']
                    }

                    # Display name
                    if row.get('display_name') and not pd.isna(row.get('display_name')):
                        job['displayName'] = row['display_name']

                    # Cron expression
                    if row.get('cron_expression') and not pd.isna(row.get('cron_expression')):
                        job['cronExpression'] = row['cron_expression']

                    # Active status
                    if row.get('active') and not pd.isna(row.get('active')):
                        job['active'] = self._parse_boolean(row['active'])


                    # Job parameters (if any)
                    if row.get('job_parameters') and not pd.isna(row.get('job_parameters')):
                        job['jobParameters'] = row['job_parameters']

                    scheduler_jobs.append(job)

                if scheduler_jobs:
                    system_config['schedulerJobs'] = scheduler_jobs
                    logger.info(f"  ✓ Scheduler Jobs: {len(scheduler_jobs)} jobs")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Scheduler Jobs: {str(e)}")

        # 9. Maker-Checker Configuration
        df = self._read_sheet('Maker Checker Config')
        if not df.empty:
            try:
                maker_checker = []
                for _, row in df.iterrows():
                    if not row.get('entity') or pd.isna(row.get('entity')):
                        continue

                    config = {
                        'entity': row['entity']
                    }

                    # Action
                    if row.get('action') and not pd.isna(row.get('action')):
                        config['action'] = row['action']

                    # Enabled status
                    if row.get('maker_checker_enabled'):
                        config['enabled'] = self._parse_boolean(row['maker_checker_enabled'])

                    maker_checker.append(config)

                if maker_checker:
                    system_config['makerCheckerConfig'] = maker_checker
                    logger.info(f"  ✓ Maker-Checker Config: {len(maker_checker)} rules")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Maker-Checker Config: {str(e)}")

        # 10. SMS/Email Configuration
        df = self._read_sheet('SMS Email Config')
        if not df.empty:
            try:
                sms_email_config = []
                for _, row in df.iterrows():
                    # Use config_key as the main identifier
                    config_key = row.get('config_key')
                    if not config_key or pd.isna(config_key):
                        continue

                    config = {
                        'name': str(config_key)
                    }

                    # Config type and provider
                    if row.get('config_type') and not pd.isna(row.get('config_type')):
                        config['type'] = row['config_type']
                    if row.get('provider') and not pd.isna(row.get('provider')):
                        config['provider'] = row['provider']

                    # Config value
                    if row.get('config_value') and not pd.isna(row.get('config_value')):
                        config['value'] = str(row['config_value'])

                    # Is active
                    if row.get('is_active') and not pd.isna(row.get('is_active')):
                        config['isActive'] = self._parse_boolean(row['is_active'])

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        config['description'] = row['description']

                    sms_email_config.append(config)

                if sms_email_config:
                    system_config['smsEmailConfig'] = sms_email_config
                    logger.info(f"  ✓ SMS/Email Config: {len(sms_email_config)} settings")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping SMS/Email Config: {str(e)}")

        # 11. Holidays
        df = self._read_sheet('Holidays')
        if not df.empty:
            try:
                holidays = []
                for _, row in df.iterrows():
                    if not row.get('holiday_name') or pd.isna(row.get('holiday_name')):
                        continue

                    holiday = {
                        'name': row['holiday_name']
                    }

                    # Dates - support both formats (from_date/to_date and date)
                    if row.get('from_date'):
                        holiday['fromDate'] = self._parse_date(row['from_date'])
                    elif row.get('date'):
                        # Use single 'date' column for both from and to
                        holiday['fromDate'] = self._parse_date(row['date'])
                        holiday['toDate'] = self._parse_date(row['date'])

                    if row.get('to_date'):
                        holiday['toDate'] = self._parse_date(row['to_date'])

                    # Rescheduling - support both formats
                    if row.get('repayments_rescheduled_to'):
                        holiday['repaymentsRescheduledTo'] = self._parse_date(row['repayments_rescheduled_to'])
                    elif row.get('rescheduled_to'):
                        holiday['repaymentsRescheduledTo'] = self._parse_date(row['rescheduled_to'])

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        holiday['description'] = row['description']

                    # Offices (comma-separated list) - default to all offices if not specified
                    if row.get('offices') and not pd.isna(row.get('offices')):
                        offices_str = str(row['offices'])
                        holiday['officeNames'] = [o.strip() for o in offices_str.split(',')]

                    holidays.append(holiday)

                if holidays:
                    system_config['holidays'] = holidays
                    logger.info(f"  ✓ Holidays: {len(holidays)} holidays")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Holidays: {str(e)}")

        self.config['systemConfig'] = system_config

    def convert_security_organization(self):
        """Convert Phase 2: Security & Organization"""
        logger.info("Converting Phase 2: Security & Organization...")

        # 1. Offices
        df = self._read_sheet('Offices')
        if not df.empty:
            offices = []
            for _, row in df.iterrows():
                office = {
                    'name': row['office_name'],
                    'openingDate': self._parse_date(row['opening_date'])
                }
                if row.get('external_id'):
                    office['externalId'] = row['external_id']
                if row.get('parent_office'):
                    office['parentName'] = row['parent_office']
                offices.append(office)

            self.config['offices'] = offices
            logger.info(f"  ✓ Offices: {len(offices)} offices")

        # 2. Roles (from Roles Permissions sheet)
        df = self._read_sheet('Roles Permissions')
        if not df.empty:
            # Group by role name
            roles_dict = {}
            for _, row in df.iterrows():
                role_name = row.get('role_name')
                if not role_name or pd.isna(role_name):
                    continue
                role_name = str(role_name).strip()

                if role_name not in roles_dict:
                    roles_dict[role_name] = {
                        'name': role_name,
                        'description': row.get('description', f'{role_name} role'),
                        'disabled': False,
                        'permissions': []
                    }

                # Add permission
                permission = row.get('permission_code') or row.get('permission')
                group = row.get('permission_group')
                
                if permission and not pd.isna(permission):
                    permission = str(permission).strip()
                    if group and not pd.isna(group):
                         group = str(group).strip()
                    from permission_mappings import expand_permissions
                    expanded_perms = expand_permissions(group, permission)
                    roles_dict[role_name]['permissions'].extend(expanded_perms)

            self.config['roles'] = list(roles_dict.values())
            logger.info(f"  ✓ Roles: {len(roles_dict)} roles")

        # 3. Staff
        df = self._read_sheet('Staff')
        if not df.empty:
            staff = []
            for _, row in df.iterrows():
                staff_member = {
                    'firstName': row.get('firstname') or row.get('first_name'),
                    'lastName': row.get('lastname') or row.get('last_name'),
                    'officeName': row.get('office') or row.get('office_name'),
                    'isLoanOfficer': True,  # Default to true
                    'isActive': True  # Default to true
                }
                if row.get('external_id'):
                    staff_member['externalId'] = row['external_id']
                if row.get('mobile'):
                    staff_member['mobileNo'] = row['mobile']
                if row.get('email'):
                    staff_member['emailAddress'] = row['email']
                if row.get('joining_date'):
                    staff_member['joiningDate'] = self._parse_date(row['joining_date'])

                staff.append(staff_member)

            self.config['staff'] = staff
            logger.info(f"  ✓ Staff: {len(staff)} members")

        # 4. Users
        df = self._read_sheet('Users')
        if not df.empty:
            users = []
            for _, row in df.iterrows():
                user = {
                    'username': row['username'],
                    'firstname': row['first_name'],  # lowercase to match User.java model
                    'lastname': row['last_name'],    # lowercase to match User.java model
                    'email': row['email'],
                    'officeName': row['office_name'],
                    'roles': [r.strip() for r in row['roles'].split(',')] if row.get('roles') else []
                }
                # Password is required for user creation
                if row.get('password'):
                    user['password'] = row['password']
                if row.get('staff_name'):
                    user['staffName'] = row['staff_name']
                if row.get('password_never_expires'):
                    user['passwordNeverExpires'] = self._parse_boolean(row['password_never_expires'])

                users.append(user)

            self.config['users'] = users
            logger.info(f"  ✓ Users: {len(users)} users")

    def convert_accounting(self):
        """Convert Phase 3: Accounting Foundation"""
        logger.info("Converting Phase 3: Accounting Foundation...")

        # 1. Chart of Accounts
        df = self._read_sheet('Chart of Accounts')
        if not df.empty:
            accounts = []
            for _, row in df.iterrows():
                account = {
                    'name': row.get('gl_name') or row.get('account_name'),
                    'glCode': row['gl_code'],
                    'accountType': row.get('account_type') or row.get('classification'),
                    'accountUsage': row.get('usage') or row.get('account_usage', 'DETAIL'),
                    'manualEntriesAllowed': self._parse_boolean(row.get('manual_entries', True))
                }
                if row.get('parent_gl_code'):
                    account['parentGlCode'] = row['parent_gl_code']
                if row.get('description'):
                    account['description'] = row['description']
                if row.get('tag_name'):
                    account['tagName'] = row['tag_name']

                accounts.append(account)

            self.config['chartOfAccounts'] = accounts
            logger.info(f"  ✓ Chart of Accounts: {len(accounts)} accounts")

        # 2. Payment Types
        df = self._read_sheet('Payment Types')
        if not df.empty:
            payment_types = []
            for _, row in df.iterrows():
                payment_types.append({
                    'name': row.get('payment_type') or row.get('payment_type_name'),
                    'description': row.get('description', ''),
                    'isCashPayment': self._parse_boolean(row.get('is_cash_payment', False)),
                    'position': int(row.get('order_position') or row.get('position', 1))
                })

            self.config['paymentTypes'] = payment_types
            logger.info(f"  ✓ Payment Types: {len(payment_types)} types")

        # 3. Fund Sources
        df = self._read_sheet('Fund Sources')
        if not df.empty:
            fund_sources = []
            for _, row in df.iterrows():
                fund_sources.append({
                    'name': row['fund_name'],
                    'externalId': row.get('external_id')
                })

            self.config['fundSources'] = fund_sources
            logger.info(f"  ✓ Fund Sources: {len(fund_sources)} sources")

        # 4. Financial Activity Mapping (singular sheet name)
        # Note: This sheet has activity names, not IDs. The CLI will need to resolve these.
        df = self._read_sheet('Financial Activity Mapping')
        if not df.empty:
            mappings = []
            for _, row in df.iterrows():
                # Activity name (needs to be resolved to ID by CLI)
                activity_name = row.get('financial_activity') or row.get('financial_activity_name')
                gl_code = row.get('gl_code') or row.get('gl_account_code')

                if activity_name and not pd.isna(activity_name) and gl_code and not pd.isna(gl_code):
                    mappings.append({
                        'financialActivityName': str(activity_name),  # Using name instead of ID
                        'glAccountCode': str(gl_code)
                    })

            if mappings:
                self.config['financialActivityMappings'] = mappings
                logger.info(f"  ✓ Financial Activity Mappings: {len(mappings)} mappings")

        # 5. Tax Groups
        df = self._read_sheet('Tax Groups')
        if not df.empty:
            try:
                # Group by tax_group_name to create hierarchical structure
                tax_groups_dict = {}
                for _, row in df.iterrows():
                    group_name = row.get('tax_group_name')
                    if not group_name or pd.isna(group_name):
                        continue

                    # Initialize group if not exists
                    if group_name not in tax_groups_dict:
                        tax_groups_dict[group_name] = {
                            'name': group_name,
                            'taxComponents': []
                        }

                    # Add tax component
                    component_name = row.get('tax_component_name') or row.get('component_name')
                    if component_name and not pd.isna(component_name):
                        component = {
                            'name': component_name
                        }

                        # Percentage
                        if row.get('percentage') and not pd.isna(row.get('percentage')):
                            component['percentage'] = float(row['percentage'])

                        # Start date
                        if row.get('start_date'):
                            component['startDate'] = self._parse_date(row['start_date'])

                        # Credit account GL code
                        if row.get('credit_account_type') and not pd.isna(row.get('credit_account_type')):
                            component['creditAccountType'] = row['credit_account_type']

                        tax_groups_dict[group_name]['taxComponents'].append(component)

                if tax_groups_dict:
                    self.config['taxGroups'] = list(tax_groups_dict.values())
                    total_components = sum(len(g['taxComponents']) for g in tax_groups_dict.values())
                    logger.info(f"  ✓ Tax Groups: {len(tax_groups_dict)} groups, {total_components} components")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Tax Groups: {str(e)}")

        # 6. Tellers
        df = self._read_sheet('Tellers')
        if not df.empty:
            try:
                tellers = []
                for _, row in df.iterrows():
                    if not row.get('teller_name') or pd.isna(row.get('teller_name')):
                        continue

                    teller = {
                        'name': row['teller_name']
                    }

                    # Office
                    if row.get('office_name') and not pd.isna(row.get('office_name')):
                        teller['officeName'] = row['office_name']

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        teller['description'] = row['description']

                    # Start/End date
                    if row.get('start_date'):
                        teller['startDate'] = self._parse_date(row['start_date'])
                    if row.get('end_date'):
                        teller['endDate'] = self._parse_date(row['end_date'])

                    # Status
                    if row.get('status') and not pd.isna(row.get('status')):
                        teller['status'] = row['status']

                    tellers.append(teller)

                if tellers:
                    self.config['tellers'] = tellers
                    logger.info(f"  ✓ Tellers: {len(tellers)} tellers")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Tellers: {str(e)}")

        # 7. Teller Cashier Mappings
        df = self._read_sheet('Teller Cashier Mapping')
        if not df.empty:
            try:
                teller_mappings = []
                for _, row in df.iterrows():
                    if not row.get('teller_name') or pd.isna(row.get('teller_name')):
                        continue

                    mapping = {
                        'tellerName': row['teller_name']
                    }

                    # Staff/Cashier - prefer external_id over name
                    if row.get('staff_external_id') and not pd.isna(row.get('staff_external_id')):
                        mapping['staffExternalId'] = row['staff_external_id']
                    elif row.get('staff_name') and not pd.isna(row.get('staff_name')):
                        mapping['staffName'] = row['staff_name']

                    # Start/End date
                    if row.get('start_date'):
                        mapping['startDate'] = self._parse_date(row['start_date'])
                    if row.get('end_date'):
                        mapping['endDate'] = self._parse_date(row['end_date'])

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        mapping['description'] = row['description']

                    teller_mappings.append(mapping)

                if teller_mappings:
                    self.config['tellerCashierMappings'] = teller_mappings
                    logger.info(f"  ✓ Teller Cashier Mappings: {len(teller_mappings)} mappings")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Teller Cashier Mappings: {str(e)}")

    def convert_products(self):
        """Convert Phase 4: Financial Products"""
        logger.info("Converting Phase 4: Financial Products...")

        # 1. Charges
        df = self._read_sheet('Charges')
        if not df.empty:
            charges = []
            for _, row in df.iterrows():
                charge = {
                    'name': row['charge_name'],
                    'chargeAppliesTo': row.get('charge_type') or row.get('applies_to', 'Loan'),
                    'chargeTimeType': row.get('charge_time') or row.get('time_type', 'Disbursement'),
                    'chargeCalculationType': row['calculation_type'],
                    'amount': float(row['amount']) if row.get('amount') and not pd.isna(row['amount']) else 0,
                    'currencyCode': row.get('currency') or row.get('currency_code', 'XAF'),
                    'active': self._parse_boolean(row.get('active', True))
                }

                # Optional fields
                if row.get('charge_payment_mode') and not pd.isna(row.get('charge_payment_mode')):
                    charge['chargePaymentMode'] = row['charge_payment_mode']
                if row.get('penalty') and not pd.isna(row.get('penalty')):
                    charge['penalty'] = self._parse_boolean(row['penalty'])
                if row.get('fee_frequency') and not pd.isna(row.get('fee_frequency')):
                    charge['feeFrequency'] = row['fee_frequency']

                charges.append(charge)

            self.config['charges'] = charges
            logger.info(f"  ✓ Charges: {len(charges)} charges")

        # 2. Loan Products
        df = self._read_sheet('Loan Products')
        if not df.empty:
            loan_products = []
            for _, row in df.iterrows():
                # Skip if no product name
                if not row.get('product_name') or pd.isna(row.get('product_name')):
                    continue

                product = {
                    'name': row['product_name'],
                    'shortName': row['short_name'],
                    'currencyCode': row.get('currency', 'XAF'),
                    'accountingRule': 'NONE'  # Default to NONE, accounting configured separately
                }

                # Add optional fields if present
                if row.get('description'):
                    product['description'] = row['description']

                # Principal configuration
                if row.get('principal_min') and not pd.isna(row.get('principal_min')):
                    product['minPrincipal'] = float(row['principal_min'])
                if row.get('principal_default') and not pd.isna(row.get('principal_default')):
                    product['principal'] = float(row['principal_default'])
                if row.get('principal_max') and not pd.isna(row.get('principal_max')):
                    product['maxPrincipal'] = float(row['principal_max'])

                # Number of repayments configuration
                if row.get('number_of_repayments_min') and not pd.isna(row.get('number_of_repayments_min')):
                    product['minNumberOfRepayments'] = int(row['number_of_repayments_min'])
                if row.get('number_of_repayments_default') and not pd.isna(row.get('number_of_repayments_default')):
                    product['numberOfRepayments'] = int(row['number_of_repayments_default'])
                if row.get('number_of_repayments_max') and not pd.isna(row.get('number_of_repayments_max')):
                    product['maxNumberOfRepayments'] = int(row['number_of_repayments_max'])

                # Repayment configuration (MANDATORY)
                if row.get('repayment_every') and not pd.isna(row.get('repayment_every')):
                    product['repaymentEvery'] = int(row['repayment_every'])
                else:
                    product['repaymentEvery'] = 1  # Default to 1

                if row.get('repayment_frequency') and not pd.isna(row.get('repayment_frequency')):
                    product['repaymentFrequencyType'] = row['repayment_frequency']

                # Digits after decimal (MANDATORY)
                if row.get('digits_after_decimal') is not None and not pd.isna(row.get('digits_after_decimal')):
                    product['digitsAfterDecimal'] = int(row['digits_after_decimal'])
                else:
                    product['digitsAfterDecimal'] = 0  # Default for XAF

                # Days in year/month type (MANDATORY)
                if row.get('days_in_year_type') and not pd.isna(row.get('days_in_year_type')):
                    product['daysInYearType'] = int(row['days_in_year_type'])
                else:
                    product['daysInYearType'] = 365  # Default to 365

                if row.get('days_in_month_type') and not pd.isna(row.get('days_in_month_type')):
                    product['daysInMonthType'] = int(row['days_in_month_type'])
                else:
                    product['daysInMonthType'] = 30  # Default to 30

                # Interest recalculation (MANDATORY)
                if row.get('is_interest_recalculation_enabled') and not pd.isna(row.get('is_interest_recalculation_enabled')):
                    product['isInterestRecalculationEnabled'] = self._parse_boolean(row['is_interest_recalculation_enabled'])
                else:
                    product['isInterestRecalculationEnabled'] = False  # Default to false

                # Accounting type
                if row.get('accounting_type') and not pd.isna(row.get('accounting_type')):
                    accounting_type = row['accounting_type'].lower()
                    if 'accrual' in accounting_type:
                        product['accountingRule'] = 'ACCRUAL_PERIODIC'
                    elif 'cash' in accounting_type:
                        product['accountingRule'] = 'CASH_BASED'
                    else:
                        product['accountingRule'] = 'NONE'

                # Interest rate configuration (CRITICAL)
                if row.get('interest_rate_min') and not pd.isna(row.get('interest_rate_min')):
                    product['minInterestRatePerPeriod'] = float(row['interest_rate_min'])
                if row.get('interest_rate_default') and not pd.isna(row.get('interest_rate_default')):
                    product['interestRatePerPeriod'] = float(row['interest_rate_default'])
                if row.get('interest_rate_max') and not pd.isna(row.get('interest_rate_max')):
                    product['maxInterestRatePerPeriod'] = float(row['interest_rate_max'])

                # Interest calculation
                if row.get('interest_calculation_period') and not pd.isna(row.get('interest_calculation_period')):
                    product['interestCalculationPeriodType'] = row['interest_calculation_period']
                if row.get('interest_type') and not pd.isna(row.get('interest_type')):
                    product['interestType'] = row['interest_type']
                if row.get('amortization_type') and not pd.isna(row.get('amortization_type')):
                    product['amortizationType'] = row['amortization_type']

                # Grace periods
                if row.get('grace_on_principal_periods') and not pd.isna(row.get('grace_on_principal_periods')):
                    product['graceOnPrincipalPayment'] = int(row['grace_on_principal_periods'])
                if row.get('grace_on_interest_periods') and not pd.isna(row.get('grace_on_interest_periods')):
                    product['graceOnInterestPayment'] = int(row['grace_on_interest_periods'])

                # Fees
                if row.get('processing_fee_percent') and not pd.isna(row.get('processing_fee_percent')):
                    product['processingFeePercent'] = float(row['processing_fee_percent'])

                # Insurance
                if row.get('insurance_mandatory') and not pd.isna(row.get('insurance_mandatory')):
                    product['insuranceMandatory'] = self._parse_boolean(row['insurance_mandatory'])
                if row.get('insurance_percent') and not pd.isna(row.get('insurance_percent')):
                    product['insurancePercent'] = float(row['insurance_percent'])

                # Advanced settings
                if row.get('minimum_gap_between_installments') and not pd.isna(row.get('minimum_gap_between_installments')):
                    product['minimumGapBetweenInstallments'] = int(row['minimum_gap_between_installments'])
                if row.get('allow_partial_period_interest_calculation') and not pd.isna(row.get('allow_partial_period_interest_calculation')):
                    product['allowPartialPeriodInterestCalculation'] = self._parse_boolean(row['allow_partial_period_interest_calculation'])

                loan_products.append(product)

            self.config['loanProducts'] = loan_products
            logger.info(f"  ✓ Loan Products: {len(loan_products)} products")

        # 3. Savings Products
        df = self._read_sheet('Savings Products')
        if not df.empty:
            savings_products = []
            for _, row in df.iterrows():
                # Skip if no product name
                if not row.get('product_name') or pd.isna(row.get('product_name')):
                    continue

                product = {
                    'name': row['product_name'],
                    'shortName': row['short_name'],
                    'currencyCode': row.get('currency') or row.get('currency_code', 'XAF'),
                    'digitsAfterDecimal': int(row.get('digits_after_decimal', 0)) if row.get('digits_after_decimal') is not None and not pd.isna(row.get('digits_after_decimal')) else 0,
                    'nominalAnnualInterestRate': float(row.get('nominal_annual_interest_rate', 0)) if row.get('nominal_annual_interest_rate') and not pd.isna(row['nominal_annual_interest_rate']) else 0,
                    'interestCompoundingPeriodType': row.get('interest_compounding_period') or row.get('compounding_period', 'MONTHLY'),
                    'interestPostingPeriodType': row.get('interest_posting_period') or row.get('posting_period', 'MONTHLY'),
                    'interestCalculationType': row.get('interest_calculation_type') or row.get('calculation_type', 'DAILY_BALANCE'),
                    'interestCalculationDaysInYearType': row.get('interest_calculation_days_in_year') or row.get('days_in_year', 'DAYS_365'),
                    'accountingRule': 'NONE'  # Default, accounting configured separately
                }

                # Accounting type
                if row.get('accounting_type') and not pd.isna(row.get('accounting_type')):
                    accounting_type = row['accounting_type'].lower()
                    if 'cash' in accounting_type:
                        product['accountingRule'] = 'CASH_BASED'
                    elif 'accrual' in accounting_type:
                        product['accountingRule'] = 'ACCRUAL_PERIODIC'
                    else:
                        product['accountingRule'] = 'NONE'

                # Optional fields
                if row.get('description') and not pd.isna(row.get('description')):
                    product['description'] = row['description']
                if row.get('minimum_opening_balance') and not pd.isna(row.get('minimum_opening_balance')):
                    product['minRequiredOpeningBalance'] = float(row['minimum_opening_balance'])
                if row.get('minimum_balance') and not pd.isna(row.get('minimum_balance')):
                    product['minBalanceForInterestCalculation'] = float(row['minimum_balance'])

                # Overdraft settings
                if row.get('overdraft_allowed') and not pd.isna(row.get('overdraft_allowed')):
                    product['overdraftAllowed'] = self._parse_boolean(row['overdraft_allowed'])

                # Withdrawal fees
                if row.get('withdrawal_fee_for_transfers') and not pd.isna(row.get('withdrawal_fee_for_transfers')):
                    product['withdrawalFeeForTransfers'] = self._parse_boolean(row['withdrawal_fee_for_transfers'])

                # Dormancy tracking
                if row.get('allow_dormancy_tracking') and not pd.isna(row.get('allow_dormancy_tracking')):
                    product['allowDormancyTracking'] = self._parse_boolean(row['allow_dormancy_tracking'])
                if row.get('days_to_inactive') and not pd.isna(row.get('days_to_inactive')):
                    product['daysToInactive'] = int(row['days_to_inactive'])
                if row.get('days_to_dormancy') and not pd.isna(row.get('days_to_dormancy')):
                    product['daysToDormancy'] = int(row['days_to_dormancy'])

                # Tax withholding
                if row.get('withhold_tax') and not pd.isna(row.get('withhold_tax')):
                    product['withholdTax'] = self._parse_boolean(row['withhold_tax'])

                # Lock-in period
                if row.get('lock_in_period') and not pd.isna(row.get('lock_in_period')):
                    product['lockinPeriodFrequency'] = int(row['lock_in_period'])

                savings_products.append(product)

            self.config['savingsProducts'] = savings_products
            logger.info(f"  ✓ Savings Products: {len(savings_products)} products")

        # 4. Delinquency Buckets
        df = self._read_sheet('Delinquency Buckets')
        if not df.empty:
            delinquency_buckets = []
            for _, row in df.iterrows():
                if not row.get('bucket_name') or pd.isna(row.get('bucket_name')):
                    continue

                bucket = {
                    'name': row['bucket_name']
                }

                # Min/Max days
                if row.get('min_days') and not pd.isna(row.get('min_days')):
                    bucket['minDays'] = int(row['min_days'])
                if row.get('max_days') and not pd.isna(row.get('max_days')):
                    bucket['maxDays'] = int(row['max_days'])

                # Classification
                if row.get('classification') and not pd.isna(row.get('classification')):
                    bucket['classification'] = row['classification']

                delinquency_buckets.append(bucket)

            if delinquency_buckets:
                self.config['delinquencyBuckets'] = delinquency_buckets
                logger.info(f"  ✓ Delinquency Buckets: {len(delinquency_buckets)} buckets")

        # 5. Loan Provisioning
        df = self._read_sheet('Loan Provisioning')
        if not df.empty:
            provisioning = []
            for _, row in df.iterrows():
                if not row.get('category_name') or pd.isna(row.get('category_name')):
                    continue

                category = {
                    'categoryName': row['category_name']
                }

                # Days range
                if row.get('min_days') and not pd.isna(row.get('min_days')):
                    category['minDays'] = int(row['min_days'])
                if row.get('max_days') and not pd.isna(row.get('max_days')):
                    category['maxDays'] = int(row['max_days'])

                # Provision percentage
                if row.get('provision_percentage') and not pd.isna(row.get('provision_percentage')):
                    category['provisioningPercentage'] = float(row['provision_percentage'])

                # GL accounts
                if row.get('liability_gl_code') and not pd.isna(row.get('liability_gl_code')):
                    category['liabilityAccountId'] = int(row['liability_gl_code'])
                if row.get('expense_gl_code') and not pd.isna(row.get('expense_gl_code')):
                    category['expenseAccountId'] = int(row['expense_gl_code'])

                provisioning.append(category)

            if provisioning:
                self.config['loanProvisioning'] = provisioning
                logger.info(f"  ✓ Loan Provisioning: {len(provisioning)} categories")

        # 6. Collateral Types
        df = self._read_sheet('Collateral Types')
        if not df.empty:
            try:
                collateral_types = []
                for _, row in df.iterrows():
                    # Use collateral_type column
                    collateral_name = row.get('collateral_type') or row.get('collateral_name')
                    if not collateral_name or pd.isna(collateral_name):
                        continue

                    collateral = {
                        'name': collateral_name
                    }

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        collateral['description'] = row['description']

                    # Requires valuation
                    if row.get('requires_valuation') and not pd.isna(row.get('requires_valuation')):
                        collateral['requiresValuation'] = self._parse_boolean(row['requires_valuation'])

                    # Position
                    if row.get('position') and not pd.isna(row.get('position')):
                        collateral['position'] = int(row['position'])

                    # Quality
                    if row.get('quality') and not pd.isna(row.get('quality')):
                        collateral['quality'] = row['quality']

                    # Unit type
                    if row.get('unit_type') and not pd.isna(row.get('unit_type')):
                        collateral['unitType'] = row['unit_type']

                    # Base price/pct
                    if row.get('base_price') and not pd.isna(row.get('base_price')):
                        collateral['basePrice'] = float(row['base_price'])
                    if row.get('pct_to_base') and not pd.isna(row.get('pct_to_base')):
                        collateral['pctToBase'] = float(row['pct_to_base'])

                    collateral_types.append(collateral)

                if collateral_types:
                    self.config['collateralTypes'] = collateral_types
                    logger.info(f"  ✓ Collateral Types: {len(collateral_types)} types")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Collateral Types: {str(e)}")

        # 7. Guarantor Types
        df = self._read_sheet('Guarantor Types')
        if not df.empty:
            try:
                guarantor_types = []
                for _, row in df.iterrows():
                    # Use guarantor_type column
                    guarantor_name = row.get('guarantor_type') or row.get('guarantor_name')
                    if not guarantor_name or pd.isna(guarantor_name):
                        continue

                    guarantor = {
                        'name': guarantor_name
                    }

                    # Description
                    if row.get('description') and not pd.isna(row.get('description')):
                        guarantor['description'] = row['description']

                    # External/Internal
                    if row.get('is_external') and not pd.isna(row.get('is_external')):
                        guarantor['isExternal'] = self._parse_boolean(row['is_external'])

                    # Mandatory
                    if row.get('mandatory') and not pd.isna(row.get('mandatory')):
                        guarantor['mandatory'] = self._parse_boolean(row['mandatory'])

                    guarantor_types.append(guarantor)

                if guarantor_types:
                    self.config['guarantorTypes'] = guarantor_types
                    logger.info(f"  ✓ Guarantor Types: {len(guarantor_types)} types")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Guarantor Types: {str(e)}")

        # 8. Floating Rates
        df = self._read_sheet('Floating Rates')
        if not df.empty:
            try:
                floating_rates = []
                for _, row in df.iterrows():
                    if not row.get('rate_name') or pd.isna(row.get('rate_name')):
                        continue

                    rate = {
                        'name': row['rate_name']
                    }

                    # Is base lending rate
                    if row.get('is_base_lending_rate') and not pd.isna(row.get('is_base_lending_rate')):
                        rate['isBaseLendingRate'] = self._parse_boolean(row['is_base_lending_rate'])

                    # Is active
                    if row.get('is_active') and not pd.isna(row.get('is_active')):
                        rate['isActive'] = self._parse_boolean(row['is_active'])

                    # Created by
                    if row.get('created_by') and not pd.isna(row.get('created_by')):
                        rate['createdBy'] = row['created_by']

                    floating_rates.append(rate)

                if floating_rates:
                    self.config['floatingRates'] = floating_rates
                    logger.info(f"  ✓ Floating Rates: {len(floating_rates)} rates")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Floating Rates: {str(e)}")

        # Load accounting mappings and merge with products
        self._load_product_accounting_mappings()

    def _load_product_accounting_mappings(self):
        """Load GL account mappings for loan and savings products"""

        # 1. Loan Product Accounting Mappings
        df = self._read_sheet('Loan Product Accounting')
        if not df.empty and 'product_short_name' in df.columns:
            # Group mappings by product
            mappings_by_product = {}
            for _, row in df.iterrows():
                product_key = row.get('product_short_name')
                if not product_key or pd.isna(product_key):
                    continue

                if product_key not in mappings_by_product:
                    mappings_by_product[product_key] = {}

                mapping_type = row.get('mapping_type')
                gl_code = row.get('gl_code')

                if mapping_type and not pd.isna(mapping_type) and gl_code and not pd.isna(gl_code):
                    # Map the accounting types to Java model field names using AccountCode suffix
                    # The loader will resolve these GL codes to IDs via context
                    mapping_field_map = {
                        'Fund Source': 'fundSourceAccountCode',
                        'Loan Portfolio': 'loanPortfolioAccountCode',
                        'Interest Receivable': 'receivableInterestAccountCode',
                        'Fees Receivable': 'receivableFeeAccountCode',
                        'Penalties Receivable': 'receivablePenaltyAccountCode',
                        'Transfer in Suspense': 'transfersInSuspenseAccountCode',
                        'Interest Income': 'interestOnLoansAccountCode',  # Match Java model
                        'Fee Income': 'incomeFromFeesAccountCode',        # Match Java model (with 's')
                        'Penalty Income': 'incomeFromPenaltiesAccountCode', # Match Java model (with 's')
                        'Losses Written Off': 'writeOffAccountCode',
                        'Goodwill Credit': 'goodwillCreditAccountCode',
                        'Income from Recovery': 'incomeFromRecoveryAccountCode',
                        'Over Payment Liability': 'overpaymentLiabilityAccountCode'
                    }

                    field_name = mapping_field_map.get(mapping_type)
                    if field_name:
                        # Store as string (GL code) for resolution by the loader
                        mappings_by_product[product_key][field_name] = str(int(gl_code))

            # Merge mappings into loan products (directly on product, not nested)
            if 'loanProducts' in self.config and mappings_by_product:
                for product in self.config['loanProducts']:
                    short_name = product.get('shortName')
                    if short_name in mappings_by_product:
                        accounting = mappings_by_product[short_name]
                        if accounting:  # Only add if there are mappings
                            product['accountingRule'] = 'ACCRUAL_PERIODIC'
                            # Merge accounting fields directly onto product
                            product.update(accounting)

                logger.info(f"  ✓ Loan Product Accounting: {len(mappings_by_product)} products mapped")

        # 2. Savings Product Accounting Mappings
        df = self._read_sheet('Savings Product Accounting')
        if not df.empty and 'product_short_name' in df.columns:
            # Group mappings by product
            mappings_by_product = {}
            for _, row in df.iterrows():
                product_key = row.get('product_short_name')
                if not product_key or pd.isna(product_key):
                    continue

                if product_key not in mappings_by_product:
                    mappings_by_product[product_key] = {}

                mapping_type = row.get('mapping_type')
                gl_code = row.get('gl_code')

                if mapping_type and not pd.isna(mapping_type) and gl_code and not pd.isna(gl_code):
                    # Map the accounting types to Java model field names (using AccountCode suffix)
                    # The loader resolves these codes to IDs via context
                    # NOTE: escheatLiabilityAccountId is NOT supported by Fineract API
                    mapping_field_map = {
                        'Savings Control': 'savingsControlAccountCode',
                        'Savings Reference': 'savingsReferenceAccountCode',
                        'Transfer in Suspense': 'transfersInSuspenseAccountCode',
                        'Interest Payable': 'interestOnSavingsAccountCode',
                        'Interest on Savings': 'interestOnSavingsAccountCode',  # Alias
                        # 'Escheat Liability': excluded - not supported by Fineract API
                        'Income from Fees': 'incomeFromFeesAccountCode',
                        'Income from Penalties': 'incomeFromPenaltiesAccountCode',
                        'Overdraft Portfolio Control': 'overdraftPortfolioControlAccountCode',
                        'Overdraft Interest Income': 'incomeFromInterestAccountCode',
                        'Income from Interest': 'incomeFromInterestAccountCode',  # Alias
                        'Overdraft Losses Written Off': 'writeOffAccountCode',
                        'Losses Written Off': 'writeOffAccountCode',  # Alias
                        'Overdraft Income from Recovery': 'incomeFromRecoveryAccountCode'
                    }

                    field_name = mapping_field_map.get(mapping_type)
                    if field_name:
                        # Store as string (GL code) for resolution by loader
                        mappings_by_product[product_key][field_name] = str(int(gl_code))

            # Merge mappings into savings products (directly on product, not nested)
            if 'savingsProducts' in self.config and mappings_by_product:
                for product in self.config['savingsProducts']:
                    short_name = product.get('shortName')
                    if short_name in mappings_by_product:
                        accounting = mappings_by_product[short_name]
                        if accounting:  # Only add if there are mappings
                            product['accountingRule'] = 'CASH_BASED'
                            # Merge accounting fields directly into product (not nested)
                            product.update(accounting)

                logger.info(f"  ✓ Savings Product Accounting: {len(mappings_by_product)} products mapped")

        # 3. Payment Type Accounting Mappings
        df = self._read_sheet('Payment Type Accounting')
        if not df.empty:
            try:
                payment_type_mappings = []
                for _, row in df.iterrows():
                    if not row.get('payment_type') or pd.isna(row.get('payment_type')):
                        continue

                    mapping = {
                        'paymentTypeName': row['payment_type']
                    }

                    # Fund source GL code
                    if row.get('fund_source_account') and not pd.isna(row.get('fund_source_account')):
                        mapping['fundSourceAccountCode'] = str(row['fund_source_account'])

                    # Cash/Bank control GL code
                    if row.get('cash_account') and not pd.isna(row.get('cash_account')):
                        mapping['cashAccountCode'] = str(row['cash_account'])

                    payment_type_mappings.append(mapping)

                if payment_type_mappings:
                    self.config['paymentTypeAccountingMappings'] = payment_type_mappings
                    logger.info(f"  ✓ Payment Type Accounting: {len(payment_type_mappings)} mappings")
            except Exception as e:
                logger.warning(f"  ⚠ Skipping Payment Type Accounting: {str(e)}")

    def convert_operations(self):
        """Convert Phase 5: Client Operations & Accounts"""
        logger.info("Converting Phase 5: Client Operations & Accounts...")

        # 1. Clients
        df = self._read_sheet('Clients')
        if not df.empty:
            clients = []
            for _, row in df.iterrows():
                client = {
                    'firstName': row.get('firstname') or row.get('first_name'),
                    'lastName': row.get('lastname') or row.get('last_name'),
                    'officeName': row.get('office') or row.get('office_name'),
                    'active': True  # Default to true
                }

                # Basic fields
                if row.get('external_id'):
                    client['externalId'] = row['external_id']
                if row.get('activation_date'):
                    client['activationDate'] = self._parse_date(row['activation_date'])
                if row.get('mobile'):
                    client['mobileNo'] = row['mobile']
                if row.get('date_of_birth'):
                    client['dateOfBirth'] = self._parse_date(row['date_of_birth'])
                if row.get('gender'):
                    client['gender'] = row['gender']

                # Enhanced fields (13 missing fields)
                if row.get('middle_name') and not pd.isna(row.get('middle_name')):
                    client['middleName'] = row['middle_name']
                if row.get('email') and not pd.isna(row.get('email')):
                    client['emailAddress'] = row['email']
                if row.get('client_type') and not pd.isna(row.get('client_type')):
                    client['clientType'] = row['client_type']
                if row.get('client_classification') and not pd.isna(row.get('client_classification')):
                    client['clientClassification'] = row['client_classification']
                if row.get('legal_form') and not pd.isna(row.get('legal_form')):
                    client['legalForm'] = row['legal_form']
                # legalFormId is required by Fineract API: 1 = Person, 2 = Entity
                if row.get('legal_form_id') and not pd.isna(row.get('legal_form_id')):
                    client['legalFormId'] = int(row['legal_form_id'])
                elif row.get('client_type') and not pd.isna(row.get('client_type')):
                    # Infer from client_type if legal_form_id not provided
                    client_type = str(row['client_type']).lower()
                    client['legalFormId'] = 2 if client_type in ['corporate', 'entity', 'business'] else 1
                else:
                    # Default to Person (Individual)
                    client['legalFormId'] = 1
                if row.get('staff_name') and not pd.isna(row.get('staff_name')):
                    client['staffName'] = row['staff_name']
                if row.get('submitted_on_date'):
                    client['submittedOnDate'] = self._parse_date(row['submitted_on_date'])
                if row.get('address_line_1') and not pd.isna(row.get('address_line_1')):
                    client['addressLine1'] = row['address_line_1']
                if row.get('address_line_2') and not pd.isna(row.get('address_line_2')):
                    client['addressLine2'] = row['address_line_2']
                if row.get('city') and not pd.isna(row.get('city')):
                    client['city'] = row['city']
                if row.get('state_province') and not pd.isna(row.get('state_province')):
                    client['stateProvince'] = row['state_province']
                if row.get('country') and not pd.isna(row.get('country')):
                    client['country'] = row['country']
                if row.get('postal_code') and not pd.isna(row.get('postal_code')):
                    client['postalCode'] = row['postal_code']

                clients.append(client)

            self.config['clients'] = clients
            logger.info(f"  ✓ Clients: {len(clients)} clients")

        # 2. Groups
        df = self._read_sheet('Groups')
        if not df.empty:
            groups = []
            for _, row in df.iterrows():
                # Skip if no group name
                if not row.get('group_name') or pd.isna(row.get('group_name')):
                    continue

                group = {
                    'name': row['group_name'],
                    'officeName': row.get('office') or row.get('office_name'),
                    'active': self._parse_boolean(row.get('active', True))
                }

                # Optional fields
                if row.get('external_id') and not pd.isna(row.get('external_id')):
                    group['externalId'] = row['external_id']
                if row.get('activation_date') and not pd.isna(row.get('activation_date')):
                    group['activationDate'] = self._parse_date(row['activation_date'])
                if row.get('submitted_on_date') and not pd.isna(row.get('submitted_on_date')):
                    group['submittedOnDate'] = self._parse_date(row['submitted_on_date'])
                # Staff linking (use staff_name or staff column)
                staff = row.get('staff_name') or row.get('staff')
                if staff and not pd.isna(staff):
                    group['staffName'] = staff
                if row.get('center_name') and not pd.isna(row.get('center_name')) and row['center_name']:
                    group['centerName'] = row['center_name']

                # Client members (comma-separated external IDs)
                if row.get('client_external_ids') and not pd.isna(row.get('client_external_ids')):
                    client_ids = str(row['client_external_ids']).split(',')
                    group['clientExternalIds'] = [cid.strip() for cid in client_ids if cid.strip()]

                groups.append(group)

            self.config['groups'] = groups
            logger.info(f"  ✓ Groups: {len(groups)} groups")

        # 3. Centers
        df = self._read_sheet('Centers')
        if not df.empty:
            centers = []
            for _, row in df.iterrows():
                # Skip if no center name
                if not row.get('center_name') or pd.isna(row.get('center_name')):
                    continue

                center = {
                    'name': row['center_name'],
                    'officeName': row.get('office') or row.get('office_name'),
                    'active': self._parse_boolean(row.get('active', True))
                }

                # Optional fields
                if row.get('external_id') and not pd.isna(row.get('external_id')):
                    center['externalId'] = row['external_id']
                if row.get('activation_date') and not pd.isna(row.get('activation_date')):
                    center['activationDate'] = self._parse_date(row['activation_date'])
                if row.get('submitted_on_date') and not pd.isna(row.get('submitted_on_date')):
                    center['submittedOnDate'] = self._parse_date(row['submitted_on_date'])
                # Staff linking (use staff_name or staff column)
                staff = row.get('staff_name') or row.get('staff')
                if staff and not pd.isna(staff):
                    center['staffName'] = staff

                centers.append(center)

            self.config['centers'] = centers
            logger.info(f"  ✓ Centers: {len(centers)} centers")

        # 4. Loan Accounts
        df = self._read_sheet('Loan Accounts')
        if not df.empty:
            loan_accounts = []
            for _, row in df.iterrows():
                # Skip if no client external ID
                if not row.get('client_external_id') or pd.isna(row.get('client_external_id')):
                    continue

                account = {
                    'clientExternalId': row['client_external_id'],
                    'productShortName': row.get('product') or row.get('product_short_name')
                }

                # Dates
                if row.get('submitted_on') and not pd.isna(row.get('submitted_on')):
                    account['submittedOnDate'] = self._parse_date(row['submitted_on'])
                if row.get('expected_disbursement_date') and not pd.isna(row.get('expected_disbursement_date')):
                    account['expectedDisbursementDate'] = self._parse_date(row['expected_disbursement_date'])
                if row.get('disbursement_date') and not pd.isna(row.get('disbursement_date')):
                    account['actualDisbursementDate'] = self._parse_date(row['disbursement_date'])

                # Amount
                if row.get('principal') and not pd.isna(row.get('principal')):
                    account['principal'] = float(row['principal'])

                # Terms
                if row.get('loan_term') and not pd.isna(row.get('loan_term')):
                    account['numberOfRepayments'] = int(row['loan_term'])
                if row.get('repayment_every') and not pd.isna(row.get('repayment_every')):
                    account['repaymentEvery'] = int(row['repayment_every'])

                # External ID
                if row.get('external_id') and not pd.isna(row.get('external_id')):
                    account['externalId'] = row['external_id']

                # Workflow state (active, pending_approval, pending_disbursal)
                if row.get('workflow_state') and not pd.isna(row.get('workflow_state')):
                    account['workflowState'] = row['workflow_state']

                # Fund source
                if row.get('fund_source') and not pd.isna(row.get('fund_source')):
                    account['fundSourceName'] = row['fund_source']

                # Payment type
                if row.get('payment_type') and not pd.isna(row.get('payment_type')):
                    account['paymentTypeName'] = row['payment_type']

                loan_accounts.append(account)

            if loan_accounts:
                self.config['loanAccounts'] = loan_accounts
                logger.info(f"  ✓ Loan Accounts: {len(loan_accounts)} accounts")

        # 5. Savings Accounts
        df = self._read_sheet('Savings Accounts')
        if not df.empty:
            savings_accounts = []
            for _, row in df.iterrows():
                # Skip if no client external ID
                if not row.get('client_external_id') or pd.isna(row.get('client_external_id')):
                    continue

                account = {
                    'clientExternalId': row['client_external_id'],
                    'productShortName': row.get('product') or row.get('product_short_name')
                }

                # Dates
                if row.get('submitted_on') and not pd.isna(row.get('submitted_on')):
                    account['submittedOnDate'] = self._parse_date(row['submitted_on'])

                # External ID
                if row.get('external_id') and not pd.isna(row.get('external_id')):
                    account['externalId'] = row['external_id']

                # Field officer
                if row.get('field_officer') and not pd.isna(row.get('field_officer')):
                    account['fieldOfficerName'] = row['field_officer']

                # Activate flag
                if row.get('activate') and not pd.isna(row.get('activate')):
                    account['activate'] = self._parse_boolean(row['activate'])

                savings_accounts.append(account)

            if savings_accounts:
                self.config['savingsAccounts'] = savings_accounts
                logger.info(f"  ✓ Savings Accounts: {len(savings_accounts)} accounts")

    def convert_transactions(self):
        """Convert Phase 6: Transactions & Operations"""
        logger.info("Converting Phase 6: Transactions...")

        # 1. Savings Deposits
        df = self._read_sheet('Savings Deposits')
        if not df.empty:
            deposits = []
            for _, row in df.iterrows():
                # Support both account_external_id and savings_account_number
                account_id = row.get('account_external_id') or row.get('savings_account_number')
                if not account_id or pd.isna(account_id):
                    continue

                txn = {
                    'accountExternalId': str(account_id),
                    'transactionType': 'DEPOSIT'
                }

                if row.get('transaction_date') and not pd.isna(row.get('transaction_date')):
                    txn['transactionDate'] = self._parse_date(row['transaction_date'])
                # Support both 'amount' and 'transaction_amount'
                amount = row.get('amount') or row.get('transaction_amount')
                if amount and not pd.isna(amount):
                    txn['transactionAmount'] = float(amount)
                if row.get('payment_type') and not pd.isna(row.get('payment_type')):
                    txn['paymentTypeName'] = row['payment_type']
                if row.get('note') and not pd.isna(row.get('note')):
                    txn['note'] = row['note']

                deposits.append(txn)

            if deposits:
                if 'savingsTransactions' not in self.config:
                    self.config['savingsTransactions'] = []
                self.config['savingsTransactions'].extend(deposits)
                logger.info(f"  ✓ Savings Deposits: {len(deposits)} transactions")

        # 2. Savings Withdrawals
        df = self._read_sheet('Savings Withdrawals')
        if not df.empty:
            withdrawals = []
            for _, row in df.iterrows():
                # Support both account_external_id and savings_account_number
                account_id = row.get('account_external_id') or row.get('savings_account_number')
                if not account_id or pd.isna(account_id):
                    continue

                txn = {
                    'accountExternalId': str(account_id),
                    'transactionType': 'WITHDRAWAL'
                }

                if row.get('transaction_date') and not pd.isna(row.get('transaction_date')):
                    txn['transactionDate'] = self._parse_date(row['transaction_date'])
                # Support both 'amount' and 'transaction_amount'
                amount = row.get('amount') or row.get('transaction_amount')
                if amount and not pd.isna(amount):
                    txn['transactionAmount'] = float(amount)
                if row.get('payment_type') and not pd.isna(row.get('payment_type')):
                    txn['paymentTypeName'] = row['payment_type']
                if row.get('note') and not pd.isna(row.get('note')):
                    txn['note'] = row['note']

                withdrawals.append(txn)

            if withdrawals:
                if 'savingsTransactions' not in self.config:
                    self.config['savingsTransactions'] = []
                self.config['savingsTransactions'].extend(withdrawals)
                logger.info(f"  ✓ Savings Withdrawals: {len(withdrawals)} transactions")

        # 3. Loan Repayments
        df = self._read_sheet('Loan Repayments')
        if not df.empty:
            repayments = []
            for _, row in df.iterrows():
                # Support both loan_external_id and loan_account_number
                loan_id = row.get('loan_external_id') or row.get('loan_account_number')
                if not loan_id or pd.isna(loan_id):
                    continue

                txn = {
                    'loanExternalId': str(loan_id),
                    'transactionType': 'REPAYMENT'
                }

                if row.get('transaction_date') and not pd.isna(row.get('transaction_date')):
                    txn['transactionDate'] = self._parse_date(row['transaction_date'])
                # Calculate total amount from components or use amount directly
                total_amount = 0
                if row.get('amount') and not pd.isna(row.get('amount')):
                    total_amount = float(row['amount'])
                else:
                    # Sum up the components
                    for field in ['principal_amount', 'interest_amount', 'fee_amount', 'penalty_amount']:
                        if row.get(field) and not pd.isna(row.get(field)):
                            total_amount += float(row[field])
                if total_amount > 0:
                    txn['transactionAmount'] = total_amount
                if row.get('payment_type') and not pd.isna(row.get('payment_type')):
                    txn['paymentTypeName'] = row['payment_type']
                if row.get('note') and not pd.isna(row.get('note')):
                    txn['note'] = row['note']
                if row.get('receipt_number') and not pd.isna(row.get('receipt_number')):
                    txn['receiptNumber'] = row['receipt_number']

                repayments.append(txn)

            if repayments:
                self.config['loanTransactions'] = repayments
                logger.info(f"  ✓ Loan Repayments: {len(repayments)} transactions")

        # 4. Loan Collateral
        df = self._read_sheet('Loan Collateral')
        if not df.empty:
            collaterals = []
            for _, row in df.iterrows():
                loan_id = row.get('loan_external_id')
                if not loan_id or pd.isna(loan_id):
                    continue

                collateral = {
                    'loanExternalId': str(loan_id)
                }
                if row.get('collateral_type_name') and not pd.isna(row.get('collateral_type_name')):
                    collateral['collateralTypeName'] = row['collateral_type_name']
                if row.get('value') and not pd.isna(row.get('value')):
                    collateral['value'] = float(row['value'])
                if row.get('description') and not pd.isna(row.get('description')):
                    collateral['description'] = row['description']

                collaterals.append(collateral)

            if collaterals:
                self.config['loanCollaterals'] = collaterals
                logger.info(f"  ✓ Loan Collateral: {len(collaterals)} items")

        # 5. Loan Guarantors
        df = self._read_sheet('Loan Guarantors')
        if not df.empty:
            guarantors = []
            for _, row in df.iterrows():
                loan_id = row.get('loan_external_id')
                if not loan_id or pd.isna(loan_id):
                    continue

                guarantor = {
                    'loanExternalId': str(loan_id)
                }
                if row.get('guarantor_type') and not pd.isna(row.get('guarantor_type')):
                    guarantor['guarantorType'] = row['guarantor_type']
                if row.get('client_external_id') and not pd.isna(row.get('client_external_id')):
                    guarantor['clientExternalId'] = str(row['client_external_id'])
                if row.get('staff_name') and not pd.isna(row.get('staff_name')):
                    guarantor['staffName'] = row['staff_name']
                if row.get('firstname') and not pd.isna(row.get('firstname')):
                    guarantor['firstname'] = row['firstname']
                if row.get('lastname') and not pd.isna(row.get('lastname')):
                    guarantor['lastname'] = row['lastname']
                if row.get('address_line_1') and not pd.isna(row.get('address_line_1')):
                    guarantor['addressLine1'] = row['address_line_1']
                if row.get('city') and not pd.isna(row.get('city')):
                    guarantor['city'] = row['city']
                if row.get('amount') and not pd.isna(row.get('amount')):
                    guarantor['amount'] = float(row['amount'])
                if row.get('savings_external_id') and not pd.isna(row.get('savings_external_id')):
                    guarantor['savingsExternalId'] = str(row['savings_external_id'])

                guarantors.append(guarantor)

            if guarantors:
                self.config['loanGuarantors'] = guarantors
                logger.info(f"  ✓ Loan Guarantors: {len(guarantors)} items")

        # 6. Teller Accounting Rules
        df = self._read_sheet('Teller Accounting Rules')
        if not df.empty:
            rules = []
            for _, row in df.iterrows():
                teller_name = row.get('teller_name')
                if not teller_name or pd.isna(teller_name):
                    continue

                rule = {
                    'tellerName': teller_name
                }
                if row.get('cash_in_gl_code') and not pd.isna(row.get('cash_in_gl_code')):
                    rule['cashInGlCode'] = str(row['cash_in_gl_code'])
                if row.get('cash_out_gl_code') and not pd.isna(row.get('cash_out_gl_code')):
                    rule['cashOutGlCode'] = str(row['cash_out_gl_code'])
                if row.get('description') and not pd.isna(row.get('description')):
                    rule['description'] = row['description']
                if row.get('office_name') and not pd.isna(row.get('office_name')):
                    rule['officeName'] = row['office_name']

                rules.append(rule)

            if rules:
                self.config['tellerAccountingRules'] = rules
                logger.info(f"  ✓ Teller Accounting Rules: {len(rules)} items")

        if not self.config.get('savingsTransactions') and not self.config.get('loanTransactions'):
            logger.info("  ℹ No transactions found (accounts may need to exist first)")

    def convert_all(self) -> Dict[str, Any]:
        """Convert all Excel sheets to YAML structure"""
        logger.info("╔" + "═" * 78 + "╗")
        logger.info("║" + " " * 22 + "EXCEL TO YAML CONVERTER" + " " * 33 + "║")
        logger.info("╚" + "═" * 78 + "╝")
        logger.info(f"\nExcel file: {self.excel_file}\n")

        self.convert_system_config()
        self.convert_security_organization()
        self.convert_accounting()
        self.convert_products()
        self.convert_operations()
        self.convert_transactions()


        # Convert Business Date
        df = self._read_sheet('Business Date')
        if not df.empty and len(df) > 0:
            row = df.iloc[0]
            # Parse the date string and convert to "dd MMMM yyyy" format
            import datetime
            date_str = row.get('date')
            if date_str and not pd.isna(date_str):
                # Handle different date formats
                if isinstance(date_str, str):
                    # Try parsing yyyy-MM-dd format
                    try:
                        parsed_date = datetime.datetime.strptime(date_str, '%Y-%m-%d')
                    except ValueError:
                        # Try parsing dd MMMM yyyy format
                        try:
                            parsed_date = datetime.datetime.strptime(date_str, '%d %B %Y')
                        except ValueError:
                            logger.warning(f"Could not parse business date: {date_str}")
                            parsed_date = datetime.datetime.now()
                elif isinstance(date_str, datetime.datetime):
                    parsed_date = date_str
                else:
                    parsed_date = datetime.datetime.now()
                
                # Convert to string format "dd MMMM yyyy" (e.g., "13 January 2026")
                formatted_date = parsed_date.strftime('%d %B %Y')
                self.config['businessDate'] = {
                    'type': row.get('type', 'BUSINESS_DATE'),
                    'date': formatted_date,
                    'dateFormat': 'dd MMMM yyyy',
                    'locale': row.get('locale', 'en')
                }
                logger.info(f"  ✓ Business Date: {formatted_date}")




        # Validate converted data
        self._validate_conversion()

        return self.config

    def _validate_conversion(self):
        """Validate the converted data for completeness"""
        logger.info("\n" + "=" * 80)
        logger.info("COMPREHENSIVE DATA VALIDATION")
        logger.info("=" * 80)

        validation_results = []
        warnings = []
        errors = []

        # Phase 1: System Configuration
        logger.info("\n📋 Phase 1: System Configuration")

        # 1.1 Currency
        currency = self.config.get('systemConfig', {}).get('currency', {})
        if currency and currency.get('code'):
            validation_results.append(('Currency', True, f"1 currency ({currency['code']})"))
            logger.info(f"  ✓ Currency: {currency['code']}")
        else:
            validation_results.append(('Currency', False, "Missing"))
            errors.append("❌ Currency configuration missing")

        # 1.2 Working Days
        working_days = self.config.get('systemConfig', {}).get('workingDays', {})
        if working_days and working_days.get('recurrence'):
            validation_results.append(('Working Days', True, "Configured"))
            logger.info(f"  ✓ Working Days: Configured")
        else:
            validation_results.append(('Working Days', False, "Missing"))
            errors.append("❌ Working Days configuration missing")

        # 1.3 Global Configuration
        global_config = self.config.get('systemConfig', {}).get('globalConfig', [])
        if len(global_config) > 0:
            validation_results.append(('Global Config', True, f"{len(global_config)} settings"))
            logger.info(f"  ✓ Global Config: {len(global_config)} settings")
        else:
            validation_results.append(('Global Config', False, "Empty"))
            warnings.append("⚠️  No global configurations found")

        # 1.4 Codes and Values
        codes = self.config.get('systemConfig', {}).get('codes', [])
        empty_codes = [c['name'] for c in codes if len(c.get('values', [])) == 0]
        total_values = sum(len(c.get('values', [])) for c in codes)
        if empty_codes:
            validation_results.append(('Codes', False, f"{len(codes)} codes, {len(empty_codes)} empty"))
            errors.append(f"❌ CRITICAL: {len(empty_codes)} codes have no values: {', '.join(empty_codes)}")
        elif len(codes) > 0:
            validation_results.append(('Codes', True, f"{len(codes)} codes, {total_values} values"))
            logger.info(f"  ✓ Codes: {len(codes)} codes with {total_values} total values")
        else:
            validation_results.append(('Codes', False, "Empty"))
            warnings.append("⚠️  No codes defined")

        # 1.5 Account Number Preferences
        account_prefs = self.config.get('systemConfig', {}).get('accountNumberPreferences', [])
        if len(account_prefs) > 0:
            validation_results.append(('Account Preferences', True, f"{len(account_prefs)} types"))
            logger.info(f"  ✓ Account Preferences: {len(account_prefs)} types")
        else:
            validation_results.append(('Account Preferences', False, "Empty"))
            warnings.append("⚠️  No account number preferences")

        # 1.6 Notification Templates
        templates = self.config.get('systemConfig', {}).get('notificationTemplates', [])
        missing_body = [t['name'] for t in templates if not t.get('messageBody')]
        if missing_body:
            validation_results.append(('Notification Templates', False, f"{len(templates)} templates, {len(missing_body)} incomplete"))
            errors.append(f"❌ {len(missing_body)} templates missing message body: {', '.join(missing_body[:5])}")
        elif len(templates) > 0:
            validation_results.append(('Notification Templates', True, f"{len(templates)} templates"))
            logger.info(f"  ✓ Notification Templates: {len(templates)} complete templates")
        else:
            validation_results.append(('Notification Templates', False, "Empty"))
            warnings.append("⚠️  No notification templates")

        # 1.7 Data Tables
        data_tables = self.config.get('systemConfig', {}).get('dataTables', [])
        empty_tables = [t['name'] for t in data_tables if len(t.get('columns', [])) == 0]
        total_columns = sum(len(t.get('columns', [])) for t in data_tables)
        if empty_tables:
            validation_results.append(('Data Tables', False, f"{len(data_tables)} tables, {len(empty_tables)} empty"))
            errors.append(f"❌ CRITICAL: {len(empty_tables)} data tables have no columns")
        elif len(data_tables) > 0:
            validation_results.append(('Data Tables', True, f"{len(data_tables)} tables, {total_columns} columns"))
            logger.info(f"  ✓ Data Tables: {len(data_tables)} tables with {total_columns} columns")
        else:
            validation_results.append(('Data Tables', False, "Empty"))
            warnings.append("⚠️  No data tables defined")

        # 1.8 Scheduler Jobs
        scheduler_jobs = self.config.get('systemConfig', {}).get('schedulerJobs', [])
        if len(scheduler_jobs) > 0:
            validation_results.append(('Scheduler Jobs', True, f"{len(scheduler_jobs)} jobs"))
            logger.info(f"  ✓ Scheduler Jobs: {len(scheduler_jobs)} jobs")
        else:
            validation_results.append(('Scheduler Jobs', False, "Empty"))
            warnings.append("⚠️  No scheduler jobs configured")

        # 1.9 Maker-Checker Config
        maker_checker = self.config.get('systemConfig', {}).get('makerCheckerConfig', [])
        if len(maker_checker) > 0:
            validation_results.append(('Maker-Checker Config', True, f"{len(maker_checker)} rules"))
            logger.info(f"  ✓ Maker-Checker Config: {len(maker_checker)} rules")
        else:
            validation_results.append(('Maker-Checker Config', False, "Empty"))
            warnings.append("⚠️  No maker-checker rules configured")

        # 1.10 SMS/Email Configuration
        sms_email = self.config.get('systemConfig', {}).get('smsEmailConfig', [])
        if len(sms_email) > 0:
            validation_results.append(('SMS/Email Config', True, f"{len(sms_email)} settings"))
            logger.info(f"  ✓ SMS/Email Config: {len(sms_email)} settings")
        else:
            validation_results.append(('SMS/Email Config', False, "Empty"))
            warnings.append("⚠️  No SMS/Email configuration")

        # 1.11 Holidays
        holidays = self.config.get('systemConfig', {}).get('holidays', [])
        if len(holidays) > 0:
            validation_results.append(('Holidays', True, f"{len(holidays)} holidays"))
            logger.info(f"  ✓ Holidays: {len(holidays)} holidays")
        else:
            validation_results.append(('Holidays', False, "Empty"))
            # Not a warning - holidays are optional

        # Phase 2: Security & Organization
        logger.info("\n👥 Phase 2: Security & Organization")

        # 2.1 Offices
        offices = self.config.get('offices', [])
        missing_fields = []
        for o in offices:
            if not o.get('name'):
                missing_fields.append('name')
            if not o.get('openingDate'):
                missing_fields.append('openingDate')

        if len(offices) > 0 and not missing_fields:
            validation_results.append(('Offices', True, f"{len(offices)} offices"))
            logger.info(f"  ✓ Offices: {len(offices)} offices")
        elif missing_fields:
            validation_results.append(('Offices', False, f"{len(offices)} offices, missing fields"))
            errors.append(f"❌ Offices missing required fields: {set(missing_fields)}")
        else:
            validation_results.append(('Offices', False, "Empty"))
            warnings.append("⚠️  No offices defined")

        # 2.2 Roles
        roles = self.config.get('roles', [])
        roles_no_perms = [r['name'] for r in roles if not r.get('permissions')]
        if roles_no_perms:
            validation_results.append(('Roles', False, f"{len(roles)} roles, {len(roles_no_perms)} without permissions"))
            errors.append(f"❌ {len(roles_no_perms)} roles have no permissions")
        elif len(roles) > 0:
            total_perms = sum(len(r.get('permissions', [])) for r in roles)
            validation_results.append(('Roles', True, f"{len(roles)} roles, {total_perms} permissions"))
            logger.info(f"  ✓ Roles: {len(roles)} roles with {total_perms} permissions")
        else:
            validation_results.append(('Roles', False, "Empty"))
            warnings.append("⚠️  No roles defined")

        # 2.3 Staff
        staff = self.config.get('staff', [])
        staff_missing = []
        for s in staff:
            if not s.get('firstName') or not s.get('lastName'):
                staff_missing.append(s.get('firstName', 'Unknown'))

        if len(staff) > 0 and not staff_missing:
            validation_results.append(('Staff', True, f"{len(staff)} members"))
            logger.info(f"  ✓ Staff: {len(staff)} members")
        elif staff_missing:
            validation_results.append(('Staff', False, f"{len(staff)} members, {len(staff_missing)} incomplete"))
            errors.append(f"❌ {len(staff_missing)} staff members missing name")
        else:
            validation_results.append(('Staff', False, "Empty"))
            warnings.append("⚠️  No staff members defined")

        # Phase 3: Accounting
        logger.info("\n💰 Phase 3: Accounting Foundation")

        # 3.1 Chart of Accounts
        gl_accounts = self.config.get('chartOfAccounts', [])
        gl_missing = []
        for gl in gl_accounts:
            if not gl.get('glCode') or not gl.get('name'):
                gl_missing.append(gl.get('glCode', 'Unknown'))

        if len(gl_accounts) > 0 and not gl_missing:
            validation_results.append(('Chart of Accounts', True, f"{len(gl_accounts)} accounts"))
            logger.info(f"  ✓ Chart of Accounts: {len(gl_accounts)} accounts")
        elif gl_missing:
            validation_results.append(('Chart of Accounts', False, f"{len(gl_accounts)} accounts, {len(gl_missing)} incomplete"))
            errors.append(f"❌ {len(gl_missing)} GL accounts missing required fields")
        else:
            validation_results.append(('Chart of Accounts', False, "Empty"))
            errors.append("❌ CRITICAL: No GL accounts defined")

        # 3.2 Payment Types
        payment_types = self.config.get('paymentTypes', [])
        if len(payment_types) > 0:
            validation_results.append(('Payment Types', True, f"{len(payment_types)} types"))
            logger.info(f"  ✓ Payment Types: {len(payment_types)} types")
        else:
            validation_results.append(('Payment Types', False, "Empty"))
            warnings.append("⚠️  No payment types defined")

        # 3.3 Fund Sources
        fund_sources = self.config.get('fundSources', [])
        if len(fund_sources) > 0:
            validation_results.append(('Fund Sources', True, f"{len(fund_sources)} sources"))
            logger.info(f"  ✓ Fund Sources: {len(fund_sources)} sources")
        else:
            validation_results.append(('Fund Sources', False, "Empty"))
            warnings.append("⚠️  No fund sources defined")

        # 3.4 Financial Activity Mappings
        activity_mappings = self.config.get('financialActivityMappings', [])
        if len(activity_mappings) > 0:
            validation_results.append(('Financial Activities', True, f"{len(activity_mappings)} mappings"))
            logger.info(f"  ✓ Financial Activities: {len(activity_mappings)} mappings")
        else:
            validation_results.append(('Financial Activities', False, "Empty"))
            warnings.append("⚠️  No financial activity mappings")

        # 3.5 Tax Groups
        tax_groups = self.config.get('taxGroups', [])
        if len(tax_groups) > 0:
            total_components = sum(len(g.get('taxComponents', [])) for g in tax_groups)
            validation_results.append(('Tax Groups', True, f"{len(tax_groups)} groups, {total_components} components"))
            logger.info(f"  ✓ Tax Groups: {len(tax_groups)} groups, {total_components} components")
        else:
            validation_results.append(('Tax Groups', False, "Empty"))
            warnings.append("⚠️  No tax groups defined")

        # 3.6 Tellers
        tellers = self.config.get('tellers', [])
        if len(tellers) > 0:
            validation_results.append(('Tellers', True, f"{len(tellers)} tellers"))
            logger.info(f"  ✓ Tellers: {len(tellers)} tellers")
        else:
            validation_results.append(('Tellers', False, "Empty"))
            # Not a warning - tellers are optional

        # 3.7 Teller Cashier Mappings
        teller_mappings = self.config.get('tellerCashierMappings', [])
        if len(teller_mappings) > 0:
            validation_results.append(('Teller Mappings', True, f"{len(teller_mappings)} mappings"))
            logger.info(f"  ✓ Teller Mappings: {len(teller_mappings)} mappings")
        else:
            validation_results.append(('Teller Mappings', False, "Empty"))
            # Not a warning - optional

        # Phase 4: Products
        logger.info("\n🏦 Phase 4: Financial Products")

        # 4.1 Charges
        charges = self.config.get('charges', [])
        if len(charges) > 0:
            validation_results.append(('Charges', True, f"{len(charges)} charges"))
            logger.info(f"  ✓ Charges: {len(charges)} charges")
        else:
            validation_results.append(('Charges', False, "Empty"))
            warnings.append("⚠️  No charges defined")

        # 4.2 Loan Products
        loan_products = self.config.get('loanProducts', [])
        loans_no_accounting = [p['name'] for p in loan_products if not p.get('accounting')]
        loans_incomplete = []
        for p in loan_products:
            if not p.get('shortName') or not p.get('currencyCode'):
                loans_incomplete.append(p.get('name', 'Unknown'))

        if loans_incomplete:
            validation_results.append(('Loan Products', False, f"{len(loan_products)} products, {len(loans_incomplete)} incomplete"))
            errors.append(f"❌ {len(loans_incomplete)} loan products missing required fields")
        elif loans_no_accounting:
            validation_results.append(('Loan Products', False, f"{len(loan_products)} products, {len(loans_no_accounting)} no accounting"))
            warnings.append(f"⚠️  {len(loans_no_accounting)} loan products missing GL mappings")
        elif len(loan_products) > 0:
            total_mappings = sum(len(p.get('accounting', {})) for p in loan_products)
            validation_results.append(('Loan Products', True, f"{len(loan_products)} products, {total_mappings} GL mappings"))
            logger.info(f"  ✓ Loan Products: {len(loan_products)} products with {total_mappings} GL mappings")
        else:
            validation_results.append(('Loan Products', False, "Empty"))
            warnings.append("⚠️  No loan products defined")

        # 4.3 Savings Products
        savings_products = self.config.get('savingsProducts', [])
        savings_no_accounting = [p['name'] for p in savings_products if not p.get('accounting')]
        savings_incomplete = []
        for p in savings_products:
            if not p.get('shortName') or not p.get('currencyCode'):
                savings_incomplete.append(p.get('name', 'Unknown'))

        if savings_incomplete:
            validation_results.append(('Savings Products', False, f"{len(savings_products)} products, {len(savings_incomplete)} incomplete"))
            errors.append(f"❌ {len(savings_incomplete)} savings products missing required fields")
        elif savings_no_accounting:
            validation_results.append(('Savings Products', False, f"{len(savings_products)} products, {len(savings_no_accounting)} no accounting"))
            warnings.append(f"⚠️  {len(savings_no_accounting)} savings products missing GL mappings")
        elif len(savings_products) > 0:
            total_mappings = sum(len(p.get('accounting', {})) for p in savings_products)
            validation_results.append(('Savings Products', True, f"{len(savings_products)} products, {total_mappings} GL mappings"))
            logger.info(f"  ✓ Savings Products: {len(savings_products)} products with {total_mappings} GL mappings")
        else:
            validation_results.append(('Savings Products', False, "Empty"))
            warnings.append("⚠️  No savings products defined")

        # 4.4 Delinquency Buckets
        delinquency_buckets = self.config.get('delinquencyBuckets', [])
        if len(delinquency_buckets) > 0:
            validation_results.append(('Delinquency Buckets', True, f"{len(delinquency_buckets)} buckets"))
            logger.info(f"  ✓ Delinquency Buckets: {len(delinquency_buckets)} buckets")
        else:
            validation_results.append(('Delinquency Buckets', False, "Empty"))
            warnings.append("⚠️  No delinquency buckets defined")

        # 4.5 Loan Provisioning
        loan_provisioning = self.config.get('loanProvisioning', [])
        if len(loan_provisioning) > 0:
            validation_results.append(('Loan Provisioning', True, f"{len(loan_provisioning)} categories"))
            logger.info(f"  ✓ Loan Provisioning: {len(loan_provisioning)} categories")
        else:
            validation_results.append(('Loan Provisioning', False, "Empty"))
            warnings.append("⚠️  No loan provisioning categories")

        # 4.6 Collateral Types
        collateral_types = self.config.get('collateralTypes', [])
        if len(collateral_types) > 0:
            validation_results.append(('Collateral Types', True, f"{len(collateral_types)} types"))
            logger.info(f"  ✓ Collateral Types: {len(collateral_types)} types")
        else:
            validation_results.append(('Collateral Types', False, "Empty"))
            # Not a warning - optional

        # 4.7 Guarantor Types
        guarantor_types = self.config.get('guarantorTypes', [])
        if len(guarantor_types) > 0:
            validation_results.append(('Guarantor Types', True, f"{len(guarantor_types)} types"))
            logger.info(f"  ✓ Guarantor Types: {len(guarantor_types)} types")
        else:
            validation_results.append(('Guarantor Types', False, "Empty"))
            # Not a warning - optional

        # 4.8 Floating Rates
        floating_rates = self.config.get('floatingRates', [])
        if len(floating_rates) > 0:
            validation_results.append(('Floating Rates', True, f"{len(floating_rates)} rates"))
            logger.info(f"  ✓ Floating Rates: {len(floating_rates)} rates")
        else:
            validation_results.append(('Floating Rates', False, "Empty"))
            # Not a warning - optional

        # 4.9 Payment Type Accounting
        payment_type_accounting = self.config.get('paymentTypeAccountingMappings', [])
        if len(payment_type_accounting) > 0:
            validation_results.append(('Payment Type Accounting', True, f"{len(payment_type_accounting)} mappings"))
            logger.info(f"  ✓ Payment Type Accounting: {len(payment_type_accounting)} mappings")
        else:
            validation_results.append(('Payment Type Accounting', False, "Empty"))
            # Not a warning - optional

        # Phase 5: Operations
        logger.info("\n👤 Phase 5: Client Operations")

        # 5.1 Clients
        clients = self.config.get('clients', [])
        clients_incomplete = []
        for c in clients:
            if not c.get('firstName') or not c.get('lastName') or not c.get('officeName'):
                clients_incomplete.append(f"{c.get('firstName', '')} {c.get('lastName', '')}")

        if clients_incomplete:
            validation_results.append(('Clients', False, f"{len(clients)} clients, {len(clients_incomplete)} incomplete"))
            errors.append(f"❌ {len(clients_incomplete)} clients missing required fields")
        elif len(clients) > 0:
            validation_results.append(('Clients', True, f"{len(clients)} clients"))
            logger.info(f"  ✓ Clients: {len(clients)} clients")
        else:
            validation_results.append(('Clients', False, "Empty"))
            warnings.append("⚠️  No clients defined")

        # 5.2 Groups
        groups = self.config.get('groups', [])
        if len(groups) > 0:
            validation_results.append(('Groups', True, f"{len(groups)} groups"))
            logger.info(f"  ✓ Groups: {len(groups)} groups")
        else:
            validation_results.append(('Groups', False, "Empty"))
            # Not a warning - groups are optional

        # 5.3 Centers
        centers = self.config.get('centers', [])
        if len(centers) > 0:
            validation_results.append(('Centers', True, f"{len(centers)} centers"))
            logger.info(f"  ✓ Centers: {len(centers)} centers")
        else:
            validation_results.append(('Centers', False, "Empty"))
            # Not a warning - centers are optional

        # Calculate overall statistics
        total_checks = len(validation_results)
        passed_checks = sum(1 for _, passed, _ in validation_results if passed)
        completeness_pct = (passed_checks / total_checks * 100) if total_checks > 0 else 0

        # Summary Table
        logger.info("\n" + "=" * 80)
        logger.info("VALIDATION SUMMARY")
        logger.info("=" * 80)

        logger.info(f"\n{'Entity Type':<30} {'Status':<10} {'Details':<40}")
        logger.info("-" * 80)
        for entity, passed, details in validation_results:
            status = "✓ Pass" if passed else "✗ Fail"
            logger.info(f"{entity:<30} {status:<10} {details:<40}")

        logger.info("=" * 80)

        # Print warnings
        if warnings:
            logger.info("\n⚠️  WARNINGS:")
            for warning in warnings:
                logger.warning(f"  {warning}")

        # Print errors
        if errors:
            logger.info("\n❌ ERRORS:")
            for error in errors:
                logger.error(f"  {error}")

        # Final Summary
        logger.info("\n" + "=" * 80)
        if errors:
            logger.error(f"❌ Data Completeness: {completeness_pct:.1f}% ({passed_checks}/{total_checks} checks passed)")
            logger.error(f"   {len(errors)} critical error(s), {len(warnings)} warning(s)")
            logger.error("   Conversion completed but output may be incomplete. Review errors above.")
        elif warnings:
            logger.warning(f"⚠️  Data Completeness: {completeness_pct:.1f}% ({passed_checks}/{total_checks} checks passed)")
            logger.warning(f"   {len(warnings)} warning(s) found")
            logger.info("   Conversion completed successfully with minor issues.")
        else:
            logger.info(f"✅ Data Completeness: {completeness_pct:.1f}% ({passed_checks}/{total_checks} checks passed)")
            logger.info("   All validation checks passed! Complete data transfer verified.")

        logger.info("=" * 80)

    def write_yaml(self, output_file: str):
        """Write configuration to YAML file"""
        logger.info(f"\nWriting YAML to: {output_file}")

        with open(output_file, 'w') as f:
            yaml.dump(
                self.config,
                f,
                default_flow_style=False,
                sort_keys=False,
                allow_unicode=True,
                width=100
            )

        logger.info("✅ Conversion completed successfully!")
        logger.info(f"\nYou can now import this YAML file using fineract-config-cli:")
        logger.info(f"  java -jar fineract-config-cli.jar import --file {output_file}")


def main():
    parser = argparse.ArgumentParser(
        description='Convert Fineract Excel template to YAML configuration',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  # Basic conversion
  python3 excel_to_yaml.py -i ../output/demo_data.xlsx -o demo-config.yml

  # Convert with validation
  python3 excel_to_yaml.py -i data.xlsx -o config.yml --validate

Usage with fineract-config-cli:
  1. Convert Excel to YAML:
     python3 excel_to_yaml.py -i demo_data.xlsx -o demo-config.yml

  2. Import to Fineract:
     cd ../../fineract-config-cli
     java -jar target/fineract-config-cli.jar import --file demo-config.yml
        '''
    )

    parser.add_argument(
        '-i', '--input',
        required=True,
        help='Input Excel file path'
    )

    parser.add_argument(
        '-o', '--output',
        required=True,
        help='Output YAML file path'
    )

    parser.add_argument(
        '--validate',
        action='store_true',
        help='Validate Excel structure before conversion'
    )

    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be converted without writing output'
    )

    args = parser.parse_args()

    # Validate input file exists
    if not os.path.exists(args.input):
        logger.error(f"Error: Input file not found: {args.input}")
        sys.exit(1)

    try:
        # Create converter
        converter = ExcelToYamlConverter(args.input)

        # Convert
        config = converter.convert_all()

        # Show summary
        logger.info("\n" + "=" * 80)
        logger.info("CONVERSION SUMMARY")
        logger.info("=" * 80)
        logger.info(f"Tenant: {config.get('tenant')}")
        if 'systemConfig' in config:
            logger.info(f"System Config sections: {len(config['systemConfig'])}")
        if 'offices' in config:
            logger.info(f"Offices: {len(config['offices'])}")
        if 'roles' in config:
            logger.info(f"Roles: {len(config['roles'])}")
        if 'staff' in config:
            logger.info(f"Staff: {len(config['staff'])}")
        if 'users' in config:
            logger.info(f"Users: {len(config['users'])}")
        if 'chartOfAccounts' in config:
            logger.info(f"GL Accounts: {len(config['chartOfAccounts'])}")
        if 'charges' in config:
            logger.info(f"Charges: {len(config['charges'])}")
        if 'loanProducts' in config:
            logger.info(f"Loan Products: {len(config['loanProducts'])}")
        if 'savingsProducts' in config:
            logger.info(f"Savings Products: {len(config['savingsProducts'])}")
        if 'clients' in config:
            logger.info(f"Clients: {len(config['clients'])}")
        if 'groups' in config:
            logger.info(f"Groups: {len(config['groups'])}")
        if 'centers' in config:
            logger.info(f"Centers: {len(config['centers'])}")
        logger.info("=" * 80)

        # Write output
        if not args.dry_run:
            converter.write_yaml(args.output)
        else:
            logger.info("\n[DRY RUN] No file written")

    except Exception as e:
        logger.error(f"Conversion failed: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
