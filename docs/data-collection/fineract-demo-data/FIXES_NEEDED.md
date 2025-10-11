# Fineract Demo Data Loader - Remaining Fixes

## ✅ Completed Fixes

1. **SSL Certificate Verification** - Disabled for self-signed certificates (`load_demo_data.py:43-47`)
2. **Double URL Path** - Removed `/fineract-provider/api/v1` prefix from all endpoint calls
3. **GL Account Key Storage** - Now consistently uses string keys (`load_demo_data.py:300`)
4. **Codes API** - Removed unsupported `isSystem` parameter (`load_demo_data.py:1066-1068`)
5. **Charges API** - Added mandatory `chargePaymentMode` parameter (`load_demo_data.py:348`)

## 🚨 CRITICAL: Permission Issue (MUST FIX FIRST)

### 403 Forbidden - Cannot Create Offices

**Error**: `User does not have sufficient priviledges to act on the provided office`

**Root Cause**: The `mifos` user doesn't have permission to create offices in your Fineract instance.

**Impact**: Blocks ALL data loading because:
- No offices → No staff → No clients → No loans/savings

**Solutions** (choose one):

1. **Grant Permissions** (Recommended):
   ```
   - Log into Fineract UI as admin
   - Navigate to Admin → Users & Roles → Manage Roles
   - Find the role assigned to 'mifos' user
   - Grant "CREATE_OFFICE" permission
   - Save changes
   ```

2. **Use Admin User**:
   ```json
   // In config/fineract_config.json, change:
   "username": "admin",     // or your admin username
   "password": "admin_password"
   ```

3. **Pre-create Offices Manually**:
   ```
   - Log into Fineract UI
   - Navigate to Admin → Organization → Manage Offices
   - Create these 4 offices manually:
     * Head Office Yaounde (opening_date: 2024-01-01)
     * Douala Branch (parent: Head Office Yaounde, opening_date: 2024-01-15)
     * Bafoussam Branch (parent: Head Office Yaounde, opening_date: 2024-02-01)
     * Bamenda Branch (parent: Head Office Yaounde, opening_date: 2024-02-15)
   - Then run the loader script
   ```

## ⚠️ API Validation Errors (Fix after resolving permissions)

These errors will be fixed automatically in `generate_excel_template.py` to prevent them in future runs:

### 1. Savings Products - shortName Too Long

**Error**: `The parameter shortName exceeds max length of 4`

**Current Values**:
- `VOL-SAV` (7 chars) → `VSAV` (4 chars)
- `FIXED-DEP` (9 chars) → `FXDP` (4 chars)
- `MAND-GRP` (8 chars) → `MGRP` (4 chars)

**Fix Location**: `generate_excel_template.py` - Update Savings Products sheet generation

### 2. Loan Products - Unsupported Parameter

**Error**: `The parameter transactionProcessingStrategyId is not supported`

**Cause**: Your Fineract version uses a different parameter name for transaction processing strategy

**Fix Options**:
1. Remove the parameter entirely (use default strategy)
2. Use `transactionProcessingStrategyCode` instead with value like "mifos-standard-strategy"
3. Query `/loanproducts/template` endpoint to find correct parameter name

**Fix Location**: `load_demo_data.py:200` and `load_demo_data.py:598`

### 3. Data Tables - Missing entitySubType & Invalid Length

**Error**:
- `The parameter entitySubType is mandatory`
- `The parameter length must be greater than 0`

**Fix Required**:
```python
# In load_demo_data.py, line 1003-1030, update:
data_table_data = {
    'datatableName': table_name,
    'apptableName': app_table_name,
    'entitySubType': None,  # ADD THIS
    'multiRow': False,
    'columns': columns
}

# And for each column with type String/Text:
column = {
    'name': field['field_name'],
    'type': field_type_mapping.get(field['field_type'], 'String'),
    'mandatory': field['mandatory'] == 'Yes',
    'length': 200 if field['field_type'] in ['String', 'Text'] else None  # CHANGE: Use None instead of 0
}
```

### 4. Account Number Formats - Invalid prefixType Values

**Error**: `The parameter prefixType must be one of [401, 1, 101/201/301]`

**Current Mapping** (INCORRECT):
```python
prefix_type_mapping = {
    'Office Short Name': 2,        # Should be 101/201/301 depending on entity
    'Product Short Name': 3        # Should be 201/301 depending on entity
}
```

**Correct Mapping**:
```python
# Different prefixType values per entity type:
# Client (1): [401, 1, 101]
# Loan (2):   [401, 1, 201]
# Savings (3): [401, 1, 301]
# Groups (4):  [1]
# Centers (5): [1]

# Where:
# 1   = None (no prefix)
# 101 = Office name for Client
# 201 = Product short name for Loan
# 301 = Product short name for Savings
# 401 = Custom prefix
```

**Fix Location**: `load_demo_data.py:716-740`

### 5. Charges - Additional Required Fields

Some charge types require additional mandatory fields based on `chargeTimeType`:

**Monthly charges** (`chargeTimeType=6`): Require `feeOnMonthDay` and `feeInterval`
**Annual charges** (`chargeTimeType=7`): Require `feeOnMonthDay` and `feeInterval`

**Example Fix**:
```python
if row['charge_time'] == 'Monthly':
    data['feeOnMonthDay'] = [1, 1]  # 1st of January (month, day)
    data['feeInterval'] = 1         # Every 1 month
    data['dateFormat'] = 'dd MMMM'
    data['monthDayFormat'] = 'dd MMMM'
    data['locale'] = 'en'
```

**Fix Location**: `load_demo_data.py:340-362`

### 6. Working Days - Invalid Structure

**Error**: `The parameter recurrence is mandatory`

**Current Code** (CORRECT structure but may have data issue):
```python
data = {
    'recurrence': {
        'frequency': 'WEEKLY',
        'interval': 1,
        'repeatsOnDay': working_days  # Ensure this list is not empty
    },
    'locale': 'en'
}
```

**Possible Issue**: `working_days` list might be empty. Verify the Excel data has "Yes" for at least one day.

**Fix Location**: `load_demo_data.py:681-696`

### 7. Maker-Checker - 405 Method Not Allowed

**Error**: HTTP 405 for POST `/makercheckers`

**Cause**: Your Fineract version may not support programmatic maker-checker enablement, or uses a different endpoint.

**Solutions**:
1. Enable maker-checker manually via Fineract UI:
   - Admin → System → Maker Checker
2. Comment out the `enable_maker_checker()` call in `load_all()` method
3. Check if your Fineract version uses `/v1/permissions/{id}/makerchecker` instead

**Fix Location**: `load_demo_data.py:361-442` and `load_demo_data.py:1113`

## 📝 Recommended Next Steps

1. **Immediate**: Fix the office permission issue (see Critical section above)
2. **Short-term**: Update `generate_excel_template.py` to prevent shortName length issues
3. **Medium-term**: Query your Fineract instance's `/loanproducts/template` to find correct parameter names
4. **Long-term**: Create a Fineract version compatibility matrix for different API versions

## 🧪 Testing Strategy

After fixing permissions, test incrementally:

```bash
# Test 1: Only system config & GL accounts (no permission issues)
python3 load_demo_data.py ../output/fineract_demo_data_*.xlsx

# Test 2: If offices succeed, full run
python3 load_demo_data.py ../output/fineract_demo_data_*.xlsx

# Test 3: Check created entities
curl -u mifos:password https://localhost/fineract-provider/api/v1/offices --insecure | jq
curl -u mifos:password https://localhost/fineract-provider/api/v1/glaccounts --insecure | jq
```

## 📚 Documentation References

- Fineract API Docs: https://demo.fineract.dev/fineract-provider/api-docs/apiLive.htm
- Your current Fineract version's specific API may differ from documentation
- Use `/v1/{entity}/template` endpoints to discover required/optional parameters

---

**Last Updated**: 2025-10-10
**Status**: Permission issue blocking all data loading. API validation fixes ready once permissions resolved.
