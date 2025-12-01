#!/usr/bin/env python3
"""
Fineract API Client
Handles HTTP communication with Fineract REST API
"""

import requests
import json
import logging
from typing import Dict, Optional

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

    def upsert(self, endpoint: str, data: Dict, search_field: str, search_value: str) -> Dict:
        """
        Update entity if exists, create if not (upsert operation)

        Args:
            endpoint: API endpoint (e.g., '/loanproducts')
            data: Payload to create/update
            search_field: Field to search by (e.g., 'shortName', 'externalId', 'name', 'glCode')
            search_value: Value to search for

        Returns:
            API response from PUT (update) or POST (create)
        """
        try:
            # Search for existing entity
            logger.debug(f"Searching for existing entity: {search_field}={search_value}")

            # Try to find existing entity
            try:
                # For most endpoints, we can search with query params
                search_params = {search_field: search_value}
                existing_list = self.get(endpoint, params=search_params)

                # Handle different response formats
                if isinstance(existing_list, list) and len(existing_list) > 0:
                    existing = existing_list[0]
                elif isinstance(existing_list, dict):
                    # Some endpoints return dict with data array
                    if 'pageItems' in existing_list and len(existing_list['pageItems']) > 0:
                        existing = existing_list['pageItems'][0]
                    elif search_field in existing_list:
                        existing = existing_list
                    else:
                        existing = None
                else:
                    existing = None

            except Exception as search_error:
                logger.debug(f"Search failed: {search_error}. Will attempt to create.")
                existing = None

            if existing:
                # Update existing entity
                entity_id = existing.get('id')
                if entity_id:
                    logger.info(f"→ Updating existing entity: {search_value} (ID: {entity_id})")
                    return self.put(f'{endpoint}/{entity_id}', data)
                else:
                    logger.warning(f"Found entity but no ID field. Creating new entity.")
                    return self.post(endpoint, data)
            else:
                # Create new entity
                logger.info(f"→ Creating new entity: {search_value}")
                return self.post(endpoint, data)

        except Exception as e:
            logger.error(f"Upsert failed for {search_field}={search_value}: {str(e)}")
            raise
