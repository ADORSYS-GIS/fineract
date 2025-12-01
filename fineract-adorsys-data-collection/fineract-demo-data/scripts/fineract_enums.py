"""
Fineract Enum Mapping and Validation Module

This module provides centralized enum validation for the Excel-to-YAML converter,
ensuring that only valid enum values are written to YAML files.

This Python module mirrors the Java FineractEnumMapper.java to maintain consistency
between the YAML generation (Python) and YAML import (Java) pipelines.

Reference: fineract-config-cli/src/main/java/org/apache/fineract/config/util/FineractEnumMapper.java
Audit Source: AUDIT_REPORT.md - 28 enum methods across 11 loaders

Usage:
    from fineract_enums import validate_enum, get_valid_values

    # Validate enum value
    if not validate_enum('ChargeAppliesTo', 'LOAN'):
        print(f"Invalid value! Valid options: {get_valid_values('ChargeAppliesTo')}")

    # Get enum ID
    from fineract_enums import ChargeAppliesTo
    loan_id = ChargeAppliesTo.LOAN  # Returns 1
"""

from enum import IntEnum
from typing import Optional, List
import logging

logger = logging.getLogger(__name__)


# ===========================================================================================
# CHARGE ENUMS (ChargeLoader.java - 5 enums)
# ===========================================================================================

class ChargeAppliesTo(IntEnum):
    """
    Enum for charge application scope.

    Fineract API values:
    - LOAN = 1
    - SAVINGS = 2
    - CLIENT = 3
    - SHARES = 4
    """
    LOAN = 1
    SAVINGS = 2
    CLIENT = 3
    SHARES = 4


class ChargeTimeType(IntEnum):
    """
    Enum for when a charge is applied.

    Fineract API values:
    - DISBURSEMENT = 1
    - SPECIFIED_DUE_DATE = 2
    - INSTALMENT_FEE = 3
    - OVERDUE_INSTALLMENT = 4
    - SAVINGS_ACTIVATION = 5
    - WITHDRAWAL_FEE = 6
    - ANNUAL_FEE = 7
    - MONTHLY_FEE = 8
    - WEEKLY_FEE = 9
    """
    DISBURSEMENT = 1
    SPECIFIED_DUE_DATE = 2
    INSTALMENT_FEE = 3
    INSTALLMENT_FEE = 3  # Alias for American spelling
    OVERDUE_INSTALLMENT = 4
    OVERDUE_INSTALMENT = 4  # Alias for British spelling
    SAVINGS_ACTIVATION = 5
    WITHDRAWAL_FEE = 6
    ANNUAL_FEE = 7
    MONTHLY_FEE = 8
    WEEKLY_FEE = 9


class ChargeCalculationType(IntEnum):
    """
    Enum for how charge amounts are calculated.

    Fineract API values:
    - FLAT = 1
    - PERCENT_OF_AMOUNT = 2
    - PERCENT_OF_AMOUNT_AND_INTEREST = 3
    - PERCENT_OF_INTEREST = 4
    - PERCENT_OF_DISBURSEMENT_AMOUNT = 5
    """
    FLAT = 1
    PERCENT_OF_AMOUNT = 2
    PERCENT_OF_AMOUNT_AND_INTEREST = 3
    PERCENT_OF_INTEREST = 4
    PERCENT_OF_DISBURSEMENT_AMOUNT = 5


class ChargePaymentMode(IntEnum):
    """
    Enum for charge payment modes.

    Fineract API values:
    - REGULAR = 0
    - ACCOUNT_TRANSFER = 1
    """
    REGULAR = 0
    ACCOUNT_TRANSFER = 1


class FeeInterval(IntEnum):
    """
    Enum for recurring fee intervals.

    Fineract API values:
    - DAYS = 0
    - WEEKS = 1
    - MONTHS = 2
    - YEARS = 3
    """
    DAYS = 0
    DAY = 0  # Alias
    WEEKS = 1
    WEEK = 1  # Alias
    MONTHS = 2
    MONTH = 2  # Alias
    YEARS = 3
    YEAR = 3  # Alias


# ===========================================================================================
# CHART OF ACCOUNTS ENUMS (ChartOfAccountsLoader.java - 2 enums)
# ===========================================================================================

class GLAccountType(IntEnum):
    """
    Enum for General Ledger account types.

    Fineract API values:
    - ASSET = 1
    - LIABILITY = 2
    - EQUITY = 3
    - INCOME = 4
    - EXPENSE = 5
    """
    ASSET = 1
    LIABILITY = 2
    EQUITY = 3
    INCOME = 4
    EXPENSE = 5


class GLAccountUsage(IntEnum):
    """
    Enum for GL account usage (detail vs header).

    Fineract API values:
    - DETAIL = 1
    - HEADER = 2
    """
    DETAIL = 1
    HEADER = 2


# ===========================================================================================
# CLIENT ENUMS (ClientLoader.java - 1 enum)
# ===========================================================================================

class Gender(IntEnum):
    """
    Enum for client gender.

    WARNING: These IDs reference system code values that may not exist in all
    Fineract instances. Verify code values exist before using.

    Common Fineract API values:
    - MALE = 22
    - FEMALE = 23
    - OTHER = 24
    """
    MALE = 22
    M = 22  # Alias
    FEMALE = 23
    F = 23  # Alias
    OTHER = 24
    O = 24  # Alias


# ===========================================================================================
# FINANCIAL ACTIVITY ENUMS (FinancialActivityMappingLoader.java - 1 enum)
# ===========================================================================================

class FinancialActivity(IntEnum):
    """
    Enum for financial activity types (hundred-based numbering).

    Fineract API values:
    - ASSET_FUND_SOURCE = 100
    - LIABILITY_FUND_SOURCE = 101
    - ASSET_TRANSFER = 200
    - LIABILITY_TRANSFER = 201
    - OPENING_BALANCES_TRANSFER_CONTRA = 202
    - CASH_AT_MAINVAULT = 300
    - CASH_AT_TELLER = 301
    """
    ASSET_FUND_SOURCE = 100
    LIABILITY_FUND_SOURCE = 101
    ASSET_TRANSFER = 200
    LIABILITY_TRANSFER = 201
    OPENING_BALANCES_TRANSFER_CONTRA = 202
    CASH_AT_MAINVAULT = 300
    CASH_AT_TELLER = 301


# ===========================================================================================
# FREQUENCY ENUMS (shared across multiple loaders - 4 enums)
# ===========================================================================================

class RepaymentFrequencyType(IntEnum):
    """
    Enum for loan repayment frequency.

    Used by: LoanAccountLoader, LoanProductLoader

    Fineract API values:
    - DAYS = 0
    - WEEKS = 1
    - MONTHS = 2
    - YEARS = 3
    """
    DAYS = 0
    DAY = 0  # Alias
    WEEKS = 1
    WEEK = 1  # Alias
    MONTHS = 2
    MONTH = 2  # Alias
    YEARS = 3
    YEAR = 3  # Alias


class InterestType(IntEnum):
    """
    Enum for loan interest calculation method.

    Used by: LoanAccountLoader, LoanProductLoader

    Fineract API values:
    - DECLINING_BALANCE = 0
    - FLAT = 1
    """
    DECLINING_BALANCE = 0
    DECLINING = 0  # Alias
    FLAT = 1


class AmortizationType(IntEnum):
    """
    Enum for loan amortization type.

    Used by: LoanAccountLoader, LoanProductLoader

    Fineract API values:
    - EQUAL_PRINCIPAL = 0
    - EQUAL_INSTALLMENTS = 1
    """
    EQUAL_PRINCIPAL = 0
    EQUAL_INSTALLMENTS = 1
    EQUAL_INSTALMENTS = 1  # Alias for British spelling


class InterestCalculationPeriodType(IntEnum):
    """
    Enum for interest calculation period.

    Used by: LoanAccountLoader, LoanProductLoader

    Fineract API values:
    - DAILY = 0
    - SAME_AS_REPAYMENT_PERIOD = 1
    """
    DAILY = 0
    SAME_AS_REPAYMENT_PERIOD = 1


# ===========================================================================================
# LOAN PRODUCT SPECIFIC ENUMS (LoanProductLoader.java - 2 enums)
# ===========================================================================================

class InterestRateFrequencyType(IntEnum):
    """
    Enum for interest rate frequency.

    Used by: LoanProductLoader

    Fineract API values:
    - MONTH = 2
    - YEAR = 3
    """
    MONTH = 2
    MONTHS = 2  # Alias
    PER_MONTH = 2  # Alias
    YEAR = 3
    YEARS = 3  # Alias
    PER_YEAR = 3  # Alias


class AccountingRule(IntEnum):
    """
    Enum for product accounting rules.

    Used by: LoanProductLoader, SavingsProductLoader

    Fineract API values:
    - NONE = 1
    - CASH_BASED = 2
    - ACCRUAL_PERIODIC = 3 (Loan products only)
    - ACCRUAL_UPFRONT = 4 (Loan products only)
    """
    NONE = 1
    CASH_BASED = 2
    CASH = 2  # Alias
    ACCRUAL_PERIODIC = 3
    PERIODIC = 3  # Alias
    ACCRUAL_UPFRONT = 4
    UPFRONT = 4  # Alias


# ===========================================================================================
# SAVINGS PRODUCT ENUMS (SavingsProductLoader.java - 4 enums)
# ===========================================================================================

class InterestCompoundingPeriodType(IntEnum):
    """
    Enum for interest compounding period.

    Used by: SavingsProductLoader

    Fineract API values:
    - DAILY = 1
    - MONTHLY = 4
    - QUARTERLY = 5
    - SEMI_ANNUAL = 6
    - ANNUAL = 7
    """
    DAILY = 1
    MONTHLY = 4
    MONTH = 4  # Alias
    QUARTERLY = 5
    QUARTER = 5  # Alias
    SEMI_ANNUAL = 6
    SEMIANNUAL = 6  # Alias
    BIANNUAL = 6  # Alias
    ANNUAL = 7
    YEARLY = 7  # Alias
    YEAR = 7  # Alias


class InterestPostingPeriodType(IntEnum):
    """
    Enum for interest posting period.

    Used by: SavingsProductLoader

    Fineract API values:
    - MONTHLY = 4
    - QUARTERLY = 5
    - BIANNUAL = 6
    - ANNUAL = 7
    """
    MONTHLY = 4
    MONTH = 4  # Alias
    QUARTERLY = 5
    QUARTER = 5  # Alias
    BIANNUAL = 6
    SEMI_ANNUAL = 6  # Alias
    SEMIANNUAL = 6  # Alias
    ANNUAL = 7
    YEARLY = 7  # Alias
    YEAR = 7  # Alias


class InterestCalculationType(IntEnum):
    """
    Enum for savings interest calculation method.

    Used by: SavingsProductLoader

    Fineract API values:
    - DAILY_BALANCE = 1
    - AVERAGE_DAILY_BALANCE = 2
    """
    DAILY_BALANCE = 1
    AVERAGE_DAILY_BALANCE = 2


class InterestCalculationDaysInYearType(IntEnum):
    """
    Enum for days in year for interest calculation.

    Used by: SavingsProductLoader

    Fineract API values:
    - DAYS_360 = 360
    - DAYS_365 = 365
    """
    DAYS_360 = 360
    DAYS_365 = 365


# ===========================================================================================
# TRANSACTION TYPE ENUMS (Command string mapping - NOT integer IDs)
# ===========================================================================================

class LoanTransactionType:
    """
    Enum for loan transaction types (maps to command strings, not IDs).

    Used by: LoanTransactionLoader

    Fineract API commands:
    - REPAYMENT → "repayment"
    - WAIVER → "waiveinterest"
    - WRITEOFF → "writeoff"
    """
    REPAYMENT = "repayment"
    WAIVER = "waiveinterest"
    WAIVE_INTEREST = "waiveinterest"  # Alias
    WRITEOFF = "writeoff"
    WRITE_OFF = "writeoff"  # Alias


class SavingsTransactionType:
    """
    Enum for savings transaction types (maps to command strings, not IDs).

    Used by: SavingsTransactionLoader

    Fineract API commands:
    - DEPOSIT → "deposit"
    - WITHDRAWAL → "withdrawal"
    """
    DEPOSIT = "deposit"
    WITHDRAWAL = "withdrawal"
    WITHDRAW = "withdrawal"  # Alias


# ===========================================================================================
# WORKING DAYS ENUM (WorkingDaysLoader.java - 1 enum - REFERENCE IMPLEMENTATION)
# ===========================================================================================

class RepaymentRescheduleType(IntEnum):
    """
    Enum for repayment rescheduling when due date falls on non-working day.

    REFERENCE IMPLEMENTATION - This is the original pattern that informed all other enums.

    Used by: WorkingDaysLoader

    Fineract API values:
    - SAME_DAY = 1
    - MOVE_TO_NEXT_WORKING_DAY = 2
    - MOVE_TO_NEXT_REPAYMENT_MEETING_DAY = 3
    - MOVE_TO_PREVIOUS_WORKING_DAY = 4
    - MOVE_TO_NEXT_MEETING_DAY = 5
    """
    SAME_DAY = 1
    MOVE_TO_NEXT_WORKING_DAY = 2
    MOVE_TO_NEXT_REPAYMENT_MEETING_DAY = 3
    MOVE_TO_PREVIOUS_WORKING_DAY = 4
    MOVE_TO_NEXT_MEETING_DAY = 5


# ===========================================================================================
# VALIDATION UTILITIES
# ===========================================================================================

# Registry of all enum classes
ENUM_REGISTRY = {
    'ChargeAppliesTo': ChargeAppliesTo,
    'ChargeTimeType': ChargeTimeType,
    'ChargeCalculationType': ChargeCalculationType,
    'ChargePaymentMode': ChargePaymentMode,
    'FeeInterval': FeeInterval,
    'GLAccountType': GLAccountType,
    'GLAccountUsage': GLAccountUsage,
    'Gender': Gender,
    'FinancialActivity': FinancialActivity,
    'RepaymentFrequencyType': RepaymentFrequencyType,
    'InterestType': InterestType,
    'AmortizationType': AmortizationType,
    'InterestCalculationPeriodType': InterestCalculationPeriodType,
    'InterestRateFrequencyType': InterestRateFrequencyType,
    'AccountingRule': AccountingRule,
    'InterestCompoundingPeriodType': InterestCompoundingPeriodType,
    'InterestPostingPeriodType': InterestPostingPeriodType,
    'InterestCalculationType': InterestCalculationType,
    'InterestCalculationDaysInYearType': InterestCalculationDaysInYearType,
    'LoanTransactionType': LoanTransactionType,
    'SavingsTransactionType': SavingsTransactionType,
    'RepaymentRescheduleType': RepaymentRescheduleType,
}


def validate_enum(enum_name: str, value: str) -> bool:
    """
    Validates if a value is valid for the specified enum.

    Args:
        enum_name: Name of the enum class (e.g., 'ChargeAppliesTo')
        value: Value to validate (e.g., 'LOAN')

    Returns:
        True if value is valid for the enum, False otherwise

    Example:
        >>> validate_enum('ChargeAppliesTo', 'LOAN')
        True
        >>> validate_enum('ChargeAppliesTo', 'INVALID')
        False
    """
    if enum_name not in ENUM_REGISTRY:
        logger.warning(f"Unknown enum type: {enum_name}")
        return False

    enum_class = ENUM_REGISTRY[enum_name]

    # For string-based enums (transaction types)
    if not issubclass(enum_class, IntEnum):
        return hasattr(enum_class, value.upper())

    # For IntEnum classes
    try:
        # Try to get by name
        return value.upper() in enum_class.__members__
    except (AttributeError, TypeError):
        return False


def get_valid_values(enum_name: str) -> List[str]:
    """
    Gets all valid values for the specified enum.

    Args:
        enum_name: Name of the enum class

    Returns:
        List of valid enum value names

    Example:
        >>> get_valid_values('ChargeAppliesTo')
        ['LOAN', 'SAVINGS', 'CLIENT', 'SHARES']
    """
    if enum_name not in ENUM_REGISTRY:
        logger.warning(f"Unknown enum type: {enum_name}")
        return []

    enum_class = ENUM_REGISTRY[enum_name]

    # For string-based enums
    if not issubclass(enum_class, IntEnum):
        return [name for name in dir(enum_class)
                if not name.startswith('_') and name.isupper()]

    # For IntEnum classes
    return list(enum_class.__members__.keys())


def get_enum_value(enum_name: str, value: str) -> Optional[int]:
    """
    Gets the integer ID for an enum value name.

    Args:
        enum_name: Name of the enum class
        value: Enum value name (e.g., 'LOAN')

    Returns:
        Integer ID if valid, None otherwise

    Example:
        >>> get_enum_value('ChargeAppliesTo', 'LOAN')
        1
        >>> get_enum_value('ChargeAppliesTo', 'INVALID')
        None
    """
    if enum_name not in ENUM_REGISTRY:
        logger.warning(f"Unknown enum type: {enum_name}")
        return None

    enum_class = ENUM_REGISTRY[enum_name]

    # For string-based enums, return the string value
    if not issubclass(enum_class, IntEnum):
        if hasattr(enum_class, value.upper()):
            return getattr(enum_class, value.upper())
        return None

    # For IntEnum classes
    try:
        return enum_class[value.upper()].value
    except (KeyError, AttributeError):
        return None


def validate_and_warn(enum_name: str, value: str, context: str = "") -> bool:
    """
    Validates enum value and logs a warning if invalid.

    Args:
        enum_name: Name of the enum class
        value: Value to validate
        context: Additional context for the warning message

    Returns:
        True if valid, False if invalid (with warning logged)

    Example:
        >>> validate_and_warn('ChargeAppliesTo', 'INVALID', 'Row 5, Column B')
        WARNING: Invalid ChargeAppliesTo value 'INVALID' at Row 5, Column B.
                 Valid values: LOAN, SAVINGS, CLIENT, SHARES
        False
    """
    if validate_enum(enum_name, value):
        return True

    valid_values = get_valid_values(enum_name)
    context_str = f" at {context}" if context else ""
    logger.warning(
        f"Invalid {enum_name} value '{value}'{context_str}. "
        f"Valid values: {', '.join(valid_values)}"
    )
    return False


# ===========================================================================================
# FIELD-SPECIFIC VALIDATORS (for use in Excel-to-YAML conversion)
# ===========================================================================================

def validate_charge_applies_to(value: str, context: str = "") -> bool:
    """Validates ChargeAppliesTo enum value."""
    return validate_and_warn('ChargeAppliesTo', value, context)


def validate_charge_time_type(value: str, context: str = "") -> bool:
    """Validates ChargeTimeType enum value."""
    return validate_and_warn('ChargeTimeType', value, context)


def validate_charge_calculation_type(value: str, context: str = "") -> bool:
    """Validates ChargeCalculationType enum value."""
    return validate_and_warn('ChargeCalculationType', value, context)


def validate_accounting_rule(value: str, context: str = "") -> bool:
    """Validates AccountingRule enum value."""
    return validate_and_warn('AccountingRule', value, context)


def validate_repayment_frequency(value: str, context: str = "") -> bool:
    """Validates RepaymentFrequencyType enum value."""
    return validate_and_warn('RepaymentFrequencyType', value, context)


def validate_interest_type(value: str, context: str = "") -> bool:
    """Validates InterestType enum value."""
    return validate_and_warn('InterestType', value, context)


def validate_amortization_type(value: str, context: str = "") -> bool:
    """Validates AmortizationType enum value."""
    return validate_and_warn('AmortizationType', value, context)


if __name__ == "__main__":
    # Self-test
    print("Fineract Enum Validation Module - Self Test")
    print("=" * 60)

    # Test 1: Valid enum value
    print("\nTest 1: Valid enum value")
    result = validate_enum('ChargeAppliesTo', 'LOAN')
    print(f"  validate_enum('ChargeAppliesTo', 'LOAN') = {result}")
    assert result is True

    # Test 2: Invalid enum value
    print("\nTest 2: Invalid enum value")
    result = validate_enum('ChargeAppliesTo', 'INVALID')
    print(f"  validate_enum('ChargeAppliesTo', 'INVALID') = {result}")
    assert result is False

    # Test 3: Get valid values
    print("\nTest 3: Get valid values")
    values = get_valid_values('ChargeAppliesTo')
    print(f"  get_valid_values('ChargeAppliesTo') = {values}")
    assert 'LOAN' in values

    # Test 4: Get enum ID
    print("\nTest 4: Get enum ID")
    enum_id = get_enum_value('ChargeAppliesTo', 'LOAN')
    print(f"  get_enum_value('ChargeAppliesTo', 'LOAN') = {enum_id}")
    assert enum_id == 1

    # Test 5: Case-insensitive validation
    print("\nTest 5: Case-insensitive validation")
    result = validate_enum('ChargeAppliesTo', 'loan')
    print(f"  validate_enum('ChargeAppliesTo', 'loan') = {result}")
    assert result is True

    # Test 6: Alias support
    print("\nTest 6: Alias support (MONTH → MONTHS)")
    enum_id = get_enum_value('RepaymentFrequencyType', 'MONTH')
    print(f"  get_enum_value('RepaymentFrequencyType', 'MONTH') = {enum_id}")
    assert enum_id == 2

    print("\n" + "=" * 60)
    print("All tests passed! ✓")
