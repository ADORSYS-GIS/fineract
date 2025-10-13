#!/usr/bin/env python3
"""
Entity Loader
Handles loading of business entities like offices, staff, clients, etc.
"""

import logging
import time
from datetime import datetime
from typing import Dict
import pandas as pd

logger = logging.getLogger(__name__)


class EntityLoader:
    """Loads business entities into Fineract"""

    def __init__(self, client, excel_file: str, config: Dict):
        self.client = client
        self.excel_file = excel_file
        self.config = config

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        df = df.where(pd.notnull(df), None)
        return df

    def load_offices(self):
        """Create offices/branches"""
        logger.info("=" * 80)
        logger.info("LOADING OFFICES")
        logger.info("=" * 80)

        # Try to get existing offices from Fineract
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
            role_name_mapping = {
                'Branch Manager': 'Super user',
                'Loan Officer': 'Super user',
                'Cashier': 'Super user'
            }

            fineract_role_name = role_name_mapping.get(staff_row['role'], 'Super user')

            # Get available roles from Fineract
            if not hasattr(self.client, 'available_roles'):
                roles_response = self.client.get('/roles')
                self.client.available_roles = {role['name']: role['id'] for role in roles_response}
                logger.info(f"Found {len(self.client.available_roles)} roles in Fineract: {list(self.client.available_roles.keys())}")

            role_id = self.client.available_roles.get(fineract_role_name)

            if not role_id:
                logger.warning(f"Role '{fineract_role_name}' not found. Using first available role.")
                role_id = list(self.client.available_roles.values())[0] if self.client.available_roles else 1

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
                    'legalFormId': 1  # 1=PERSON, 2=ENTITY
                }

                if staff_id:
                    data['staffId'] = staff_id

                if row['date_of_birth']:
                    data['dateOfBirth'] = row['date_of_birth']

                response = self.client.post('/clients', data)
                client_id = response.get('resourceId') or response.get('clientId')

                self.client.created_clients[row['external_id']] = client_id

                logger.info(f"✓ Created client: {row['firstname']} {row['lastname']} (ID: {client_id})")
                time.sleep(0.5)

            except Exception as e:
                logger.error(f"✗ Failed to create client {row['firstname']} {row['lastname']}: {str(e)}")

    def load_tellers(self):
        """Create tellers (cash counters) at each branch"""
        logger.info("=" * 80)
        logger.info("LOADING TELLERS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Tellers')

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

        for _, row in df.iterrows():
            try:
                # Convert dates from yyyy-MM-dd to dd MMMM yyyy format
                date_str = row['date']
                date_obj = datetime.strptime(date_str, '%Y-%m-%d')
                formatted_date = date_obj.strftime('%-d %B %Y')

                # Convert rescheduled_to date
                rescheduled_to_str = row['rescheduled_to']
                rescheduled_to_obj = datetime.strptime(rescheduled_to_str, '%Y-%m-%d')
                formatted_rescheduled_to = rescheduled_to_obj.strftime('%-d %B %Y')

                data = {
                    'name': row['holiday_name'],
                    'fromDate': formatted_date,
                    'toDate': formatted_date,
                    'reschedulingType': 2,  # 2 = Reschedule to specific date
                    'repaymentsRescheduledTo': formatted_rescheduled_to,
                    'description': row.get('description', ''),
                    'offices': offices,
                    'dateFormat': 'dd MMMM yyyy',
                    'locale': 'en'
                }

                try:
                    response = self.client.post('/holidays', data)
                    holiday_id = response.get('resourceId') or response.get('holidayId')
                    logger.info(f"✓ Created holiday: {row['holiday_name']} ({date_str}) → rescheduled to {rescheduled_to_str} - ID: {holiday_id}")
                    time.sleep(0.3)
                except Exception as e:
                    if 'already exists' in str(e).lower() or 'duplicate' in str(e).lower():
                        logger.info(f"  ⊙ Holiday already exists: {row['holiday_name']} ({date_str})")
                    else:
                        raise

            except Exception as e:
                logger.error(f"✗ Failed to create holiday {row['holiday_name']}: {str(e)}")
