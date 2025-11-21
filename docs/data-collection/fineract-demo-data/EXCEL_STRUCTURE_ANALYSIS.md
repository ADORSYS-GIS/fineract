# Excel Structure Analysis - Root Cause of Missing Data

## Investigation Date: 2025-11-20

## Summary
The Excel to YAML converter is failing to extract data because it's looking for **wrong column names** and doesn't understand the **hierarchical data structure** in the Excel sheets.

---

## 1. CODES AND VALUES SHEET ✅ STRUCTURE CORRECT

### Actual Excel Columns:
```
['code_name', 'code_value', 'code_position', 'is_active', 'description']
```

### Converter Expectations (excel_to_yaml.py lines 129-139):
```python
value_name = row.get('code_value') or row.get('value_name') or row.get('code_value_name')
```

### ROOT CAUSE:
**FALSE ALARM!** The column name is correct: `code_value` ✅

The real issue is likely:
1. NaN/empty value handling
2. Logic bug in grouping by code_name
3. Silent failure when appending values

### Sample Data:
```
code_name='Gender', code_value='Male', code_position=1, is_active='Yes'
code_name='Gender', code_value='Female', code_position=2, is_active='Yes'
code_name='ClientType', code_value='Individual', code_position=1, is_active='Yes'
```

**Expected YAML output:**
```yaml
codes:
  - name: Gender
    values:
      - name: Male
        position: 1
        isActive: true
      - name: Female
        position: 2
        isActive: true
```

---

## 2. DATA TABLES SHEET ✅ HIERARCHICAL STRUCTURE

### Actual Excel Columns:
```
['entity_type', 'table_name', 'field_name', 'field_type', 'mandatory', 'dropdown_values', 'description']
```

### Data Structure:
**Multi-row hierarchical format** - Each row is a FIELD, not a TABLE!
```
entity_type='Client', table_name='client_additional_info', field_name='id_type', field_type='Dropdown', mandatory='Yes'
entity_type='Client', table_name='client_additional_info', field_name='id_number', field_type='String', mandatory='Yes'
entity_type='Client', table_name='client_additional_info', field_name='id_expiry_date', field_type='Date', mandatory='No'
...
entity_type='Loan', table_name='loan_purpose_details', field_name='loan_purpose', field_type='Dropdown', mandatory='Yes'
```

### ROOT CAUSE:
The converter treats **one row = one data table**, but actually **one row = one field** in a data table!

**Current converter logic (lines 201-217):**
```python
for _, row in df.iterrows():
    if row.get('table_name'):
        data_tables.append({
            'name': row['table_name'],
            'appTableName': row.get('app_table_name', 'm_client'),
            'multiRow': self._parse_boolean(row.get('multi_row', False))
        })
        # NO COLUMNS!
```

**What it should do:**
1. Group rows by `table_name`
2. Extract table metadata from first row
3. Extract column definitions from all rows with same `table_name`

**Expected YAML output:**
```yaml
dataTables:
  - name: client_additional_info
    appTableName: m_client
    multiRow: false
    columns:
      - name: id_type
        type: Dropdown
        mandatory: true
        values: ["National ID", "Passport", "Voter Card", "Driver License"]
      - name: id_number
        type: String
        mandatory: true
```

---

## 3. NOTIFICATION TEMPLATES SHEET ✅ STRUCTURE CORRECT

### Actual Excel Columns:
```
['template_name', 'channel', 'event_trigger', 'subject', 'message_body', 'is_active', 'description']
```

### Converter Expectations (lines 174-199):
```python
if row.get('template_name'):
    template['name'] = row['template_name']
# Gets: name, type, entity, subject
# MISSING: message_body extraction!
```

### ROOT CAUSE:
The converter code **exists but doesn't extract** `message_body` or `subject` fields!

### Sample Data:
```
template_name='Client Activation', channel='SMS', subject=NaN,
message_body='Hello {clientName}, your account has been activated...'

template_name='Client Activation Email', channel='Email',
subject='Welcome to {institutionName}',
message_body='Dear {clientName},\n\nYour account has been successfully activated...'
```

**Expected YAML output:**
```yaml
notificationTemplates:
  - name: Client Activation
    channel: SMS
    eventTrigger: Client Activated
    messageBody: "Hello {clientName}, your account has been activated at {officeName}..."
    isActive: true
  - name: Client Activation Email
    channel: Email
    eventTrigger: Client Activated
    subject: "Welcome to {institutionName}"
    messageBody: "Dear {clientName},\n\nYour account has been successfully activated..."
    isActive: true
```

---

## 4. LOAN PRODUCT ACCOUNTING SHEET ❌ NOT CONVERTED AT ALL

### Actual Excel Columns:
```
['product_short_name', 'mapping_type', 'gl_code', 'gl_name', 'description']
```

### Current Converter Status:
**SHEET NOT PROCESSED AT ALL!** There's no converter method for this sheet.

### Sample Data:
```
product_short_name='MSOL', mapping_type='Fund Source', gl_code=42, gl_name='Cash on Hand'
product_short_name='MSOL', mapping_type='Loan Portfolio', gl_code=51, gl_name='Loans to Clients - Principal'
product_short_name='MSOL', mapping_type='Interest Income', gl_code=81, gl_name='Interest Income on Loans'
...
product_short_name='SBIZ', mapping_type='Fund Source', gl_code=42, gl_name='Cash on Hand'
```

### Data Structure:
**Multi-row hierarchical format** - Each row is one GL mapping for a product
- 13 mappings per loan product (Fund Source, Loan Portfolio, Interest Income, Fee Income, etc.)

### ROOT CAUSE:
**Feature not implemented!** The converter has NO code to handle accounting mappings.

**Expected YAML output:**
```yaml
loanProducts:
  - shortName: MSOL
    name: Microfinance Solidarity Loan
    accounting:
      rule: ACCRUAL_PERIODIC
      fundSourceAccount: 42  # Cash on Hand
      loanPortfolioAccount: 51  # Loans to Clients - Principal
      interestReceivableAccount: 52
      feesReceivableAccount: 53
      penaltiesReceivableAccount: 54
      transferInSuspenseAccount: 122
      interestIncomeAccount: 81
      feeIncomeAccount: 82
      penaltyIncomeAccount: 83
      lossesWrittenOffAccount: 93
      goodwillCreditAccount: 81
      incomeFromRecoveryAccount: 81
      overpaymentLiabilityAccount: 64
```

---

## FIXING PRIORITY

### Priority 1: Code Values (CRITICAL)
- ✅ Column names are correct
- ❌ Bug in value extraction logic
- **Fix**: Debug the grouping and appending logic

### Priority 2: Data Tables (HIGH)
- ✅ Column names are correct
- ❌ Wrong extraction logic (treats field as table)
- **Fix**: Group by table_name, extract columns array

### Priority 3: Notification Templates (MEDIUM)
- ✅ Column names are correct
- ❌ Message body not extracted
- **Fix**: Add `message_body` and `subject` extraction

### Priority 4: Accounting Mappings (HIGH)
- ❌ Not implemented at all
- **Fix**: Create new converter method, group by product_short_name, map GL accounts

---

## NEXT STEPS

1. Fix Code Values extraction logic
2. Rewrite Data Tables extraction to support hierarchical structure
3. Add message_body/subject extraction to Notification Templates
4. Implement Loan Product Accounting converter method
5. Add similar method for Savings Product Accounting
6. Add validation to catch empty arrays/missing data
7. Test with generated Excel file
