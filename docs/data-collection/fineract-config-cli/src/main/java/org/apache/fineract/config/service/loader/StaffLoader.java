package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.security.Staff;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.apache.fineract.config.util.InputValidator;
import org.apache.fineract.config.util.InputValidator.ValidationResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for staff members.
 *
 * <p>Creates and updates staff member records in the Fineract system.
 *
 * <p>Staff members are referenced by:
 *
 * <ul>
 *   <li>Users - can be linked to staff records
 *   <li>Clients - assigned loan officer
 *   <li>Groups - assigned staff
 *   <li>Centers - assigned staff
 * </ul>
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/staff - List all staff
 *   <li>POST /api/v1/staff - Create staff
 *   <li>PUT /api/v1/staff/{id} - Update staff
 * </ul>
 */
@Slf4j
@Component
public class StaffLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;
  private final InputValidator inputValidator;

  public StaffLoader(
      FineractApiClient apiClient,
      ChangeDetectionService changeDetectionService,
      InputValidator inputValidator) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
    this.inputValidator = inputValidator;
  }

  /**
   * Loads staff members.
   *
   * @param staffList list of staff members
   * @param context import context
   * @param result import result
   */
  public void load(List<Staff> staffList, ImportContext context, ImportResult result) {
    log.debug("Loading {} staff members", staffList.size());

    for (Staff staff : staffList) {
      try {
        loadSingleStaff(staff, context, result);
      } catch (Exception ex) {
        log.error("Failed to load staff '{}': {}", staff.getExternalId(), ex.getMessage());
        result.recordEntity("staff", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single staff member.
   *
   * @param staff staff member to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleStaff(Staff staff, ImportContext context, ImportResult result) {
    log.debug("Loading staff: {}", staff.getExternalId());

    // Validate input data
    List<ValidationResult> validationResults = new ArrayList<>();
    validationResults.add(inputValidator.validateName(staff.getFirstName(), "firstName"));
    validationResults.add(inputValidator.validateName(staff.getLastName(), "lastName"));
    validationResults.add(inputValidator.validateExternalId(staff.getExternalId(), "externalId"));
    validationResults.add(inputValidator.validateEmail(staff.getEmailAddress(), "emailAddress"));
    validationResults.add(inputValidator.validatePhone(staff.getMobileNo(), "mobileNo"));
    validationResults.add(
        inputValidator.validateEntityIdentifier(staff.getOfficeName(), "officeName"));

    ValidationResult validation = inputValidator.validateAll(validationResults);
    if (!validation.isValid()) {
      String errors = validation.getErrorMessage();
      log.error("Validation failed for staff '{}': {}", staff.getExternalId(), errors);
      throw new IllegalArgumentException("Invalid staff data: " + errors);
    }

    Long staffId = null;
    Map<String, Object> existingStaff = null;

    // Check if staff already exists by external ID
    if (staff.getExternalId() != null) {
      try {
        List<Map<String, Object>> allStaff = apiClient.get("/api/v1/staff", List.class);

        existingStaff =
            allStaff.stream()
                .filter(s -> staff.getExternalId().equals(s.get("externalId")))
                .findFirst()
                .orElse(null);

        if (existingStaff != null) {
          staffId = ((Number) existingStaff.get("id")).longValue();
          log.debug("Staff already exists: {} (ID: {})", staff.getExternalId(), staffId);
        }
      } catch (Exception ex) {
        // Staff not found, proceed with creation
        log.debug("Staff not found by external ID, will create new staff");
      }
    }

    if (existingStaff != null && staffId != null) {
      // Staff exists - check for updates
      Map<String, Object> proposedData = buildUpdateRequest(staff, context);

      // Detect changes
      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("staff", existingStaff, proposedData);

      if (!changes.isEmpty()) {
        // Update staff
        log.info(
            "Updating staff: {} {} ({} fields changed)",
            staff.getFirstName(),
            staff.getLastName(),
            changes.size());
        apiClient.put("/api/v1/staff/" + staffId, proposedData, Map.class);
        result.recordEntity("staff", ImportResult.EntityAction.UPDATED);
      } else {
        log.debug("Staff unchanged: {}", staff.getExternalId());
        result.recordEntity("staff", ImportResult.EntityAction.UNCHANGED);
      }
    } else {
      // Create new staff
      Map<String, Object> request = buildRequest(staff, context);
      Map<String, Object> response = apiClient.post("/api/v1/staff", request, Map.class);
      staffId = ((Number) response.get("resourceId")).longValue();

      log.info("Staff created: {} {} (ID: {})", staff.getFirstName(), staff.getLastName(), staffId);
      result.recordEntity("staff", ImportResult.EntityAction.CREATED);
    }

    // Store for reference
    if (staff.getExternalId() != null) {
      context.registerEntity("staff", staff.getExternalId(), staffId);
    }
    // Also register by name for backwards compatibility
    String fullName = staff.getFirstName() + " " + staff.getLastName();
    context.registerEntity("staff", fullName, staffId);
  }

  /**
   * Builds API request for staff creation.
   *
   * @param staff staff member
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Staff staff, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (staff.getExternalId() != null) {
      request.put("externalId", staff.getExternalId());
    }

    request.put("firstname", staff.getFirstName());
    request.put("lastname", staff.getLastName());

    // Resolve office
    if (staff.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", staff.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        throw new IllegalStateException(
            "Office '" + staff.getOfficeName() + "' not found for staff");
      }
    }

    // Loan officer flag
    if (staff.getIsLoanOfficer() != null) {
      request.put("isLoanOfficer", staff.getIsLoanOfficer());
    }

    // Active flag
    if (staff.getIsActive() != null) {
      request.put("isActive", staff.getIsActive());
    }

    // Joining date
    if (staff.getJoiningDate() != null) {
      request.put("joiningDate", staff.getJoiningDate().format(DATE_FORMATTER));
    }

    // Contact info
    if (staff.getMobileNo() != null) {
      request.put("mobileNo", staff.getMobileNo());
    }

    if (staff.getEmailAddress() != null) {
      request.put("emailAddress", staff.getEmailAddress());
    }

    return request;
  }

  /**
   * Builds API request for staff update.
   *
   * <p>Staff can update most fields even when active, except externalId and joiningDate.
   *
   * @param staff staff member
   * @param context import context
   * @return request map for update
   */
  private Map<String, Object> buildUpdateRequest(Staff staff, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // External ID is immutable (not included)

    request.put("firstname", staff.getFirstName());
    request.put("lastname", staff.getLastName());

    // Resolve office
    if (staff.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", staff.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      }
    }

    // Loan officer flag
    if (staff.getIsLoanOfficer() != null) {
      request.put("isLoanOfficer", staff.getIsLoanOfficer());
    }

    // Active flag
    if (staff.getIsActive() != null) {
      request.put("isActive", staff.getIsActive());
    }

    // Joining date (may be immutable depending on Fineract version, but include it)
    if (staff.getJoiningDate() != null) {
      request.put("joiningDate", staff.getJoiningDate().format(DATE_FORMATTER));
      request.put("locale", LOCALE);
      request.put("dateFormat", DATE_FORMAT);
    }

    // Contact info
    if (staff.getMobileNo() != null) {
      request.put("mobileNo", staff.getMobileNo());
    }

    if (staff.getEmailAddress() != null) {
      request.put("emailAddress", staff.getEmailAddress());
    }

    return request;
  }
}
