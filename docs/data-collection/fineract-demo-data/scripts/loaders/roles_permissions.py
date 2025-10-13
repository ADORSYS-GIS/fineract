#!/usr/bin/env python3
"""
Roles and Permissions Loader
Handles loading of custom roles with granular permissions
"""

import logging
import time
from typing import Dict, List
import pandas as pd

logger = logging.getLogger(__name__)


class RolesPermissionsLoader:
    """Loads custom roles and permissions into Fineract"""

    def __init__(self, client, excel_file: str, config: Dict):
        self.client = client
        self.excel_file = excel_file
        self.config = config
        self.permission_cache = {}  # Cache for permission code to ID mapping

    def _read_excel_sheet(self, sheet_name: str) -> pd.DataFrame:
        """Read specific sheet from Excel"""
        df = pd.read_excel(self.excel_file, sheet_name=sheet_name)
        df = df.where(pd.notnull(df), None)
        return df

    def _fetch_all_permissions(self):
        """Fetch all available permissions from Fineract and cache them"""
        if self.permission_cache:
            return self.permission_cache

        try:
            logger.info("Fetching all available permissions from Fineract...")
            # Get permissions from the /permissions endpoint
            permissions_response = self.client.get('/permissions')

            # Build a cache: permission code -> permission object
            for perm in permissions_response:
                code = perm.get('code')
                if code:
                    self.permission_cache[code] = perm

            logger.info(f"Cached {len(self.permission_cache)} permissions")
            return self.permission_cache

        except Exception as e:
            logger.error(f"Failed to fetch permissions: {str(e)}")
            return {}

    def _get_permissions_for_group(self, permission_group: str, permission_level: str) -> List[str]:
        """
        Map simplified permission group and level to Fineract permission codes.

        Args:
            permission_group: e.g., 'Client', 'Loan', 'Savings', 'Transaction', etc.
            permission_level: e.g., 'ALL_FUNCTIONS', 'CREATE_UPDATE_READ', 'CREATE_READ', 'READ', 'APPROVE'

        Returns:
            List of Fineract permission codes
        """
        permissions = []

        # Fetch all available permissions
        all_perms = self._fetch_all_permissions()
        if not all_perms:
            logger.warning("No permissions available from Fineract")
            return permissions

        # Entity name mapping
        entity_mapping = {
            'Client': 'CLIENT',
            'Loan': 'LOAN',
            'Savings': 'SAVINGSACCOUNT',
            'Transaction': 'TRANSACTION',
            'Report': 'REPORT',
            'Office': 'OFFICE',
            'Staff': 'STAFF',
            'Accounting': 'JOURNALENTRY',
            'Teller': 'TELLER',
            'Maker-Checker': 'CHECKER'
        }

        entity_name = entity_mapping.get(permission_group)
        if not entity_name:
            logger.warning(f"Unknown permission group: {permission_group}")
            return permissions

        # Action mapping based on permission level
        if permission_level == 'ALL_FUNCTIONS':
            # Include all CRUD operations
            actions = ['CREATE', 'READ', 'UPDATE', 'DELETE', 'APPROVE', 'DISBURSE', 'ACTIVATE',
                      'WITHDRAW', 'REJECT', 'SAVEORUPDATE']
        elif permission_level == 'CREATE_UPDATE_READ':
            actions = ['CREATE', 'READ', 'UPDATE', 'SAVEORUPDATE']
        elif permission_level == 'CREATE_READ':
            actions = ['CREATE', 'READ']
        elif permission_level == 'READ':
            actions = ['READ']
        elif permission_level == 'APPROVE':
            actions = ['APPROVE', 'APPROVALMATRIX']
        elif permission_level == 'CREATE':
            actions = ['CREATE']
        else:
            logger.warning(f"Unknown permission level: {permission_level}")
            return permissions

        # Find matching permissions
        for code, perm in all_perms.items():
            perm_entity = perm.get('entityName', '').upper()
            perm_action = perm.get('actionName', '').upper()

            # Match entity and action
            if entity_name in perm_entity or perm_entity in entity_name:
                if any(action in perm_action for action in actions):
                    permissions.append(code)

        # Special handling for special permission groups
        if permission_group == 'Maker-Checker':
            # Add checker inbox and approval permissions
            for code in all_perms.keys():
                if 'CHECKER' in code.upper() or 'APPROVAL' in code.upper():
                    if permission_level == 'APPROVE' and 'READ' not in code.upper():
                        permissions.append(code)
                    elif permission_level == 'CREATE' and 'READ' in code.upper():
                        permissions.append(code)

        return list(set(permissions))  # Remove duplicates

    def load_roles_permissions(self):
        """Create roles with custom permissions"""
        logger.info("=" * 80)
        logger.info("LOADING ROLES AND PERMISSIONS")
        logger.info("=" * 80)

        df = self._read_excel_sheet('Roles Permissions')

        # Get existing roles
        try:
            existing_roles_response = self.client.get('/roles')
            existing_roles = {role['name']: role['id'] for role in existing_roles_response}
            logger.info(f"Found {len(existing_roles)} existing roles in Fineract")
        except Exception as e:
            logger.warning(f"Could not retrieve existing roles: {str(e)}")
            existing_roles = {}

        # Group permissions by role
        roles_data = {}
        for _, row in df.iterrows():
            role_name = row['role_name']
            if role_name not in roles_data:
                roles_data[role_name] = {
                    'name': role_name,
                    'description': f"{role_name} with custom permissions",
                    'permissions': []
                }

            # Get permissions for this group and level
            permission_codes = self._get_permissions_for_group(
                row['permission_group'],
                row['permission']
            )

            roles_data[role_name]['permissions'].extend(permission_codes)

            if permission_codes:
                logger.info(f"  Mapped {row['permission_group']}.{row['permission']} → {len(permission_codes)} permissions")

        # Create or update roles
        for role_name, role_data in roles_data.items():
            try:
                # Remove duplicates from permissions
                role_data['permissions'] = list(set(role_data['permissions']))

                if role_name in existing_roles:
                    role_id = existing_roles[role_name]
                    logger.info(f"⊙ Role already exists: {role_name} (ID: {role_id})")

                    # Update permissions for existing role
                    self._update_role_permissions(role_id, role_name, role_data['permissions'])
                else:
                    # Create new role
                    create_data = {
                        'name': role_name,
                        'description': role_data['description']
                    }

                    response = self.client.post('/roles', create_data)
                    role_id = response.get('resourceId')

                    logger.info(f"✓ Created role: {role_name} (ID: {role_id})")
                    time.sleep(0.3)

                    # Set permissions for new role
                    self._update_role_permissions(role_id, role_name, role_data['permissions'])

            except Exception as e:
                logger.error(f"✗ Failed to create/update role {role_name}: {str(e)}")

        logger.info("")
        logger.info("NOTE: Role permissions have been configured based on permission mappings.")
        logger.info("      Review roles in Fineract Admin UI: Admin → System → Manage Roles")

    def _update_role_permissions(self, role_id: int, role_name: str, permission_codes: List[str]):
        """Update permissions for a role"""
        try:
            if not permission_codes:
                logger.warning(f"  ⚠ No permissions to assign for role: {role_name}")
                return

            # Build permissions payload
            # Fineract expects: {"permissions": {"<permission_code>": true, ...}}
            permissions_dict = {code: True for code in permission_codes}

            data = {
                'permissions': permissions_dict
            }

            self.client.put(f'/roles/{role_id}/permissions', data)
            logger.info(f"  ✓ Assigned {len(permission_codes)} permissions to {role_name}")
            time.sleep(0.3)

        except Exception as e:
            logger.warning(f"  ⚠ Failed to update permissions for {role_name}: {str(e)}")
