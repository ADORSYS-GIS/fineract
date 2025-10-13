#!/usr/bin/env python3
"""
System Configuration Loader
Handles loading of system-wide configurations like currencies, working days, etc.
"""

import logging
import time
from datetime import datetime
from typing import Dict
import pandas as pd

logger = logging.getLogger(__name__)


class SystemConfigLoader:
    """Loads system configuration into Fineract"""

    def __init__(self, client, excel_file: str, config: Dict):
        self.client = client
        self.excel_file = excel_file
        self.config = config

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        # Replace NaN with None for proper JSON serialization
        df = df.where(pd.notnull(df), None)
        return df

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
            byday_string = ','.join(working_days) + ','  # Note: Fineract expects trailing comma
            recurrence_string = f"FREQ=WEEKLY;INTERVAL=1;BYDAY={byday_string}"

            # Update working days via API
            data = {
                'recurrence': recurrence_string,
                'repaymentRescheduleType': 2,
                'extendTermForDailyRepayments': False,
                'locale': 'en'
            }

            self.client.put('/workingdays', data)

            logger.info(f"✓ Working days configured: {', '.join(day_names)}")
            logger.info(f"  Recurrence rule: {recurrence_string}")

        except Exception as e:
            logger.warning(f"⚠ Working days configuration may require manual setup: {str(e)}")

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
                    error_str = str(e)
                    if '500' in error_str or 'Internal Server Error' in error_str:
                        logger.warning(f"  ⚠ Server error configuring {row['config_name']} (Fineract bug/incompatibility)")
                        logger.debug(f"    This configuration may not be supported in your Fineract version")
                    elif '404' in error_str:
                        logger.debug(f"  Configuration not available: {row['config_name']}")
                    else:
                        logger.warning(f"  ⚠ Failed to configure {row['config_name']}: {error_str}")

        except Exception as e:
            logger.error(f"✗ Failed to load global configuration: {str(e)}")

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
                        # Skip if code_value is None or empty
                        if not value_row['code_value'] or pd.isna(value_row['code_value']):
                            logger.debug(f"    Skipping empty/null code value")
                            continue

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
                        if 'already exists' in str(e).lower() or 'duplicate' in str(e).lower():
                            logger.debug(f"    Value already exists: {value_row['code_value']}")
                        elif 'mandatory' in str(e).lower():
                            logger.warning(f"    ⚠ Skipping invalid value (missing required field): {value_row['code_value']}")
                        else:
                            logger.warning(f"    ⚠ Failed to add value {value_row['code_value']}: {str(e)}")

            except Exception as e:
                logger.error(f"✗ Failed to create code {code_name}: {str(e)}")

    def load_account_number_preferences(self):
        """Configure account number generation preferences"""
        logger.info("=" * 80)
        logger.info("LOADING ACCOUNT NUMBER PREFERENCES")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Account Number Preferences')

        # Account number preferences in Fineract
        entity_type_mapping = {
            'Client': 1,
            'Loan': 2,
            'Savings': 3,
            'Groups': 4,
            'Centers': 5
        }

        prefix_type_mapping = {
            'Office Name': 1,
            'Office Short Name': 1,
            'Client Type': 101,
            'Loan Product Short Name': 201,
            'Savings Product Short Name': 301,
            'Product Short Name': 201
        }

        for idx, row in df.iterrows():
            try:
                entity_type_id = entity_type_mapping.get(row['entity_type'])
                prefix_type_id = prefix_type_mapping.get(row['prefix_type'], 0)

                if not entity_type_id:
                    continue

                # Auto-correct prefix type based on entity type if needed
                if row['entity_type'] == 'Savings' and 'Product' in row['prefix_type']:
                    prefix_type_id = 301
                elif row['entity_type'] == 'Loan' and 'Product' in row['prefix_type']:
                    prefix_type_id = 201

                data = {
                    'accountType': entity_type_id,
                    'prefixType': prefix_type_id
                }

                try:
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

    def load_scheduler_jobs(self):
        """Configure scheduler jobs"""
        logger.info("=" * 80)
        logger.info("LOADING SCHEDULER JOBS CONFIGURATION")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Scheduler Jobs')

        # Get existing jobs
        try:
            existing_jobs = self.client.get('/jobs')

            # Create a mapping of display names for easier lookup
            jobs_map = {job['displayName']: job for job in existing_jobs}

            logger.info(f"Found {len(existing_jobs)} existing jobs in Fineract")
            logger.debug("Available jobs in Fineract:")
            for job in sorted(existing_jobs, key=lambda x: x['displayName']):
                logger.debug(f"  - {job['displayName']} (ID: {job['jobId']})")

            configured_count = 0
            not_found_jobs = []

            for idx, row in df.iterrows():
                try:
                    # Find job by display name
                    job = jobs_map.get(row['display_name'])

                    if job:
                        job_id = job['jobId']
                        configured_count += 1

                        # Update job schedule if cron expression is provided
                        if row['cron_expression']:
                            update_data = {
                                'displayName': row['display_name'],
                                'cronExpression': row['cron_expression'],
                                'active': row['active'] == 'Yes'
                            }

                            self.client.put(f'/jobs/{job_id}', update_data)
                            logger.info(f"✓ Configured job: {row['display_name']} (ID: {job_id})")
                            logger.info(f"  Schedule: {row['cron_expression']}, Active: {row['active']}")
                        else:
                            logger.info(f"✓ Job found: {row['display_name']} (ID: {job_id})")

                    else:
                        logger.warning(f"  ⚠ Job not found in Fineract: {row['display_name']}")
                        not_found_jobs.append(row['display_name'])

                    time.sleep(0.2)

                except Exception as e:
                    logger.error(f"✗ Failed to configure job {row['display_name']}: {str(e)}")

            # Summary
            logger.info("")
            logger.info(f"✓ Successfully configured {configured_count}/{len(df)} scheduler jobs")

            if not_found_jobs:
                logger.info("")
                logger.info(f"⚠ {len(not_found_jobs)} job(s) not found in this Fineract version:")
                for job_name in not_found_jobs:
                    logger.info(f"  • {job_name}")
                logger.info("")
                logger.info("These jobs may be:")
                logger.info("  - Available under different names in your Fineract version")
                logger.info("  - Require specific plugins or modules to be enabled")
                logger.info("  - Not available in your Fineract version")
                logger.info("")
                logger.info(f"Available jobs in your Fineract instance ({len(existing_jobs)} total):")
                for job in sorted(existing_jobs, key=lambda x: x['displayName']):
                    logger.info(f"  • {job['displayName']}")

        except Exception as e:
            logger.error(f"✗ Failed to load scheduler jobs: {str(e)}")

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

        entity_mapping = {
            'Client': 0,
            'Loan': 1,
            'Savings': 2,
            'Group': 3
        }

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

                entity_id = entity_mapping.get(row.get('entity_type', 'Client'), 0)
                type_id = type_mapping.get(row['channel'], 0)

                # Build mappers for variable substitution
                mappers = []

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

                template_data = {
                    'entity': entity_id,
                    'type': type_id,
                    'name': row['template_name'],
                    'text': row['message_body'],
                    'mappers': mappers
                }

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

        field_type_mapping = {
            'String': 'String',
            'Number': 'Number',
            'Decimal': 'Decimal',
            'Date': 'Date',
            'Datetime': 'Datetime',
            'Text': 'Text',
            'Dropdown': 'Dropdown'
        }

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
                        length = 19
                    elif field_type in ['Dropdown']:
                        length = 100
                    else:
                        length = 0

                    column = {
                        'name': field['field_name'],
                        'type': field_type,
                        'mandatory': field['mandatory'] == 'Yes'
                    }

                    if length > 0:
                        column['length'] = length

                    if field['field_type'] == 'Dropdown' and field['dropdown_values']:
                        column['code'] = field['field_name'] + '_options'

                    columns.append(column)

                data_table_data = {
                    'datatableName': table_name,
                    'apptableName': app_table_name,
                    'multiRow': False,
                    'columns': columns
                }

                if entity_type == 'Client':
                    data_table_data['entitySubType'] = 'PERSON'

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
