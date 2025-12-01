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
