# Staff Implementation Guide

## Overview
Staff implementation has been completed as part of Sprint 1 (HIGH PRIORITY). Staff members (loan officers, managers, etc.) can now be configured and loaded into Fineract.

## Implementation Details

### Files Created
1. **Staff.java** - Model representing staff members
   - Location: `src/main/java/org/apache/fineract/config/model/security/Staff.java`
   - Fields: externalId, officeName, firstName, lastName, isLoanOfficer, isActive, joiningDate, mobileNo, emailAddress

2. **StaffLoader.java** - Loader with update support
   - Location: `src/main/java/org/apache/fineract/config/service/loader/StaffLoader.java`
   - Pattern: Pattern B (Manual Change Detection)
   - Features: Upsert support, change detection, dual registration (externalId + full name)

### Files Modified
1. **ChangeDetectionService.java** - Added staff immutable fields
   - Immutable fields: `id`, `externalId`
   - Location: Line 231-233

2. **ImportService.java** - Integrated StaffLoader into Phase 2
   - Loading order: Phase 2, after Users, before Phase 3
   - Depends on: Offices
   - Referenced by: Clients, Groups, Centers, Users

## Configuration Example

### YAML Structure
```yaml
staff:
  - externalId: STAFF-001
    officeName: Head Office
    firstName: Jane
    lastName: Advisor
    isLoanOfficer: true
    isActive: true
    joiningDate: [2023, 1, 15]
    mobileNo: "+237670000001"
    emailAddress: jane.advisor@microfinance.cm

  - externalId: STAFF-002
    officeName: Yaoundé Branch
    firstName: Pierre
    lastName: Lending
    isLoanOfficer: true
    isActive: true
    joiningDate: [2023, 3, 1]
    mobileNo: "+237670000002"
    emailAddress: pierre.lending@microfinance.cm
```

### Field Descriptions
- **externalId** (required): Unique identifier for the staff member
- **officeName** (required): Office where staff is assigned (resolved to officeId)
- **firstName** (required): First name
- **lastName** (required): Last name
- **isLoanOfficer** (optional): Whether staff can be assigned to loans (default: false)
- **isActive** (optional): Active status (default: true)
- **joiningDate** (optional): Date joined in format [year, month, day]
- **mobileNo** (optional): Mobile phone number
- **emailAddress** (optional): Email address

## Testing

### Test Data
Test data has been added to `target/test-classes/phase2-demo-config.yml` with 5 staff members:
- STAFF-001: Jane Advisor (Head Office, Loan Officer, Active)
- STAFF-002: Pierre Lending (Yaoundé Branch, Loan Officer, Active)
- STAFF-003: Sophie Credit (Douala Branch, Loan Officer, Active)
- STAFF-004: Emmanuel Support (Yaoundé North Sub-Branch, Not Loan Officer, Active)
- STAFF-005: Grace Manager (Head Office, Not Loan Officer, Inactive)

### Running Tests
```bash
# Compile the project
mvn clean compile -DskipTests

# Run import with Phase 2 config (includes staff)
java -jar target/fineract-config-cli.jar import -c target/test-classes/phase2-demo-config.yml

# Verify staff loaded
# Check Fineract API: GET /api/v1/staff
```

### Update Testing
Staff loader supports updates with change detection:
1. Modify staff fields in YAML (except externalId)
2. Re-run import
3. Loader will detect changes and update only modified fields
4. UNCHANGED records will be skipped

## Dependencies

### Depends On
- Offices (must be loaded first)

### Referenced By
- Clients (staffName field)
- Groups (staffName field)
- Centers (staffName field)
- Users (can be linked to staff records)
- Loan Accounts (loanOfficerName field)

## API Endpoints
- **GET /api/v1/staff** - List all staff
- **POST /api/v1/staff** - Create staff
- **PUT /api/v1/staff/{id}** - Update staff

## Immutable Fields
The following fields cannot be changed after creation:
- `id` - System-generated ID
- `externalId` - External identifier
- `joiningDate` - May be immutable depending on Fineract version (included in update for compatibility)

## Known Limitations
1. Staff search by externalId requires fetching all staff (no query parameter)
2. Joining date mutability depends on Fineract version
3. No support for staff transfer between offices (would require special API call)

## Next Steps
After Staff implementation is verified:
1. Update Phase 5 configs to reference staff members
2. Test client/loan officer assignments
3. Proceed with Holiday implementation (Sprint 2)
