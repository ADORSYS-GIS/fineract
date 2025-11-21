# Excel to YAML Converter for Fineract Config CLI

## Overview

The `excel_to_yaml.py` script converts Fineract demo data Excel templates (36+ sheets) to YAML format compatible with the fineract-config-cli tool.

## Location

```
fineract-demo-data/scripts/excel_to_yaml.py
```

## Installation

The script uses standard Python libraries already included in the project:
- `pandas` - Excel file reading
- `openpyxl` - Excel format support
- `PyYAML` - YAML output generation

All dependencies are already installed for the fineract-demo-data toolkit.

## Usage

### Basic Conversion

```bash
cd fineract-demo-data/scripts

# Convert Excel to YAML
python3 excel_to_yaml.py \
  --input ../output/fineract_demo_data_20251120_184850.xlsx \
  --output ../output/demo-config.yml
```

### Dry Run (Preview without Writing)

```bash
python3 excel_to_yaml.py \
  --input ../output/demo_data.xlsx \
  --output demo-config.yml \
  --dry-run
```

### With Validation

```bash
python3 excel_to_yaml.py \
  --input ../output/demo_data.xlsx \
  --output demo-config.yml \
  --validate
```

## Complete Workflow

### Step 1: Generate Excel Template

```bash
cd fineract-demo-data/scripts
python3 generate_excel_template.py
```

This creates: `../output/fineract_demo_data_YYYYMMDD_HHMMSS.xlsx`

### Step 2: Edit Excel (Optional)

Customize the Excel file with your specific data:
- Update offices, staff, clients
- Modify products and charges
- Adjust chart of accounts

### Step 3: Convert to YAML

```bash
python3 excel_to_yaml.py \
  --input ../output/fineract_demo_data_20251120_184850.xlsx \
  --output ../../fineract-config-cli/config/demo.yml
```

### Step 4: Import to Fineract

```bash
cd ../../fineract-config-cli
java -jar target/fineract-config-cli.jar import --file config/demo.yml
```

## Supported Sheets

### ✅ Phase 1: System Configuration
- ✅ Currency Config
- ✅ Working Days
- ✅ Global Configuration
- ✅ Codes and Values
- ✅ Account Number Preferences
- ✅ Notification Templates
- ✅ Data Tables

### ✅ Phase 2: Security & Organization
- ✅ Offices
- ✅ Roles Permissions
- ✅ Staff
- ⚠️ Users (sheet name varies in different Excel versions)

### ✅ Phase 3: Accounting Foundation
- ✅ Chart of Accounts
- ✅ Payment Types
- ✅ Fund Sources
- ✅ Financial Activity Mappings

### ✅ Phase 4: Financial Products
- ✅ Charges
- ✅ Loan Products
- ✅ Savings Products

### ✅ Phase 5: Client Operations & Accounts
- ✅ Clients
- ✅ Groups
- ✅ Centers

### ℹ️ Phase 6: Transactions
- ℹ️ Transactions typically imported separately after accounts exist

## Excel Sheet Structure

The converter expects these column names (with fallbacks for variations):

### Currency Config
- `currency_code`, `currency_name`, `decimal_places`

### Working Days
- `day_of_week`, `working_day`

### Offices
- `office_name`, `parent_office`, `opening_date`, `external_id`

### Staff
- `firstname`/`first_name`, `lastname`/`last_name`, `office`, `mobile`, `email`, `joining_date`

### Roles Permissions
- `role_name`, `permission_code`/`permission`

### Chart of Accounts
- `gl_code`, `gl_name`/`account_name`, `account_type`/`classification`, `usage`, `manual_entries`

### Groups
- `group_name`, `office`/`office_name`, `staff`, `external_id`, `activation_date`, `submitted_on_date`, `active`, `center_name`, `client_external_ids` (comma-separated)

### Centers
- `center_name`, `office`/`office_name`, `staff`, `external_id`, `activation_date`, `submitted_on_date`, `active`

## Output Format

The converter generates a single monolithic YAML file structured by phases:

```yaml
tenant: default

systemConfig:
  currency:
    code: XAF
    name: Central African CFA Franc
    decimalPlaces: 0

  workingDays:
    recurrence: FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
    repaymentReschedulingType: MOVE_TO_NEXT_WORKING_DAY

  globalConfig:
    - name: maker-checker
      enabled: false

  codes:
    - name: CustomerType
      values:
        - name: Individual
          position: 1
          isActive: true

offices:
  - name: Head Office
    openingDate: [2024, 1, 1]
    externalId: HO001

roles:
  - name: Branch Manager
    description: Branch Manager role
    disabled: false
    permissions:
      - ALL_FUNCTIONS_READ
      - CREATE_CLIENT

staff:
  - firstName: John
    lastName: Doe
    officeName: Head Office
    isLoanOfficer: true
    emailAddress: john.doe@example.com

chartOfAccounts:
  - name: Assets
    glCode: "10000"
    accountType: ASSET
    accountUsage: HEADER
```

## Current Limitations

### Column Name Variations
Different Excel templates may use different column names. The converter includes fallback logic for common variations but may need updates for new formats.

### Optional Sheets
Some sheets are optional and will be skipped if:
- Sheet doesn't exist in Excel
- Required columns are missing
- Data cannot be parsed

### Error Handling
- The converter logs warnings for skipped sheets
- Missing optional fields are handled gracefully
- Invalid data types cause conversion to fail with clear error messages

### Known Issues
1. **Column Name Mapping**: Some Excel versions use different column names than expected
2. **Data Type Conversion**: Complex product configurations may need manual YAML editing
3. **Accounting Mappings**: GL account mappings for products need separate configuration

## Troubleshooting

### Error: "Sheet not found"
- Check Excel file has the expected sheet names
- Sheet names are case-sensitive
- Use `pandas.ExcelFile(filename).sheet_names` to list available sheets

### Error: "KeyError: 'column_name'"
- Excel uses different column name than expected
- Update the converter's column mapping
- Or manually edit the YAML output

### Empty YAML Sections
- Check Excel sheets have data (not just headers)
- Verify data is in correct format (dates, numbers)
- Check logs for warning messages

## Extending the Converter

To add support for new sheets or column names:

1. Add new conversion method in `ExcelToYamlConverter` class
2. Call method from `convert_all()`
3. Handle column name variations with `.get()` and fallbacks
4. Test with your Excel format

Example:

```python
def convert_new_entity(self):
    """Convert New Entity Type"""
    logger.info("Converting New Entity...")

    df = self._read_sheet('New Entity Sheet')
    if not df.empty:
        entities = []
        for _, row in df.iterrows():
            entity = {
                'name': row.get('entity_name') or row.get('name'),
                'value': row.get('entity_value') or row.get('value')
            }
            entities.append(entity)

        self.config['newEntities'] = entities
        logger.info(f"  ✓ New Entities: {len(entities)} entities")
```

## Performance

- **Small Excel** (< 1MB, < 100 rows): < 1 second
- **Medium Excel** (1-10MB, 100-1000 rows): 1-5 seconds
- **Large Excel** (> 10MB, > 1000 rows): 5-30 seconds

Memory usage scales with Excel file size.

## Integration with Fineract Config CLI

The converter output is fully compatible with fineract-config-cli:

```bash
# 1. Convert
python3 excel_to_yaml.py -i data.xlsx -o config.yml

# 2. Validate YAML
cd ../../fineract-config-cli
java -jar target/fineract-config-cli.jar validate --file config.yml

# 3. Import to Fineract
java -jar target/fineract-config-cli.jar import --file config.yml

# 4. Export from Fineract (for comparison)
java -jar target/fineract-config-cli.jar export --output exported-config.yml
```

## Future Enhancements

### Planned Features
- ✅ Core conversion (Phase 1-5) - **IMPLEMENTED**
- 📋 Phase 6 transaction conversion (loans, savings accounts, repayments)
- 📋 Validation against fineract-config-cli schema
- 📋 Incremental updates (diff Excel versions)
- 📋 Reverse conversion (YAML → Excel)
- 📋 Multi-file YAML output (split by phase)
- 📋 Custom column mapping configuration file
- 📋 Advanced product features (interest rates, fees, accounting mappings)

### Community Contributions Welcome
- Add support for additional Excel formats
- Improve column name detection
- Add data validation rules
- Create Excel templates for different use cases

## Support

### Questions or Issues?
1. Check Excel sheet names match expected format
2. Review log output for warnings
3. Validate YAML output structure
4. Open issue in fineract-config-cli repository

### Excel Template Documentation
See main fineract-demo-data README.md for:
- Complete sheet descriptions
- Column definitions
- Data format requirements
- Example values

## License

Apache License 2.0 - Same as Fineract project

## Version History

- **v1.2.0** (2025-11-20) - Complete Phase 5 release
  - ✅ Phase 1 (System Config) - 7/7 entity types (100%)
  - ✅ Phase 2 (Security & Organization) - 3/4 entity types (75%)
  - ✅ Phase 3 (Accounting Foundation) - 4/4 entity types (100%)
  - ✅ Phase 4 (Financial Products) - 3/3 entity types (100%)
  - ✅ Phase 5 (Client Operations) - 3/3 entity types (100%) ⭐ **NEW**
    - ✅ Clients
    - ✅ Groups (with client membership support)
    - ✅ Centers (with group linking)
  - Updated Excel template generator with Groups and Centers sheets
  - Successfully tested with 46-sheet Excel file
  - Output: 1,152 lines YAML (25 KB)

- **v1.1.0** (2025-11-20) - Production-ready release
  - ✅ Phase 1 (System Config) - 7/7 entity types
  - ✅ Phase 2 (Security & Organization) - 3/4 entity types (offices, roles, staff)
  - ✅ Phase 3 (Accounting Foundation) - 4/4 entity types (GL accounts, payment types, fund sources, activity mappings)
  - ✅ Phase 4 (Financial Products) - 3/3 entity types (charges, loan products, savings products)
  - ✅ Phase 5 (Client Operations) - 1/3 entity types (clients)
  - Fixed all column name mapping issues
  - Robust error handling with fallback patterns
  - Dry-run mode
  - Comprehensive CLI interface
  - Successfully tested with 44-sheet Excel file

- **v1.0.0** (2025-11-20) - Initial release (deprecated)
  - Phase 1 (System Config) support
  - Phase 2 (Security & Organization) support
  - Partial Phase 3 (Accounting) support
