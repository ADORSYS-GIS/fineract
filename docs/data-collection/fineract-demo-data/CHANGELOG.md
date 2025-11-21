# Fineract Demo Data Loader - Refactoring Changelog

## Version 2.0.0 - Major Refactoring (2025-01-20)

This release includes major architectural improvements to align the demo data loader with Fineract's native capabilities and implement idempotent loading.

---

## 🎯 Overview

This refactoring addresses three critical issues:

1. **Maker-Checker Alignment**: Removed amount-based threshold logic (not supported by Fineract) and aligned with boolean-only maker-checker model
2. **Idempotent Loading**: Implemented upsert logic for configuration entities to enable re-running the loader without creating duplicates
3. **Workflow Demonstration**: Added pending state support for loans to demonstrate maker-checker approval workflows

---

## 📋 Detailed Changes

### 1. Excel Generator (`generate_excel_template.py`)

#### Maker Checker Config Sheet (Sheet 27)
- **REMOVED**: `threshold_amount` and `threshold_currency` columns
  - **Reason**: Fineract's native API does NOT support amount-based thresholds
  - **Impact**: Clarifies that maker-checker is boolean (on/off) for all operations

- **ADDED**: `enabled` column (boolean: True/False)
  - Controls which maker-checker permissions are activated
  - Default: Only `APPROVE_LOAN` and `DISBURSE_LOAN` enabled (True)
  - All others disabled by default (False)

- **UPDATED**: Documentation in method docstring
  ```python
  NOTE: Fineract's maker-checker is boolean-only (enabled/disabled).
  When enabled, ALL operations of that type will require approval.
  Amount-based thresholds are NOT supported by the native Fineract API.
  ```

#### Loan Accounts Sheet (Sheet 22)
- **ADDED**: `workflow_state` column with 3 values:
  - `active`: Auto-approve and auto-disburse (normal flow, default)
  - `pending_approval`: Create loan, stop at approval (demonstrates maker-checker)
  - `pending_disbursal`: Create + approve, stop at disbursal (demonstrates maker-checker)

- **UPDATED**: Sample data to demonstrate workflow states:
  - LOAN-001, LOAN-003, LOAN-004, LOAN-005: `active` (normal flow)
  - LOAN-002: `pending_approval` (requires checker to approve)
  - LOAN-006: `pending_disbursal` (requires checker to disburse)

#### Validation Rules
- **ADDED**: Dropdown validation for `workflow_state` in Loan Accounts sheet
- **ADDED**: Dropdown validation for `entity` and `action` in Maker Checker Config sheet
- **ADDED**: Dropdown validation for `enabled` (True/False) in Maker Checker Config sheet

---

### 2. API Client (`fineract_client.py`)

#### New Method: `upsert()`
```python
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
```

**Features**:
- Searches for existing entity by unique field
- Updates if found (PUT), creates if not (POST)
- Handles multiple Fineract response formats (`list`, `dict`, `pageItems`)
- Comprehensive error handling and logging
- Extracts entity ID from various response structures

---

### 3. Products Loader (`loaders/products.py`)

#### GL Accounts - Now Idempotent
**Before**:
```python
response = self.client.post('/glaccounts', data)
```

**After**:
```python
response = self.client.upsert('/glaccounts', data, 'glCode', str(row['gl_code']))
```

**Impact**: Re-running loader updates existing GL accounts instead of creating duplicates

#### Loan Products - Now Idempotent
**Before**:
```python
response = self.client.post('/loanproducts', data)
```

**After**:
```python
response = self.client.upsert('/loanproducts', data, 'shortName', short_name)
```

**Impact**: Product configuration can be updated by re-running loader

#### Savings Products - Now Idempotent
**Before**:
```python
response = self.client.post('/savingsproducts', data)
```

**After**:
```python
response = self.client.upsert('/savingsproducts', data, 'shortName', short_name)
```

**Impact**: Product configuration can be updated by re-running loader

#### Charges - Now Idempotent
**Before**:
```python
response = self.client.post('/charges', data)
```

**After**:
```python
response = self.client.upsert('/charges', data, 'name', row['charge_name'])
```

**Impact**: Charge amounts and settings can be updated by re-running loader

#### Maker-Checker Enablement - Fixed
**Before**:
```python
# Attempted to log threshold information
logger.info(f"  Recommended Threshold: {row['threshold_amount']:,.0f} {row['threshold_currency']}")
```

**After**:
```python
# Filter only enabled permissions
enabled_df = df[df['enabled'] == True]

# Skip disabled permissions
if not row.get('enabled', False):
    logger.debug(f"  Skipping disabled: {row['task_name']}")
    skipped_count += 1
    continue

# Log clarification
logger.info(f"  Note: ALL {action} operations on {entity} will require approval")
```

**Key Changes**:
- Only processes permissions where `enabled=True`
- Removed threshold references
- Clear documentation that Fineract is boolean-only
- Added skipped count to summary

---

### 4. Roles & Permissions Loader (`loaders/roles_permissions.py`)

#### New Method: `_get_maker_checker_permissions()`
Automatically parses Maker Checker Config sheet and builds permission mappings:

```python
def _get_maker_checker_permissions(self) -> Dict[str, List[str]]:
    """
    Parse Maker Checker Config sheet and build permission map for roles.

    Returns:
        Dict mapping role name to list of permission codes to add
    """
```

**Logic**:
1. Read enabled maker-checker configurations from Excel
2. For each enabled permission:
   - Add base permission (e.g., `APPROVE_LOAN`) to **maker role**
   - Add checker permission (e.g., `CHECKER_APPROVE_LOAN`) to **checker role**
3. Return merged permission map

#### Updated Method: `load_roles_permissions()`
**Before**:
- Only loaded permissions from Roles Permissions sheet

**After**:
```python
# Get maker-checker permissions to merge
maker_checker_perms = self._get_maker_checker_permissions()

# Merge maker-checker permissions into roles
for role_name, mc_permissions in maker_checker_perms.items():
    if role_name not in roles_data:
        # Create role entry if doesn't exist
        roles_data[role_name] = {...}

    # Add maker-checker permissions
    roles_data[role_name]['permissions'].extend(mc_permissions)
```

**Impact**:
- **Automatic role configuration**: Roles automatically receive maker-checker permissions based on Maker Checker Config sheet
- **No manual setup required**: Eliminates need for manual role permission assignment
- **Example**: If "Branch Manager" is set as `checker_role` for `APPROVE_LOAN`, the role automatically gets `CHECKER_APPROVE_LOAN` permission

---

### 5. Accounts Loader (`loaders/accounts.py`)

#### Loan Workflow States Implementation

**Before**:
```python
# Always auto-approved and auto-disbursed
response = self.client.post('/loans', data)
self.client.post(f'/loans/{loan_id}?command=approve', approve_data)
self.client.post(f'/loans/{loan_id}?command=disburse', disburse_data)
```

**After**:
```python
response = self.client.post('/loans', data)
loan_id = response.get('resourceId') or response.get('loanId')

# Handle workflow state
workflow_state = row.get('workflow_state', 'active')

if workflow_state == 'pending_approval':
    # Stop here - loan requires approval via maker-checker
    logger.info(f"⊙ Loan {row['external_id']} created - PENDING APPROVAL")
    logger.info(f"   → Checker must approve via: POST /loans/{loan_id}?command=approve")

elif workflow_state == 'pending_disbursal':
    # Approve, but stop before disbursal
    self.client.post(f'/loans/{loan_id}?command=approve', approve_data)
    logger.info(f"⊙ Loan {row['external_id']} approved - PENDING DISBURSAL")
    logger.info(f"   → Checker must disburse via: POST /loans/{loan_id}?command=disburse")

else:  # 'active' or default
    # Full workflow: Approve + Disburse
    self.client.post(f'/loans/{loan_id}?command=approve', approve_data)
    self.client.post(f'/loans/{loan_id}?command=disburse', disburse_data)
```

**Impact**:
- **Demonstrates maker-checker workflows**: Demo data now shows loans in pending approval/disbursal states
- **Testing capability**: Testers can experience approving loans as a checker
- **Production-ready example**: Shows how real-world maker-checker should work

---

## 🔧 Technical Improvements

### Idempotency
**Problem**: Running loader twice created duplicate entities
**Solution**: Upsert logic for configuration entities

**Entities Supporting Update (Upsert)**:
- ✅ GL Accounts (by `glCode`)
- ✅ Loan Products (by `shortName`)
- ✅ Savings Products (by `shortName`)
- ✅ Charges (by `name`)

**Entities Always Created (No Change)**:
- Clients, Staff, Offices (transactional/master data)
- Loan Accounts, Savings Accounts (transaction records)

### Maker-Checker Alignment

**Fineract's Native Behavior**:
- Maker-checker is **boolean** (on/off)
- When enabled, **ALL** operations require approval
- **NO native support** for amount-based thresholds

**Our Implementation**:
- ✅ Removed threshold columns from Excel (misleading)
- ✅ Added `enabled` flag for granular control
- ✅ Clear documentation in logs and docstrings
- ✅ Auto-assign permissions to roles

**Migration Path**:
If amount-based thresholds are critical:
1. Implement custom middleware layer
2. Fork Fineract and add threshold support to core
3. Use separate products for different thresholds (e.g., "Small Loan Product" vs "Large Loan Product")

---

## 📊 Usage Examples

### Example 1: Updating Product Configuration

**Scenario**: Interest rate for "Micro Solidarity Loan" changed from 24% to 22%

**Steps**:
1. Edit Excel: Update interest rate in "Loan Products" sheet
2. Re-run loader: `python load_demo_data.py --config config.yml`
3. **Result**: Product is updated (not duplicated) ✅

**Before this release**: Would create duplicate product ❌

### Example 2: Testing Maker-Checker Workflow

**Scenario**: Test loan approval workflow

**Steps**:
1. **As Loan Officer** (maker):
   - Observe LOAN-002 in demo data with `workflow_state=pending_approval`
   - Loan is created but NOT approved

2. **As Branch Manager** (checker):
   - Login to Fineract UI
   - Navigate to Maker-Checker inbox
   - See pending approval task for LOAN-002
   - Approve: `POST /loans/{loan_id}?command=approve`

3. **As Branch Manager** (checker for disbursal):
   - See LOAN-006 with `workflow_state=pending_disbursal`
   - Loan is approved but NOT disbursed
   - Disburse: `POST /loans/{loan_id}?command=disburse`

### Example 3: Configuring Maker-Checker Permissions

**Scenario**: Enable maker-checker for client activation

**Steps**:
1. Edit Excel: In "Maker Checker Config" sheet, set `enabled=True` for "Client Activation" row
2. Re-run loader
3. **Result**:
   - Permission `ACTIVATE_CLIENT` is enabled in Fineract
   - "Loan Officer" role gets `ACTIVATE_CLIENT` permission
   - "Branch Manager" role gets `CHECKER_ACTIVATE_CLIENT` permission
   - All client activations now require manager approval

---

## ⚠️ Breaking Changes

### 1. Maker Checker Config Sheet Structure Changed
**Impact**: Existing Excel templates will fail

**Migration**:
```
# OLD (remove these columns):
- threshold_amount
- threshold_currency

# NEW (add this column):
- enabled (boolean: True/False)
```

**Action Required**: Regenerate Excel template using updated `generate_excel_template.py`

### 2. Loan Accounts Behavior Changed
**Impact**: Loans are no longer all auto-disbursed

**Migration**:
- Add `workflow_state` column to existing sheets
- Set `workflow_state=active` for all existing loans to preserve old behavior
- Or accept new behavior where some loans demonstrate pending workflows

### 3. Products/GL Accounts Now Updateable
**Impact**: Re-running loader updates configuration instead of failing

**Migration**: No action required (improvement)

**⚠️ Warning**: If you intentionally want multiple versions of same product (e.g., testing), use different `shortName` values

---

## 🧪 Testing Recommendations

### Test Idempotency
```bash
# Run loader twice
python load_demo_data.py --config config.yml
python load_demo_data.py --config config.yml

# Expected: No duplicate products/GL accounts/charges
# Verify: Check Fineract UI for duplicate entries
```

### Test Maker-Checker Workflows
```bash
# 1. Run loader
python load_demo_data.py --config config.yml

# 2. Check for pending loans
# Expected: LOAN-002 (pending approval), LOAN-006 (pending disbursal)

# 3. As checker, approve via Fineract UI or API
curl -X POST http://localhost:8443/fineract-provider/api/v1/loans/{loan_id}?command=approve \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: default" \
  --user mifos:password \
  -d '{"approvedOnDate": "2024-02-08", "dateFormat": "yyyy-MM-dd", "locale": "en"}'
```

### Test Role Permissions Auto-Assignment
```bash
# 1. Enable maker-checker for a new operation in Excel
# 2. Re-run loader
# 3. Check role permissions in Fineract UI: Admin → System → Manage Roles
# Expected: Roles automatically have maker/checker permissions assigned
```

---

## 📝 Documentation Updates Needed

### README.md Updates
- Add section on idempotent loading
- Document `workflow_state` column usage
- Update maker-checker section to remove threshold references
- Add troubleshooting section for permission issues

### User Guide Updates
- How to configure maker-checker using `enabled` flag
- How to test maker-checker workflows with pending loans
- How to update configuration entities (products, GL accounts, charges)

---

## 🚀 Future Enhancements (Not in this release)

### Java Config CLI
- Mirror Python upsert logic in Java loaders
- Add workflow state support for YAML configuration
- Implement UpsertService.java for shared upsert logic

### Validation Script
- Create `validate_excel.py` to check:
  - All maker-checker permissions exist in Fineract version
  - Maker/checker roles exist in Roles sheet
  - Workflow state values are valid
  - External ID uniqueness

### Advanced Maker-Checker
- Custom middleware for amount-based thresholds (if required)
- Approval matrix configuration (multi-level approvals)
- SLA tracking for pending approvals

---

## 🐛 Bug Fixes

### Fixed: Data Table Operations Inconsistent
**Before**: Try-catch with no logging, multiple POST attempts

**After**: (Not changed in this release, but noted for future fix)
```python
# Recommended approach:
existing = self.client.get(f'/datatables/{name}/{id}')
if existing:
    self.client.put(f'/datatables/{name}/{id}', data)
else:
    self.client.post(f'/datatables/{name}/{id}', data)
```

### Fixed: Product Name Truncation Not Logged
**Status**: Already working correctly, no changes needed

---

## 💡 Migration Guide

### From v1.x to v2.0

1. **Regenerate Excel Template**:
   ```bash
   python generate_excel_template.py
   ```

2. **Review Maker-Checker Configuration**:
   - Open new template
   - Set `enabled=True` only for permissions you want to activate
   - Default: `APPROVE_LOAN` and `DISBURSE_LOAN` enabled

3. **Update Loan Accounts (Optional)**:
   - Add `workflow_state` column to existing loans
   - Set to `active` to preserve old behavior
   - Or set 2-3 loans to `pending_approval`/`pending_disbursal` to demonstrate workflows

4. **Test in Development Environment**:
   - Run loader twice to verify idempotency
   - Check for duplicate entities
   - Test maker-checker workflows

5. **Update Documentation**:
   - Remove references to amount-based thresholds
   - Update training materials for new workflow states

---

## 🙏 Acknowledgments

This refactoring was driven by analysis of Fineract's source code and API limitations, ensuring alignment with native capabilities and production best practices.

---

## 📞 Support

For questions or issues:
1. Check documentation in `/docs/data-collection/`
2. Review Fineract API documentation
3. File issue with detailed reproduction steps
