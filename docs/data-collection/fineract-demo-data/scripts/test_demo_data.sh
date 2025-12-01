#!/bin/bash

# Fineract Demo Data Test Script
# Tests the complete workflow: generate Excel -> load data

set -e  # Exit on error

echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║                  FINERACT DEMO DATA TEST SCRIPT                            ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
function print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

function print_error() {
    echo -e "${RED}✗ $1${NC}"
}

function print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check Python version
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 1: Checking Python installation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    print_success "Python found: $PYTHON_VERSION"
else
    print_error "Python 3 is not installed"
    exit 1
fi

# Check pip
if command -v pip3 &> /dev/null; then
    print_success "pip3 is installed"
else
    print_error "pip3 is not installed"
    exit 1
fi

# Check and install dependencies
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 2: Checking Python dependencies"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

MISSING_DEPS=0

# Check pandas
if python3 -c "import pandas" &> /dev/null; then
    print_success "pandas is installed"
else
    print_error "pandas is not installed"
    MISSING_DEPS=1
fi

# Check openpyxl
if python3 -c "import openpyxl" &> /dev/null; then
    print_success "openpyxl is installed"
else
    print_error "openpyxl is not installed"
    MISSING_DEPS=1
fi

# Check requests
if python3 -c "import requests" &> /dev/null; then
    print_success "requests is installed"
else
    print_error "requests is not installed"
    MISSING_DEPS=1
fi

if [ $MISSING_DEPS -eq 1 ]; then
    echo ""
    print_info "Installing missing dependencies..."
    pip3 install -r ../requirements.txt
    print_success "Dependencies installed"
fi

# Check directory structure
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 3: Verifying directory structure"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

REQUIRED_DIRS=("../output" "../logs" "../config" "../templates" "../docs")

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        print_success "Directory exists: $dir"
    else
        print_info "Creating directory: $dir"
        mkdir -p "$dir"
        print_success "Created: $dir"
    fi
done

# Check config file
if [ -f "../config/fineract_config.json" ]; then
    print_success "Config file exists: ../config/fineract_config.json"
else
    print_error "Config file not found: ../config/fineract_config.json"
    exit 1
fi

# Generate Excel template
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 4: Generating Excel template"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

python3 generate_excel_template.py

if [ $? -eq 0 ]; then
    print_success "Excel template generated successfully"

    # Find the latest Excel file
    EXCEL_FILE=$(ls -t ../output/fineract_demo_data_*.xlsx 2>/dev/null | head -1)

    if [ -n "$EXCEL_FILE" ]; then
        print_success "Excel file: $EXCEL_FILE"
        FILE_SIZE=$(du -h "$EXCEL_FILE" | cut -f1)
        print_info "File size: $FILE_SIZE"
    else
        print_error "No Excel file found in output directory"
        exit 1
    fi
else
    print_error "Failed to generate Excel template"
    exit 1
fi

# Test Fineract connection (optional)
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 5: Testing Fineract connection (optional)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Read config
FINERACT_URL=$(python3 -c "import json; f=open('../config/fineract_config.json'); c=json.load(f); print(c['fineract_url'])")
USERNAME=$(python3 -c "import json; f=open('../config/fineract_config.json'); c=json.load(f); print(c['username'])")
PASSWORD=$(python3 -c "import json; f=open('../config/fineract_config.json'); c=json.load(f); print(c['password'])")

print_info "Testing connection to: $FINERACT_URL"

# Try to connect
if command -v curl &> /dev/null; then
    RESPONSE=$(curl -s -u "$USERNAME:$PASSWORD" -w "%{http_code}" -o /dev/null "$FINERACT_URL/offices" 2>/dev/null || echo "000")

    if [ "$RESPONSE" = "200" ]; then
        print_success "Fineract connection successful (HTTP 200)"
        echo ""
        print_info "You can now load data with:"
        echo "  python3 load_demo_data.py $EXCEL_FILE"
    elif [ "$RESPONSE" = "000" ]; then
        print_error "Cannot connect to Fineract at $FINERACT_URL"
        print_info "Make sure Fineract is running before loading data"
    else
        print_error "Fineract returned HTTP $RESPONSE"
        print_info "Check your credentials in config file"
    fi
else
    print_info "curl not found, skipping connection test"
    print_info "You can manually test with:"
    echo "  curl -u $USERNAME:$PASSWORD $FINERACT_URL/offices"
fi

# Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_success "Environment check complete"
print_success "Excel template generated: $EXCEL_FILE"
echo ""
echo "Next steps:"
echo "  1. Review/modify the Excel file if needed"
echo "  2. Ensure Fineract is running"
echo "  3. Run: python3 load_demo_data.py $EXCEL_FILE"
echo ""
echo "For help, see: ../README.md"
echo ""
