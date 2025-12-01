"""
Loaders package for Fineract demo data
"""

from .system_config import SystemConfigLoader
from .entities import EntityLoader
from .products import ProductLoader
from .accounts import AccountLoader

__all__ = ['SystemConfigLoader', 'EntityLoader', 'ProductLoader', 'AccountLoader']
