#!/bin/bash

# Quick Start Script for Fineract Demo Data
# This script automates the entire process

set -e

echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║          FINERACT DEMO DATA - QUICK START                                  ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Check if config file exists
if [ ! -f "config/fineract_config.json" ]; then
    echo "❌ Config file not found: config/fineract_config.json"
    echo "Please create it first. See README.md for details."
    exit 1
fi

# Install dependencies
echo "📦 Installing Python dependencies..."
pip3 install -r requirements.txt -q

# Navigate to scripts directory
cd scripts

# Generate Excel
echo ""
echo "📊 Generating Excel template..."
python3 generate_excel_template.py

# Find latest Excel file
EXCEL_FILE=$(ls -t ../output/fineract_demo_data_*.xlsx 2>/dev/null | head -1)

if [ -z "$EXCEL_FILE" ]; then
    echo "❌ No Excel file generated"
    exit 1
fi

echo "✅ Excel template created: $EXCEL_FILE"
echo ""

# Ask user if they want to load data now
read -p "Do you want to load data into Fineract now? (y/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🚀 Loading data into Fineract..."
    python3 load_demo_data.py "$EXCEL_FILE"

    if [ $? -eq 0 ]; then
        echo ""
        echo "╔════════════════════════════════════════════════════════════════════════════╗"
        echo "║                    ✅ SUCCESS!                                             ║"
        echo "╚════════════════════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Demo data has been loaded successfully!"
        echo ""
        echo "You can now:"
        echo "  - Login to Fineract with any staff username (e.g., manager.douala)"
        echo "  - Default password: password"
        echo "  - Browse clients, loans, savings accounts"
        echo ""
        echo "Check the logs for details: logs/load_demo_data.log"
    else
        echo ""
        echo "❌ Data loading failed. Check logs/load_demo_data.log for details."
        exit 1
    fi
else
    echo ""
    echo "📝 Excel file ready at: $EXCEL_FILE"
    echo ""
    echo "To load data later, run:"
    echo "  cd scripts"
    echo "  python3 load_demo_data.py $EXCEL_FILE"
fi

cd ..
