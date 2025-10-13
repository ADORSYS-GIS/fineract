# Refactored Demo Data Loader

The `load_demo_data.py` script has been refactored into multiple maintainable modules for better code organization.

## New File Structure

```
scripts/
├── fineract_client.py           # API client for Fineract HTTP communication
├── loaders/
│   ├── __init__.py               # Package initialization
│   ├── system_config.py          # System configuration loaders
│   ├── entities.py               # Business entities (offices, staff, clients)
│   ├── products.py               # Financial products (loans, savings, charges)
│   └── accounts.py               # Account loaders (loan & savings accounts)
├── load_demo_data_new.py         # New refactored main script
├── load_demo_data.py             # Original script (still functional)
└── load_demo_data_original.py   # Backup of original script
```

## Module Breakdown

### 1. `fineract_client.py` (97 lines)
- **Purpose**: HTTP client for Fineract API communication
- **Key Components**:
  - `FineractAPIClient` class
  - HTTP methods: GET, POST, PUT, DELETE
  - Entity storage dictionaries
  - Error handling and logging

### 2. `loaders/system_config.py` (~550 lines)
- **Purpose**: System-wide configurations
- **Functions**:
  - `load_currency_config()` - Configure currencies
  - `load_working_days()` - Set working days
  - `load_global_configuration()` - System settings
  - `load_codes_and_values()` - Dropdown configurations
  - `load_account_number_preferences()` - Account numbering
  - `load_scheduler_jobs()` - Scheduled tasks
  - `load_sms_email_config()` - Notification config
  - `load_notification_templates()` - Message templates
  - `load_data_tables()` - Custom fields

### 3. `loaders/entities.py` (~290 lines)
- **Purpose**: Business entities and organizational structure
- **Functions**:
  - `load_offices()` - Create office hierarchy
  - `load_staff()` - Create staff members
  - `_create_user_account()` - User account creation
  - `load_clients()` - Create clients
  - `load_tellers()` - Create teller/cashier stations
  - `load_holidays()` - Holiday calendar

### 4. `loaders/products.py` (~520 lines)
- **Purpose**: Financial products and accounting setup
- **Functions**:
  - `load_gl_accounts()` - Chart of accounts
  - `load_charges()` - Fees and charges
  - `load_fund_sources()` - Funding sources
  - `load_payment_types()` - Payment methods
  - `load_loan_products()` - Loan products with accounting
  - `load_savings_products()` - Savings products with accounting
  - `load_financial_activity_mappings()` - GL mappings
  - `enable_maker_checker()` - Dual authorization config

### 5. `loaders/accounts.py` (~185 lines)
- **Purpose**: Account creation and management
- **Functions**:
  - `load_savings_accounts()` - Create and activate savings accounts
  - `load_loan_accounts()` - Create, approve, and disburse loans

### 6. `load_demo_data_new.py` (~150 lines)
- **Purpose**: Main orchestration script
- **Responsibilities**:
  - Initialize all loaders
  - Coordinate loading sequence
  - Handle configuration
  - Summary reporting

## Benefits of Refactoring

1. **Maintainability**:
   - Each module has a clear, focused responsibility
   - Easy to locate and update specific functionality
   - Reduced file size (from 1734 lines to ~150 lines main + modules)

2. **Testability**:
   - Each loader can be tested independently
   - Mock API client for unit tests
   - Easier to debug issues

3. **Reusability**:
   - Import specific loaders as needed
   - Use loaders in different scripts
   - Build custom loading sequences

4. **Collaboration**:
   - Multiple developers can work on different modules
   - Clearer code ownership
   - Reduced merge conflicts

## Usage

### Using the New Refactored Version

```bash
# Same interface as original
python load_demo_data_new.py ../output/fineract_demo_data_20241010.xlsx ../config/fineract_config.json
```

### Using Specific Loaders Programmatically

```python
from fineract_client import FineractAPIClient
from loaders.entities import EntityLoader
import json

# Load config
with open('config.json', 'r') as f:
    config = json.load(f)

# Initialize client
client = FineractAPIClient(
    base_url=config['fineract_url'],
    username=config['username'],
    password=config['password']
)

# Use specific loader
entity_loader = EntityLoader(client, 'data.xlsx', config)
entity_loader.load_offices()
entity_loader.load_staff()
```

## Migration Path

1. **Phase 1**: Test the new version alongside the original
   - Both `load_demo_data.py` and `load_demo_data_new.py` work
   - Run parallel tests to verify identical behavior

2. **Phase 2**: Switch to new version
   - Update documentation and scripts to use `load_demo_data_new.py`
   - Keep `load_demo_data_original.py` as backup

3. **Phase 3**: Full migration
   - Rename `load_demo_data_new.py` to `load_demo_data.py`
   - Archive `load_demo_data_original.py`

## Testing

To verify the refactored code:

```bash
# Test imports
cd scripts
python3 -c "from loaders import *; print('All loaders imported successfully')"

# Test with actual data (requires running Fineract instance)
python load_demo_data_new.py ../output/fineract_demo_data_*.xlsx ../config/fineract_config.json
```

## Module Dependencies

```
load_demo_data_new.py
    ├── fineract_client.py
    └── loaders/
        ├── system_config.py (depends on fineract_client)
        ├── entities.py (depends on fineract_client)
        ├── products.py (depends on fineract_client)
        └── accounts.py (depends on fineract_client)
```

All loaders depend only on:
- `fineract_client.py` (API communication)
- `pandas` (Excel reading)
- Standard library (logging, time, typing)

## Future Enhancements

Potential improvements to the refactored structure:

1. **Add Base Loader Class**: Create a base class with common functionality
2. **Configuration Validation**: Add config schema validation
3. **Retry Logic**: Implement automatic retry for failed operations
4. **Progress Tracking**: Add progress bars or percentage completion
5. **Dry Run Mode**: Test loading sequence without making changes
6. **Selective Loading**: Allow loading specific modules only
7. **Rollback Support**: Implement transaction-like rollback on errors

## File Size Comparison

| File | Lines | Purpose |
|------|-------|---------|
| load_demo_data_original.py | 1,734 | Original monolithic script |
| load_demo_data_new.py | 150 | New main orchestrator |
| fineract_client.py | 97 | API client |
| loaders/system_config.py | 550 | System configuration |
| loaders/entities.py | 290 | Business entities |
| loaders/products.py | 520 | Financial products |
| loaders/accounts.py | 185 | Account management |
| **Total (new)** | **1,792** | **Modularized (6 files + orchestrator)** |

The total line count is similar, but the code is now organized into logical, maintainable modules.
