# Excel Generator Script Updates

## Date: October 11, 2025

## Overview
Updated `generate_excel_template.py` to generate Excel files with Fineract-compliant data that won't require runtime fixes in the loader script.

---

## Changes Made

### 1. Savings Product Short Names (Max 4 Characters)

#### Before:
```python
'short_name': 'VOL-SAV',      # 7 characters - INVALID
'short_name': 'FIXED-DEP',    # 9 characters - INVALID
'short_name': 'MAND-GRP',     # 8 characters - INVALID
```

#### After:
```python
'short_name': 'VSAV',         # 4 characters - VALID ✓
'short_name': 'FDEP',         # 4 characters - VALID ✓
'short_name': 'MGRP',         # 4 characters - VALID ✓
```

### 2. Updated All Product References

The script now uses the shortened names consistently throughout:

- **Savings Products sheet**: Updated product definitions (lines 318, 338, 358)
- **Savings Accounts sheet**: Updated 12 account references (lines 594-638)
- **Savings Product Accounting sheet**: Updated accounting mappings (lines 803-805)

---

## Product Name Mapping

| Old Name (Invalid) | New Name (Valid) | Product |
|-------------------|------------------|---------|
| `VOL-SAV` | `VSAV` | Voluntary Savings Account |
| `FIXED-DEP` | `FDEP` | Fixed Deposit Account |
| `MAND-GRP` | `MGRP` | Mandatory Group Savings |

---

## Testing

### Verification Steps

1. **Generate new Excel file:**
   ```bash
   cd scripts
   python3 generate_excel_template.py
   ```

2. **Verify short names:**
   ```bash
   python3 -c "
   import openpyxl
   wb = openpyxl.load_workbook('../output/fineract_demo_data_*.xlsx')
   ws = wb['Savings Products']
   for row in ws.iter_rows(min_row=2, max_row=4, min_col=2, max_col=2):
       print(f'Short name: {row[0].value} (length: {len(row[0].value)})')
   "
   ```

3. **Expected output:**
   ```
   Short name: VSAV (length: 4) ✓
   Short name: FDEP (length: 4) ✓
   Short name: MGRP (length: 4) ✓
   ```

### Load Test

Test the generated Excel file with the loader:

```bash
cd scripts
python3 load_demo_data.py \
  ../output/fineract_demo_data_20251011_092920.xlsx \
  ../config/fineract_config.json
```

**Expected result:** No validation errors for savings product short names.

---

## Compatibility

### Backwards Compatibility

The `load_demo_data.py` script maintains backwards compatibility:
- Still accepts old format Excel files (VOL-SAV, FIXED-DEP, MAND-GRP)
- Automatically converts to 4-character format
- Stores products with both names for lookups

### Forward Compatibility

New Excel files generated will:
- ✅ Load without short name validation errors
- ✅ Work with current Fineract API requirements
- ✅ Be consistent across all sheets

---

## Impact on Existing Files

### Old Excel Files (Pre-Fix)
- Still work with updated `load_demo_data.py`
- Runtime conversion handles old format
- Warning messages show name conversion

### New Excel Files (Post-Fix)
- Work directly without conversion
- No warnings about short names
- Cleaner logs

---

## Related Changes

These generator updates complement the loader script fixes:

1. **Loader Script** (`load_demo_data.py`):
   - Lines 599-609: Runtime short name conversion
   - Lines 656-658: Dual name storage for lookups

2. **Generator Script** (`generate_excel_template.py`):
   - Lines 318, 338, 358: Product definitions
   - Lines 594-638: Account references
   - Lines 803-805: Accounting mappings

---

## Regenerating Data

To create a fresh Excel file with all fixes:

```bash
cd /Users/guymoyo/dev/fineract/docs/data-collection/fineract-demo-data/scripts
python3 generate_excel_template.py
```

Output location: `../output/fineract_demo_data_<timestamp>.xlsx`

---

## Files Modified

1. `generate_excel_template.py` - Excel generator script
2. `FIXES_APPLIED.md` - Updated with generator changes
3. `GENERATOR_UPDATES.md` - This document

---

## Validation Checklist

Before committing changes, verify:

- [x] All savings product short names are exactly 4 characters
- [x] All savings account references use new short names
- [x] All accounting mappings use new short names
- [x] Generated Excel loads without validation errors
- [x] Documentation updated
- [x] Test script runs successfully

---

## Notes

### Why 4 Characters?

Fineract API enforces a maximum length of 4 characters for savings product short names. This is a database constraint in the `m_savings_product` table.

### Other Products

Loan products don't have this restriction:
- `MICRO-SOL` (9 chars) - VALID ✓
- `SME-BIZ` (7 chars) - VALID ✓
- `AGRI-SEASON` (11 chars) - VALID ✓

Only savings products are affected by the 4-character limit.

---

**End of Document**
