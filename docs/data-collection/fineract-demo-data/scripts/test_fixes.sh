#!/bin/bash
# Test script to verify all validation fixes

echo "================================================================================================"
echo "FINERACT DEMO DATA LOADER - VALIDATION FIXES TEST"
echo "================================================================================================"
echo ""
echo "This script will test all the fixes applied to resolve validation errors:"
echo ""
echo "✓ Fix 1: Data Tables - length parameter validation"
echo "✓ Fix 2: Loan Products - transactionProcessingStrategyCode parameter"
echo "✓ Fix 3: Savings Products - shortName length (max 4 characters)"
echo "✓ Fix 4: Charges - validation errors for penalties, monthly/annual fees"
echo "✓ Fix 5: Maker-Checker - changed to manual configuration notice"
echo "✓ Fix 6: Offices - use existing Head Office (ID: 1)"
echo ""
echo "================================================================================================"
echo ""

# Change to script directory
cd "$(dirname "$0")"

# Check if Fineract is running
echo "Checking if Fineract is accessible..."
if curl -k -s --head --request GET https://localhost/fineract-provider/api/v1/offices \
    --user mifos:password \
    --header "Fineract-Platform-TenantId: default" | grep "200 OK" > /dev/null; then
    echo "✓ Fineract is running and accessible"
else
    echo "✗ ERROR: Fineract is not accessible at https://localhost/fineract-provider/api/v1"
    echo "  Please ensure Fineract is running before running this script."
    exit 1
fi

echo ""
echo "================================================================================================"
echo "Starting data load test..."
echo "================================================================================================"
echo ""

# Run the loader
python3 load_demo_data.py ../output/fineract_demo_data_20251010_225712.xlsx ../config/fineract_config.json

exit_code=$?

echo ""
echo "================================================================================================"
echo "TEST COMPLETED"
echo "================================================================================================"
echo ""

if [ $exit_code -eq 0 ]; then
    echo "✓ All validation fixes appear to be working correctly!"
    echo ""
    echo "Summary of fixes applied:"
    echo "  1. Data Tables now have proper length parameters"
    echo "  2. Loan Products use transactionProcessingStrategyCode"
    echo "  3. Savings Products have shortened names (max 4 chars)"
    echo "  4. Charges have proper configuration for penalties and recurring fees"
    echo "  5. Maker-Checker shows manual configuration instructions"
    echo "  6. Offices correctly use existing Head Office (ID: 1)"
    echo ""
else
    echo "⚠ Some errors may have occurred. Please check the logs above."
    echo ""
fi

echo "For detailed logs, check: ../logs/load_demo_data.log"
echo ""

exit $exit_code
