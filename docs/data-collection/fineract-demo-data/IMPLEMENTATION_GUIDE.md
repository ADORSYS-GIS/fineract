# Complete Implementation Guide - Missing Converters

**Purpose**: Step-by-step guide with code templates to add all 23 missing converters to excel_to_yaml.py

**Status**: Items 1-2 completed. Items 3-21 need implementation.

---

## Table of Contents

1. [✅ Completed](#completed)
2. [🔴 CRITICAL Priority (Items 3-8)](#critical-priority)
3. [🟡 HIGH Priority (Items 9-13)](#high-priority)
4. [🟢 MEDIUM Priority (Items 14-18)](#medium-priority)
5. [🔵 Validation Updates (Item 19)](#validation-updates)
6. [General Patterns & Tips](#general-patterns)

---

## Completed

### ✅ 1. Fixed Global Config Validation Bug

**File**: `excel_to_yaml.py` line 814
**Change**: `globalConfiguration` → `globalConfig`
**Status**: DONE ✅

### ✅ 2. Enhanced Loan Products (18 fields added)

**File**: `excel_to_yaml.py` lines 489-570
**Fields Added**: Interest rates (min/default/max), principal ranges, repayment ranges, grace periods, fees, insurance, amortization, interest calculation
**Status**: DONE ✅

---

## 🔴 CRITICAL Priority

### 3. Enhance Savings Products (6 fields)

**Location**: Lines 572-605 in `excel_to_yaml.py`
**Excel Sheet**: "Savings Products"
**Fields to Add**:
- `overdraft_allowed` → `overdraftAllowed`
- `withdrawal_fee_for_transfers` → `withdrawalFeeForTransfers`
- `allow_dormancy_tracking` → `allowDormancyTracking`
- `days_to_inactive` → `daysToInactive`
- `days_to_dormancy` → `daysToDormancy`
- `withhold_tax` → `withholdTax`
- `lock_in_period` → `lockinPeriodFrequency`

**Code Template**:
```python
# Add after line 600 (after minBalanceForInterestCalculation)

# Overdraft settings
if row.get('overdraft_allowed') and not pd.isna(row.get('overdraft_allowed')):
    product['overdraftAllowed'] = self._parse_boolean(row['overdraft_allowed'])

# Withdrawal fees
if row.get('withdrawal_fee_for_transfers') and not pd.isna(row.get('withdrawal_fee_for_transfers')):
    product['withdrawalFeeForTransfers'] = self._parse_boolean(row['withdrawal_fee_for_transfers'])

# Dormancy tracking
if row.get('allow_dormancy_tracking') and not pd.isna(row.get('allow_dormancy_tracking')):
    product['allowDormancyTracking'] = self._parse_boolean(row['allow_dormancy_tracking'])
if row.get('days_to_inactive') and not pd.isna(row.get('days_to_inactive')):
    product['daysToInactive'] = int(row['days_to_inactive'])
if row.get('days_to_dormancy') and not pd.isna(row.get('days_to_dormancy')):
    product['daysToDormancy'] = int(row['days_to_dormancy'])

# Tax withholding
if row.get('withhold_tax') and not pd.isna(row.get('withhold_tax')):
    product['withholdTax'] = self._parse_boolean(row['withhold_tax'])

# Lock-in period
if row.get('lock_in_period') and not pd.isna(row.get('lock_in_period')):
    product['lockinPeriodFrequency'] = int(row['lock_in_period'])
```

---

### 4. Add Delinquency Buckets Converter

**Location**: After Savings Products converter (around line 610)
**Excel Sheet**: "Delinquency Buckets"
**Output Key**: `delinquencyBuckets` (add to `self.config`)
**Excel Columns**: `bucket_name`, `min_days`, `max_days`, `classification`

**Complete Code Template**:
```python
# 4. Delinquency Buckets (add after savings products, before accounting mappings)
df = self._read_sheet('Delinquency Buckets')
if not df.empty:
    delinquency_buckets = []
    for _, row in df.iterrows():
        if not row.get('bucket_name') or pd.isna(row.get('bucket_name')):
            continue

        bucket = {
            'name': row['bucket_name']
        }

        # Min/Max days
        if row.get('min_days') and not pd.isna(row.get('min_days')):
            bucket['minDays'] = int(row['min_days'])
        if row.get('max_days') and not pd.isna(row.get('max_days')):
            bucket['maxDays'] = int(row['max_days'])

        # Classification
        if row.get('classification') and not pd.isna(row.get('classification')):
            bucket['classification'] = row['classification']

        delinquency_buckets.append(bucket)

    if delinquency_buckets:
        self.config['delinquencyBuckets'] = delinquency_buckets
        logger.info(f"  ✓ Delinquency Buckets: {len(delinquency_buckets)} buckets")
```

---

### 5. Add Loan Provisioning Converter

**Location**: After Delinquency Buckets converter
**Excel Sheet**: "Loan Provisioning"
**Output Key**: `loanProvisioning` (add to `self.config`)
**Excel Columns**: `category_name`, `min_days`, `max_days`, `provision_percentage`, `liability_gl_code`, `expense_gl_code`

**Complete Code Template**:
```python
# 5. Loan Provisioning
df = self._read_sheet('Loan Provisioning')
if not df.empty:
    provisioning = []
    for _, row in df.iterrows():
        if not row.get('category_name') or pd.isna(row.get('category_name')):
            continue

        category = {
            'categoryName': row['category_name']
        }

        # Days range
        if row.get('min_days') and not pd.isna(row.get('min_days')):
            category['minDays'] = int(row['min_days'])
        if row.get('max_days') and not pd.isna(row.get('max_days')):
            category['maxDays'] = int(row['max_days'])

        # Provision percentage
        if row.get('provision_percentage') and not pd.isna(row.get('provision_percentage')):
            category['provisioningPercentage'] = float(row['provision_percentage'])

        # GL accounts
        if row.get('liability_gl_code') and not pd.isna(row.get('liability_gl_code')):
            category['liabilityAccountId'] = int(row['liability_gl_code'])
        if row.get('expense_gl_code') and not pd.isna(row.get('expense_gl_code')):
            category['expenseAccountId'] = int(row['expense_gl_code'])

        provisioning.append(category)

    if provisioning:
        self.config['loanProvisioning'] = provisioning
        logger.info(f"  ✓ Loan Provisioning: {len(provisioning)} categories")
```

---

### 6. Add Scheduler Jobs Converter

**Location**: In `convert_system_config()` method, after Data Tables (around line 265)
**Excel Sheet**: "Scheduler Jobs"
**Output Key**: `schedulerJobs` (add to `systemConfig`)
**Excel Columns**: `job_name`, `display_name`, `cron_expression`, `is_active`, `job_parameters`

**Complete Code Template**:
```python
# 8. Scheduler Jobs (add in convert_system_config, after Data Tables)
df = self._read_sheet('Scheduler Jobs')
if not df.empty:
    try:
        scheduler_jobs = []
        for _, row in df.iterrows():
            if not row.get('job_name') or pd.isna(row.get('job_name')):
                continue

            job = {
                'name': row['job_name']
            }

            # Display name
            if row.get('display_name') and not pd.isna(row.get('display_name')):
                job['displayName'] = row['display_name']

            # Cron expression
            if row.get('cron_expression') and not pd.isna(row.get('cron_expression')):
                job['cronExpression'] = row['cron_expression']

            # Active status
            if row.get('is_active'):
                job['active'] = self._parse_boolean(row['is_active'])

            # Job parameters (if any)
            if row.get('job_parameters') and not pd.isna(row.get('job_parameters')):
                job['jobParameters'] = row['job_parameters']

            scheduler_jobs.append(job)

        if scheduler_jobs:
            system_config['schedulerJobs'] = scheduler_jobs
            logger.info(f"  ✓ Scheduler Jobs: {len(scheduler_jobs)} jobs")
    except Exception as e:
        logger.warning(f"  ⚠ Skipping Scheduler Jobs: {str(e)}")
```

---

### 7. Add Maker-Checker Configuration Converter

**Location**: In `convert_system_config()` method, after Scheduler Jobs
**Excel Sheet**: "Maker Checker Config"
**Output Key**: `makerCheckerConfig` (add to `systemConfig`)
**Excel Columns**: `entity`, `action`, `maker_checker_enabled`

**Complete Code Template**:
```python
# 9. Maker-Checker Configuration
df = self._read_sheet('Maker Checker Config')
if not df.empty:
    try:
        maker_checker = []
        for _, row in df.iterrows():
            if not row.get('entity') or pd.isna(row.get('entity')):
                continue

            config = {
                'entity': row['entity']
            }

            # Action
            if row.get('action') and not pd.isna(row.get('action')):
                config['action'] = row['action']

            # Enabled status
            if row.get('maker_checker_enabled'):
                config['enabled'] = self._parse_boolean(row['maker_checker_enabled'])

            maker_checker.append(config)

        if maker_checker:
            system_config['makerCheckerConfig'] = maker_checker
            logger.info(f"  ✓ Maker-Checker Config: {len(maker_checker)} rules")
    except Exception as e:
        logger.warning(f"  ⚠ Skipping Maker-Checker Config: {str(e)}")
```

---

### 8. Add SMS/Email Configuration Converter

**Location**: In `convert_system_config()` method, after Maker-Checker
**Excel Sheet**: "SMS Email Config"
**Output Key**: `smsEmailConfig` (add to `systemConfig`)
**Excel Columns**: `config_name`, `config_value`, `description`

**Complete Code Template**:
```python
# 10. SMS/Email Configuration
df = self._read_sheet('SMS Email Config')
if not df.empty:
    try:
        sms_email_config = []
        for _, row in df.iterrows():
            if not row.get('config_name') or pd.isna(row.get('config_name')):
                continue

            config = {
                'name': row['config_name']
            }

            # Config value
            if row.get('config_value') and not pd.isna(row.get('config_value')):
                config['value'] = str(row['config_value'])

            # Description
            if row.get('description') and not pd.isna(row.get('description')):
                config['description'] = row['description']

            sms_email_config.append(config)

        if sms_email_config:
            system_config['smsEmailConfig'] = sms_email_config
            logger.info(f"  ✓ SMS/Email Config: {len(sms_email_config)} settings")
    except Exception as e:
        logger.warning(f"  ⚠ Skipping SMS/Email Config: {str(e)}")
```

---

## 🟡 HIGH Priority

### 9. Add Holidays Converter

**Location**: In `convert_system_config()` method
**Excel Sheet**: "Holidays"
**Output Key**: `holidays` (add to `systemConfig`)
**Excel Columns**: `holiday_name`, `from_date`, `to_date`, `repayments_rescheduled_to`, `offices`

**Complete Code Template**:
```python
# 11. Holidays
df = self._read_sheet('Holidays')
if not df.empty:
    try:
        holidays = []
        for _, row in df.iterrows():
            if not row.get('holiday_name') or pd.isna(row.get('holiday_name')):
                continue

            holiday = {
                'name': row['holiday_name']
            }

            # Dates
            if row.get('from_date'):
                holiday['fromDate'] = self._parse_date(row['from_date'])
            if row.get('to_date'):
                holiday['toDate'] = self._parse_date(row['to_date'])

            # Rescheduling
            if row.get('repayments_rescheduled_to'):
                holiday['repaymentsRescheduledTo'] = self._parse_date(row['repayments_rescheduled_to'])

            # Offices (comma-separated list)
            if row.get('offices') and not pd.isna(row.get('offices')):
                offices_str = str(row['offices'])
                holiday['offices'] = [o.strip() for o in offices_str.split(',')]

            holidays.append(holiday)

        if holidays:
            system_config['holidays'] = holidays
            logger.info(f"  ✓ Holidays: {len(holidays)} holidays")
    except Exception as e:
        logger.warning(f"  ⚠ Skipping Holidays: {str(e)}")
```

---

### 10. Add Tax Groups Converter

**Location**: In `convert_accounting()` method, after Financial Activity Mappings
**Excel Sheet**: "Tax Groups"
**Output Key**: `taxGroups` (add to `self.config`)
**Excel Columns**: `tax_group_name`, `tax_rate`, `tax_component_name`, `tax_component_gl_code`

**Complete Code Template**:
```python
# 5. Tax Groups (add in convert_accounting, after Financial Activity Mappings)
df = self._read_sheet('Tax Groups')
if not df.empty:
    # Group by tax group name
    tax_groups_dict = {}
    for _, row in df.iterrows():
        tax_group_name = row.get('tax_group_name')
        if not tax_group_name or pd.isna(tax_group_name):
            continue

        if tax_group_name not in tax_groups_dict:
            tax_groups_dict[tax_group_name] = {
                'name': tax_group_name,
                'taxComponents': []
            }

            # Tax rate (at group level)
            if row.get('tax_rate') and not pd.isna(row.get('tax_rate')):
                tax_groups_dict[tax_group_name]['taxRate'] = float(row['tax_rate'])

        # Tax component (each row is a component)
        component_name = row.get('tax_component_name')
        if component_name and not pd.isna(component_name):
            component = {
                'name': component_name
            }

            # GL code for component
            if row.get('tax_component_gl_code') and not pd.isna(row.get('tax_component_gl_code')):
                component['glAccountId'] = int(row['tax_component_gl_code'])

            tax_groups_dict[tax_group_name]['taxComponents'].append(component)

    if tax_groups_dict:
        self.config['taxGroups'] = list(tax_groups_dict.values())
        total_components = sum(len(tg['taxComponents']) for tg in tax_groups_dict.values())
        logger.info(f"  ✓ Tax Groups: {len(tax_groups_dict)} groups, {total_components} components")
```

---

### 11. Add Tellers Converter

**Location**: In `convert_accounting()` method, after Tax Groups
**Excel Sheet**: "Tellers"
**Output Key**: `tellers` (add to `self.config`)
**Excel Columns**: `teller_name`, `office_name`, `start_date`, `end_date`, `status`, `description`

**Complete Code Template**:
```python
# 6. Tellers
df = self._read_sheet('Tellers')
if not df.empty:
    tellers = []
    for _, row in df.iterrows():
        if not row.get('teller_name') or pd.isna(row.get('teller_name')):
            continue

        teller = {
            'name': row['teller_name']
        }

        # Office
        if row.get('office_name') and not pd.isna(row.get('office_name')):
            teller['officeName'] = row['office_name']

        # Dates
        if row.get('start_date'):
            teller['startDate'] = self._parse_date(row['start_date'])
        if row.get('end_date'):
            teller['endDate'] = self._parse_date(row['end_date'])

        # Status
        if row.get('status') and not pd.isna(row.get('status')):
            teller['status'] = row['status']

        # Description
        if row.get('description') and not pd.isna(row.get('description')):
            teller['description'] = row['description']

        tellers.append(teller)

    if tellers:
        self.config['tellers'] = tellers
        logger.info(f"  ✓ Tellers: {len(tellers)} tellers")
```

---

### 12. Add Teller Cashier Mapping Converter

**Location**: In `convert_accounting()` method, after Tellers
**Excel Sheet**: "Teller Cashier Mapping"
**Output Key**: `tellerCashierMappings` (add to `self.config`)
**Excel Columns**: `office_name`, `staff_name`, `savings_gl_code`, `cash_gl_code`, `expense_gl_code`, `liability_gl_code`

**Complete Code Template**:
```python
# 7. Teller Cashier Mappings
df = self._read_sheet('Teller Cashier Mapping')
if not df.empty:
    teller_mappings = []
    for _, row in df.iterrows():
        if not row.get('office_name') or pd.isna(row.get('office_name')):
            continue

        mapping = {
            'officeName': row['office_name']
        }

        # Staff assignment
        if row.get('staff_name') and not pd.isna(row.get('staff_name')):
            mapping['staffName'] = row['staff_name']

        # GL account mappings
        if row.get('savings_gl_code') and not pd.isna(row.get('savings_gl_code')):
            mapping['savingsAccountId'] = int(row['savings_gl_code'])
        if row.get('cash_gl_code') and not pd.isna(row.get('cash_gl_code')):
            mapping['cashAccountId'] = int(row['cash_gl_code'])
        if row.get('expense_gl_code') and not pd.isna(row.get('expense_gl_code')):
            mapping['expenseAccountId'] = int(row['expense_gl_code'])
        if row.get('liability_gl_code') and not pd.isna(row.get('liability_gl_code')):
            mapping['liabilityAccountId'] = int(row['liability_gl_code'])

        teller_mappings.append(mapping)

    if teller_mappings:
        self.config['tellerCashierMappings'] = teller_mappings
        logger.info(f"  ✓ Teller Cashier Mappings: {len(teller_mappings)} mappings")
```

---

### 13. Add Payment Type Accounting Converter

**Location**: In existing `_load_product_accounting_mappings()` method, after Savings Product Accounting
**Excel Sheet**: "Payment Type Accounting"
**Purpose**: Merge GL mappings into existing payment types
**Excel Columns**: `payment_type_name`, `fund_source_gl_code`, `cash_gl_code`

**Complete Code Template**:
```python
# 3. Payment Type Accounting Mappings (add in _load_product_accounting_mappings)
df = self._read_sheet('Payment Type Accounting')
if not df.empty and 'payment_type_name' in df.columns:
    # Group mappings by payment type
    mappings_by_payment_type = {}
    for _, row in df.iterrows():
        payment_type = row.get('payment_type_name')
        if not payment_type or pd.isna(payment_type):
            continue

        if payment_type not in mappings_by_payment_type:
            mappings_by_payment_type[payment_type] = {}

        # GL account mappings
        if row.get('fund_source_gl_code') and not pd.isna(row.get('fund_source_gl_code')):
            mappings_by_payment_type[payment_type]['fundSourceAccountId'] = int(row['fund_source_gl_code'])
        if row.get('cash_gl_code') and not pd.isna(row.get('cash_gl_code')):
            mappings_by_payment_type[payment_type]['cashAccountId'] = int(row['cash_gl_code'])

    # Merge into payment types
    if 'paymentTypes' in self.config and mappings_by_payment_type:
        for payment_type in self.config['paymentTypes']:
            name = payment_type.get('name')
            if name in mappings_by_payment_type:
                accounting = mappings_by_payment_type[name]
                if accounting:
                    payment_type['accounting'] = accounting

        logger.info(f"  ✓ Payment Type Accounting: {len(mappings_by_payment_type)} types mapped")
```

---

## 🟢 MEDIUM Priority

### 14. Add Collateral Types Converter

**Location**: In `convert_products()` method, after Loan Provisioning
**Excel Sheet**: "Collateral Types"
**Output Key**: `collateralTypes` (add to `self.config`)
**Excel Columns**: `collateral_type_name`, `description`, `is_active`, `quality`, `percentage`

**Complete Code Template**:
```python
# 6. Collateral Types (add in convert_products, after loan provisioning)
df = self._read_sheet('Collateral Types')
if not df.empty:
    collateral_types = []
    for _, row in df.iterrows():
        if not row.get('collateral_type_name') or pd.isna(row.get('collateral_type_name')):
            continue

        collateral_type = {
            'name': row['collateral_type_name']
        }

        # Description
        if row.get('description') and not pd.isna(row.get('description')):
            collateral_type['description'] = row['description']

        # Active status
        if row.get('is_active'):
            collateral_type['isActive'] = self._parse_boolean(row['is_active'])

        # Quality rating
        if row.get('quality') and not pd.isna(row.get('quality')):
            collateral_type['quality'] = row['quality']

        # Percentage (LTV ratio)
        if row.get('percentage') and not pd.isna(row.get('percentage')):
            collateral_type['percentage'] = float(row['percentage'])

        collateral_types.append(collateral_type)

    if collateral_types:
        self.config['collateralTypes'] = collateral_types
        logger.info(f"  ✓ Collateral Types: {len(collateral_types)} types")
```

---

### 15. Add Guarantor Types Converter

**Location**: In `convert_products()` method, after Collateral Types
**Excel Sheet**: "Guarantor Types"
**Output Key**: `guarantorTypes` (add to `self.config`)
**Excel Columns**: `guarantor_type_name`, `description`, `is_active`

**Complete Code Template**:
```python
# 7. Guarantor Types
df = self._read_sheet('Guarantor Types')
if not df.empty:
    guarantor_types = []
    for _, row in df.iterrows():
        if not row.get('guarantor_type_name') or pd.isna(row.get('guarantor_type_name')):
            continue

        guarantor_type = {
            'name': row['guarantor_type_name']
        }

        # Description
        if row.get('description') and not pd.isna(row.get('description')):
            guarantor_type['description'] = row['description']

        # Active status
        if row.get('is_active'):
            guarantor_type['isActive'] = self._parse_boolean(row['is_active'])

        guarantor_types.append(guarantor_type)

    if guarantor_types:
        self.config['guarantorTypes'] = guarantor_types
        logger.info(f"  ✓ Guarantor Types: {len(guarantor_types)} types")
```

---

### 16. Add Floating Rates Converter

**Location**: In `convert_products()` method, after Guarantor Types
**Excel Sheet**: "Floating Rates"
**Output Key**: `floatingRates` (add to `self.config`)
**Excel Columns**: `rate_name`, `base_rate`, `differential`, `is_base_lending_rate`, `is_active`

**Complete Code Template**:
```python
# 8. Floating Rates
df = self._read_sheet('Floating Rates')
if not df.empty:
    floating_rates = []
    for _, row in df.iterrows():
        if not row.get('rate_name') or pd.isna(row.get('rate_name')):
            continue

        floating_rate = {
            'name': row['rate_name']
        }

        # Base rate
        if row.get('base_rate') and not pd.isna(row.get('base_rate')):
            floating_rate['baseRate'] = float(row['base_rate'])

        # Differential
        if row.get('differential') and not pd.isna(row.get('differential')):
            floating_rate['differential'] = float(row['differential'])

        # Is base lending rate
        if row.get('is_base_lending_rate'):
            floating_rate['isBaseLendingRate'] = self._parse_boolean(row['is_base_lending_rate'])

        # Active status
        if row.get('is_active'):
            floating_rate['isActive'] = self._parse_boolean(row['is_active'])

        floating_rates.append(floating_rate)

    if floating_rates:
        self.config['floatingRates'] = floating_rates
        logger.info(f"  ✓ Floating Rates: {len(floating_rates)} rates")
```

---

### 17. Enhance Clients (13 fields)

**Location**: Lines 656-700 in `excel_to_yaml.py` (in `convert_operations()` method)
**Excel Sheet**: "Clients"
**Fields to Add**:
- `staff_external_id` → `staffName`
- `email` → `emailAddress`
- `client_type` → `clientType`
- `client_classification` → `clientClassification`
- `national_id` → `nationalId`
- `address` → `addressLine1`
- `city` → `city`
- `marital_status` → `maritalStatus`
- `number_of_dependents` → `numberOfDependents`
- `occupation` → `occupation`
- `business_type` → `businessType`
- `monthly_income` → `monthlyIncome`
- `risk_rating` → `riskRating`

**Code Template**:
```python
# Add after existing client fields (around line 690)

# Staff assignment
if row.get('staff_external_id') and not pd.isna(row.get('staff_external_id')):
    client['staffName'] = row['staff_external_id']

# Contact information
if row.get('email') and not pd.isna(row.get('email')):
    client['emailAddress'] = row['email']

# Client categorization
if row.get('client_type') and not pd.isna(row.get('client_type')):
    client['clientType'] = row['client_type']
if row.get('client_classification') and not pd.isna(row.get('client_classification')):
    client['clientClassification'] = row['client_classification']

# Identification
if row.get('national_id') and not pd.isna(row.get('national_id')):
    client['nationalId'] = str(row['national_id'])

# Address information
if row.get('address') and not pd.isna(row.get('address')):
    client['addressLine1'] = row['address']
if row.get('city') and not pd.isna(row.get('city')):
    client['city'] = row['city']

# Personal details
if row.get('marital_status') and not pd.isna(row.get('marital_status')):
    client['maritalStatus'] = row['marital_status']
if row.get('number_of_dependents') and not pd.isna(row.get('number_of_dependents')):
    client['numberOfDependents'] = int(row['number_of_dependents'])

# Financial profile
if row.get('occupation') and not pd.isna(row.get('occupation')):
    client['occupation'] = row['occupation']
if row.get('business_type') and not pd.isna(row.get('business_type')):
    client['businessType'] = row['business_type']
if row.get('monthly_income') and not pd.isna(row.get('monthly_income')):
    client['monthlyIncome'] = float(row['monthly_income'])

# Risk assessment
if row.get('risk_rating') and not pd.isna(row.get('risk_rating')):
    client['riskRating'] = row['risk_rating']
```

---

### 18. Fix Groups and Centers Staff Linking

**Location**: Lines 700-750 in `excel_to_yaml.py`
**Issue**: Groups/Centers use `staffName: loan.douala` which doesn't match staff external IDs

**Fix for Groups**:
```python
# In Groups converter (around line 715)
# BEFORE:
if row.get('staff'):
    group['staffName'] = row['staff']

# AFTER:
if row.get('staff_external_id') and not pd.isna(row.get('staff_external_id')):
    group['staffName'] = row['staff_external_id']
elif row.get('staff') and not pd.isna(row.get('staff')):
    # Fallback to staff column if external_id not present
    group['staffName'] = row['staff']
```

**Fix for Centers**:
```python
# In Centers converter (around line 745)
# Same pattern as Groups
if row.get('staff_external_id') and not pd.isna(row.get('staff_external_id')):
    center['staffName'] = row['staff_external_id']
elif row.get('staff') and not pd.isna(row.get('staff')):
    center['staffName'] = row['staff']
```

---

## 🔵 Validation Updates

### 19. Add Validation for All New Entity Types

**Location**: In `_validate_conversion()` method (lines 782-1110)
**Add after existing Phase 4 validation (around line 1027)**

**Complete Code Template**:
```python
# 4.4 Delinquency Buckets (add after Savings Products validation)
delinquency_buckets = self.config.get('delinquencyBuckets', [])
if len(delinquency_buckets) > 0:
    validation_results.append(('Delinquency Buckets', True, f"{len(delinquency_buckets)} buckets"))
    logger.info(f"  ✓ Delinquency Buckets: {len(delinquency_buckets)} buckets")
else:
    validation_results.append(('Delinquency Buckets', False, "Empty"))
    errors.append("❌ CRITICAL: No delinquency buckets defined")

# 4.5 Loan Provisioning
loan_provisioning = self.config.get('loanProvisioning', [])
if len(loan_provisioning) > 0:
    validation_results.append(('Loan Provisioning', True, f"{len(loan_provisioning)} categories"))
    logger.info(f"  ✓ Loan Provisioning: {len(loan_provisioning)} categories")
else:
    validation_results.append(('Loan Provisioning', False, "Empty"))
    errors.append("❌ CRITICAL: No loan provisioning categories defined")

# 4.6 Collateral Types
collateral_types = self.config.get('collateralTypes', [])
if len(collateral_types) > 0:
    validation_results.append(('Collateral Types', True, f"{len(collateral_types)} types"))
    logger.info(f"  ✓ Collateral Types: {len(collateral_types)} types")
else:
    validation_results.append(('Collateral Types', False, "Empty"))
    warnings.append("⚠️  No collateral types defined")

# 4.7 Guarantor Types
guarantor_types = self.config.get('guarantorTypes', [])
if len(guarantor_types) > 0:
    validation_results.append(('Guarantor Types', True, f"{len(guarantor_types)} types"))
    logger.info(f"  ✓ Guarantor Types: {len(guarantor_types)} types")
else:
    validation_results.append(('Guarantor Types', False, "Empty"))
    warnings.append("⚠️  No guarantor types defined")

# 4.8 Floating Rates
floating_rates = self.config.get('floatingRates', [])
if len(floating_rates) > 0:
    validation_results.append(('Floating Rates', True, f"{len(floating_rates)} rates"))
    logger.info(f"  ✓ Floating Rates: {len(floating_rates)} rates")
else:
    validation_results.append(('Floating Rates', False, "Empty"))
    warnings.append("⚠️  No floating rates defined")

# Add to Phase 1 validation section (after Data Tables, around line 870):

# 1.8 Scheduler Jobs
scheduler_jobs = self.config.get('systemConfig', {}).get('schedulerJobs', [])
if len(scheduler_jobs) > 0:
    validation_results.append(('Scheduler Jobs', True, f"{len(scheduler_jobs)} jobs"))
    logger.info(f"  ✓ Scheduler Jobs: {len(scheduler_jobs)} jobs")
else:
    validation_results.append(('Scheduler Jobs', False, "Empty"))
    errors.append("❌ CRITICAL: No scheduler jobs defined")

# 1.9 Maker-Checker Config
maker_checker = self.config.get('systemConfig', {}).get('makerCheckerConfig', [])
if len(maker_checker) > 0:
    validation_results.append(('Maker-Checker Config', True, f"{len(maker_checker)} rules"))
    logger.info(f"  ✓ Maker-Checker Config: {len(maker_checker)} rules")
else:
    validation_results.append(('Maker-Checker Config', False, "Empty"))
    errors.append("❌ CRITICAL: No maker-checker rules defined")

# 1.10 SMS/Email Config
sms_email_config = self.config.get('systemConfig', {}).get('smsEmailConfig', [])
if len(sms_email_config) > 0:
    validation_results.append(('SMS/Email Config', True, f"{len(sms_email_config)} settings"))
    logger.info(f"  ✓ SMS/Email Config: {len(sms_email_config)} settings")
else:
    validation_results.append(('SMS/Email Config', False, "Empty"))
    errors.append("❌ CRITICAL: No SMS/Email configuration")

# 1.11 Holidays
holidays = self.config.get('systemConfig', {}).get('holidays', [])
if len(holidays) > 0:
    validation_results.append(('Holidays', True, f"{len(holidays)} holidays"))
    logger.info(f"  ✓ Holidays: {len(holidays)} holidays")
else:
    validation_results.append(('Holidays', False, "Empty"))
    warnings.append("⚠️  No holidays defined")

# Add to Phase 3 validation (after Financial Activities, around line 970):

# 3.5 Tax Groups
tax_groups = self.config.get('taxGroups', [])
if len(tax_groups) > 0:
    total_components = sum(len(tg.get('taxComponents', [])) for tg in tax_groups)
    validation_results.append(('Tax Groups', True, f"{len(tax_groups)} groups, {total_components} components"))
    logger.info(f"  ✓ Tax Groups: {len(tax_groups)} groups with {total_components} components")
else:
    validation_results.append(('Tax Groups', False, "Empty"))
    errors.append("❌ CRITICAL: No tax groups defined")

# 3.6 Tellers
tellers = self.config.get('tellers', [])
if len(tellers) > 0:
    validation_results.append(('Tellers', True, f"{len(tellers)} tellers"))
    logger.info(f"  ✓ Tellers: {len(tellers)} tellers")
else:
    validation_results.append(('Tellers', False, "Empty"))
    warnings.append("⚠️  No tellers defined")

# 3.7 Teller Cashier Mappings
teller_mappings = self.config.get('tellerCashierMappings', [])
if len(teller_mappings) > 0:
    validation_results.append(('Teller Mappings', True, f"{len(teller_mappings)} mappings"))
    logger.info(f"  ✓ Teller Mappings: {len(teller_mappings)} mappings")
else:
    validation_results.append(('Teller Mappings', False, "Empty"))
    warnings.append("⚠️  No teller cashier mappings")
```

---

## General Patterns & Tips

### Pattern 1: Simple Entity (One Row = One Entity)

```python
df = self._read_sheet('Sheet Name')
if not df.empty:
    entities = []
    for _, row in df.iterrows():
        if not row.get('key_field') or pd.isna(row.get('key_field')):
            continue

        entity = {
            'name': row['key_field'],
            'field2': row.get('other_field', 'default')
        }

        # Optional fields
        if row.get('optional_field') and not pd.isna(row.get('optional_field')):
            entity['optionalField'] = row['optional_field']

        entities.append(entity)

    if entities:
        self.config['entityKey'] = entities
        logger.info(f"  ✓ Entity Name: {len(entities)} items")
```

### Pattern 2: Hierarchical Entity (Multiple Rows = One Entity)

```python
df = self._read_sheet('Sheet Name')
if not df.empty:
    entities_dict = {}
    for _, row in df.iterrows():
        parent_key = row.get('parent_name')
        if not parent_key or pd.isna(parent_key):
            continue

        # Create parent if doesn't exist
        if parent_key not in entities_dict:
            entities_dict[parent_key] = {
                'name': parent_key,
                'children': []
            }

        # Add child item
        child_name = row.get('child_name')
        if child_name and not pd.isna(child_name):
            entities_dict[parent_key]['children'].append({
                'name': child_name
            })

    if entities_dict:
        self.config['entityKey'] = list(entities_dict.values())
        logger.info(f"  ✓ Entity Name: {len(entities_dict)} items")
```

### Pattern 3: Merging Data into Existing Entities

```python
df = self._read_sheet('Sheet Name')
if not df.empty:
    mappings_by_key = {}
    for _, row in df.iterrows():
        key = row.get('entity_key')
        if not key or pd.isna(key):
            continue

        mappings_by_key[key] = {
            'field1': row.get('value1'),
            'field2': row.get('value2')
        }

    # Merge into existing entities
    if 'existingEntities' in self.config and mappings_by_key:
        for entity in self.config['existingEntities']:
            entity_key = entity.get('keyField')
            if entity_key in mappings_by_key:
                entity['additionalData'] = mappings_by_key[entity_key]

        logger.info(f"  ✓ Merged Data: {len(mappings_by_key)} items")
```

### Common Helper Methods

**Parse Boolean**:
```python
self._parse_boolean(value)  # Handles Yes/No, True/False, 1/0
```

**Parse Date**:
```python
self._parse_date(date_value)  # Returns [year, month, day]
```

**Check for NaN**:
```python
if value and not pd.isna(value):
    # Use value
```

### Error Handling Best Practice

Always wrap new converters in try-except:
```python
try:
    # Converter code here
except Exception as e:
    logger.warning(f"  ⚠ Skipping Entity Name: {str(e)}")
```

### Testing Each Converter

After adding each converter, test with:
```bash
python3 scripts/excel_to_yaml.py \
  -i output/fineract_demo_data_20251120_234850.xlsx \
  -o output/test-incremental.yml
```

Check the output for:
1. New entity appears in YAML
2. Validation shows entity as ✓ Pass
3. Field values match Excel data

---

## Implementation Checklist

- [ ] 1. ✅ Fix Global Config validation bug (DONE)
- [ ] 2. ✅ Enhance Loan Products (DONE)
- [ ] 3. Enhance Savings Products
- [ ] 4. Add Delinquency Buckets converter
- [ ] 5. Add Loan Provisioning converter
- [ ] 6. Add Scheduler Jobs converter
- [ ] 7. Add Maker-Checker Config converter
- [ ] 8. Add SMS/Email Config converter
- [ ] 9. Add Holidays converter
- [ ] 10. Add Tax Groups converter
- [ ] 11. Add Tellers converter
- [ ] 12. Add Teller Cashier Mapping converter
- [ ] 13. Add Payment Type Accounting converter
- [ ] 14. Add Collateral Types converter
- [ ] 15. Add Guarantor Types converter
- [ ] 16. Add Floating Rates converter
- [ ] 17. Enhance Clients extraction
- [ ] 18. Fix Groups and Centers staff linking
- [ ] 19. Update validation for all new entities
- [ ] 20. Test complete converter
- [ ] 21. Update documentation

---

## Expected Final Results

**Sheet Coverage**: 43/46 sheets (93%)
**Data Completeness**: 90%+
**Validation Checks**: 38 entity types
**Critical Issues**: 0
**Warnings**: Minor only

**Final Validation Output Should Show**:
```
✅ Data Completeness: 92.0% (35/38 checks passed)
   All critical checks passed! Complete data transfer verified.
```
