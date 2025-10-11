# Quick Reference Card

## Installation

```bash
cd fineract-demo-data
pip3 install -r requirements.txt
```

## Generate Excel Template

```bash
cd scripts
python3 generate_excel_template.py
```

Output: `output/fineract_demo_data_YYYYMMDD_HHMMSS.xlsx`

## Load Data into Fineract

```bash
cd scripts
python3 load_demo_data.py ../output/fineract_demo_data_*.xlsx
```

## Quick Start (All-in-One)

```bash
./quickstart.sh
```

## Configuration

Edit `config/fineract_config.json`:

```json
{
  "fineract_url": "http://localhost:8080/fineract-provider/api/v1",
  "username": "mifos",
  "password": "password",
  "tenant": "default"
}
```

## Test Environment

```bash
cd scripts
./test_demo_data.sh
```

## Check Logs

```bash
tail -f logs/load_demo_data.log
```

## Default Credentials

- **Username**: `manager.douala`, `loan.douala`, `cashier.douala`, etc.
- **Password**: `password` (change immediately!)

## Excel Sheets (33 Total)

1. Offices (4)
2. Staff (12)
3. Clients (12)
4. Loan Products (3)
5. Savings Products (3)
6. Charges (11)
7. Chart of Accounts (30)
8. Loan Accounts (6)
9. Savings Accounts (12)
10. Fund Sources (6)
11. Payment Types (5)
12. Holidays (12)
13. Loan Provisioning (5)
14. Collateral Types (6)
15. Guarantor Types (6)
16. Configuration (19)
17. Loan Product Accounting (39)
18. Savings Product Accounting (33)
19. Payment Type Accounting (5)
20. Financial Activity Mapping (6)
21. Teller/Cashier Office Mappings (4)
22. Tellers (4 tellers - 1 per office)
23. Roles & Permissions (22 - Reference Only)
24. Maker-Checker Configuration (12)
25. Currency Configuration (1 - XAF)
26. Working Days (7 days)
27. Account Number Preferences (5 entity types)
28. Codes and Code Values (43 values across 9 categories)
29. Scheduler Jobs (10 automated jobs)
30. Global Configuration (23 system settings)
31. SMS/Email Config (17 configuration items - Twilio, Gmail SMTP)
32. Notification Templates (16 templates - SMS/Email for all events)
33. Data Tables (24 custom fields - Client, Loan, Savings)

## Data Summary

- **Total Offices**: 4 (Yaounde, Douala, Bafoussam, Bamenda)
- **Total Staff**: 12 (4 managers, 4 loan officers, 4 cashiers)
- **Total Clients**: 12 with complete profiles
- **Active Loans**: 6 (total 6,030,000 XAF)
- **Savings Accounts**: 12 with various balances
- **GL Accounts**: 30 (OHADA-compliant)
- **Roles**: 3 (Branch Manager, Loan Officer, Cashier)
- **Maker-Checker Rules**: 12 dual-authorization rules

## API Endpoints Used

| Entity | Endpoint |
|--------|----------|
| **System Configuration** | |
| Currency | `GET /currencies` (fetch) |
| Working Days | `PUT /workingdays` |
| Global Config | `GET /configurations`, `PUT /configurations/{id}` |
| Codes | `POST /codes`, `POST /codes/{id}/codevalues` |
| Account Numbers | `POST /accountnumberformats` |
| Scheduler Jobs | `GET /jobs`, `PUT /jobs/{id}` |
| Data Tables | `POST /datatables` (custom fields) |
| **Organizational Setup** | |
| Offices | `POST /offices` |
| Staff | `POST /staff` |
| Roles | `GET /roles` (fetch existing roles) |
| Users | `POST /users` (with role assignment) |
| **Accounting** | |
| GL Accounts | `POST /glaccounts` |
| Charges | `POST /charges` |
| Funds | `POST /funds` |
| Payment Types | `POST /paymenttypes` |
| Holidays | `POST /holidays` |
| Financial Activities | `POST /financialactivityaccounts` |
| **Security** | |
| Maker-Checker | `POST /makercheckers` (enable) |
| **Products & Accounts** | |
| Loan Products | `POST /loanproducts` |
| Savings Products | `POST /savingsproducts` |
| Clients | `POST /clients` |
| Savings Accounts | `POST /savingsaccounts` |
| Loans | `POST /loans` |

## Common Commands

### Verify Fineract is Running

```bash
curl -u mifos:password http://localhost:8080/fineract-provider/api/v1/offices
```

### Check Created Offices

```bash
curl -u mifos:password http://localhost:8080/fineract-provider/api/v1/offices | jq
```

### Check Created Clients

```bash
curl -u mifos:password http://localhost:8080/fineract-provider/api/v1/clients | jq
```

### View Logs in Real-Time

```bash
tail -f logs/load_demo_data.log
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Module not found | `pip3 install -r requirements.txt` |
| Connection refused | Check Fineract is running |
| 401 Unauthorized | Check credentials in config |
| 404 Not Found | Verify Fineract URL |
| Data loading fails | Check logs/load_demo_data.log |

## File Locations

- **Scripts**: `scripts/`
- **Config**: `config/fineract_config.json`
- **Output**: `output/*.xlsx`
- **Logs**: `logs/load_demo_data.log`
- **Docs**: `README.md`, `PROJECT_SUMMARY.md`

## Customization

1. Generate Excel template
2. Open `output/fineract_demo_data_*.xlsx`
3. Modify data as needed
4. Save file
5. Run loader script

## Security Checklist

**Passwords & Access**:
- [ ] Change default passwords (default: `password`)
- [ ] Use HTTPS in production (not HTTP)
- [ ] Protect config file (chmod 600)
- [ ] Restrict API access via firewall
- [ ] Enable audit logs
- [ ] Backup database daily

**Role-Based Access Control**:
- [ ] Create custom roles in Fineract UI (Branch Manager, Loan Officer, Cashier)
- [ ] Never assign "Super user" to operational staff in production
- [ ] Implement segregation of duties (makers vs checkers)
- [ ] Review user permissions quarterly
- [ ] Disable terminated employee accounts immediately

**Maker-Checker**:
- [ ] Verify Maker-Checker is enabled for critical operations
- [ ] Never assign MAKER and CHECKER roles to same user
- [ ] Train managers on approval process
- [ ] Monitor checker inbox daily
- [ ] Generate weekly Maker-Checker activity reports

## Project Structure

```
fineract-demo-data/
├── config/               # Configuration
├── scripts/              # Python scripts
├── output/               # Generated Excel files
├── logs/                 # Execution logs
├── templates/            # Custom templates
├── docs/                 # Documentation
├── README.md             # Full documentation
├── requirements.txt      # Dependencies
└── quickstart.sh         # Quick start script
```

## Support

- **Full docs**: `README.md`
- **Project summary**: `PROJECT_SUMMARY.md`
- **Logs**: `logs/load_demo_data.log`
- **Fineract docs**: https://fineract.apache.org/

## Version

v1.3.0 (2024-10-10)

**What's New in v1.3.0**:
- 1 new sheet (22): Tellers
- 33 total sheets (expanded from 32)
- Physical cash counters created at each branch (4 tellers - 1 per office)
- Teller entities enable daily cash allocation and settlement tracking
- Complete cash management workflow with GL mappings

**Previous - v1.2.0**:
- 3 new sheets (30-32): SMS/Email Config, Notification Templates, Data Tables (Custom Fields)
- 32 total sheets (expanded from 29)
- SMS/Email notification setup with Twilio & Gmail SMTP (17 config items)
- 16 ready-to-use notification templates for all lifecycle events
- 24 custom fields for Client, Loan, and Savings entities
- Enhanced KYC capabilities and client communication

**Previous - v1.1.0**:
- 6 new sheets (24-29): Currency Config, Working Days, Account Number Preferences, Codes & Values, Scheduler Jobs, Global Configuration
- 29 total sheets (expanded from 23)
- Comprehensive system configuration support
- 43 dropdown values across 9 code categories
- 10 automated scheduler jobs
- 23 global system settings

---

**For detailed information, see README.md**
