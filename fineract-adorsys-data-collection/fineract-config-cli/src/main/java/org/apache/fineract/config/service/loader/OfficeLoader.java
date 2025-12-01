package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.security.Office;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.InputValidator;
import org.apache.fineract.config.util.InputValidator.ValidationResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for offices (branches).
 *
 * <p>Creates organizational units (offices/branches) in hierarchical structure.
 *
 * <p>API Endpoint: POST /api/v1/offices
 */
@Slf4j
@Component
public class OfficeLoader {

  private final FineractApiClient apiClient;
  private final InputValidator inputValidator;
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");

  public OfficeLoader(FineractApiClient apiClient, InputValidator inputValidator) {
    this.apiClient = apiClient;
    this.inputValidator = inputValidator;
  }

  /**
   * Loads offices.
   *
   * @param offices list of offices
   * @param context import context
   * @param result import result
   * @param dryRun dry-run mode flag
   */
  public void load(
      List<Office> offices, ImportContext context, ImportResult result, boolean dryRun) {
    log.debug("Loading {} offices (dry-run: {})", offices.size(), dryRun);

    // Pre-register existing offices (especially Head Office) for parent resolution
    registerExistingOffices(context);

    // Load offices in hierarchical order (parents before children)
    for (Office office : offices) {
      try {
        loadSingleOffice(office, context, result, dryRun);
      } catch (Exception ex) {
        log.error("Failed to load office '{}': {}", office.getName(), ex.getMessage());
        if (dryRun) {
          PlannedChange change =
              PlannedChange.wouldFail("office", office.getName(), ex.getMessage());
          result.addPlannedChange(change);
        } else {
          result.recordEntity("office", ImportResult.EntityAction.FAILED);
        }
      }
    }
  }

  /**
   * Loads a single office.
   *
   * @param office office to load
   * @param context import context
   * @param result import result
   * @param dryRun dry-run mode flag
   */
  private void loadSingleOffice(
      Office office, ImportContext context, ImportResult result, boolean dryRun) {
    String identifier = office.getName();
    log.debug("Loading office: {} (dry-run: {})", identifier, dryRun);

    // Validate input data
    List<ValidationResult> validationResults = new ArrayList<>();
    validationResults.add(inputValidator.validateEntityIdentifier(office.getName(), "name"));
    validationResults.add(inputValidator.validateExternalId(office.getExternalId(), "externalId"));
    if (office.getParentName() != null) {
      validationResults.add(
          inputValidator.validateEntityIdentifier(office.getParentName(), "parentName"));
    }

    ValidationResult validation = inputValidator.validateAll(validationResults);
    if (!validation.isValid()) {
      String errors = validation.getErrorMessage();
      log.error("Validation failed for office '{}': {}", office.getName(), errors);
      throw new IllegalArgumentException("Invalid office data: " + errors);
    }

    // Check if office already exists
    List<Map<String, Object>> existingOffices = apiClient.get("/api/v1/offices", List.class);

    Map<String, Object> existingOffice =
        existingOffices.stream()
            .filter(o -> identifier.equals(o.get("name")))
            .findFirst()
            .orElse(null);

    if (existingOffice != null) {
      // Office exists - check for updates
      Long officeId = ((Number) existingOffice.get("id")).longValue();

      Map<String, Object> proposedData = buildRequest(office, context);
      Map<String, PlannedChange.FieldChange> changedFields =
          detectChanges(existingOffice, proposedData);

      if (dryRun) {
        // DRY-RUN MODE: Plan update or no-change
        if (!changedFields.isEmpty()) {
          log.info(
              "[DRY-RUN] Would update office: {} ({} fields changed)",
              identifier,
              changedFields.size());
          PlannedChange change =
              PlannedChange.updateChange(
                  "office", identifier, existingOffice, proposedData, changedFields);
          result.addPlannedChange(change);
        } else {
          log.debug("[DRY-RUN] Office {} would remain unchanged", identifier);
          result.addPlannedChange(PlannedChange.noChange("office", identifier));
        }
      } else {
        // REAL MODE: Update if changed
        if (!changedFields.isEmpty()) {
          apiClient.put("/api/v1/offices/" + officeId, proposedData, Map.class);
          log.info("Updated office: {} ({} fields changed)", identifier, changedFields.size());
          result.recordEntity("office", ImportResult.EntityAction.UPDATED);
        } else {
          log.debug("Office {} unchanged", identifier);
          result.recordEntity("office", ImportResult.EntityAction.UNCHANGED);
        }
      }

      // Register entity ID (needed in both modes for dependency resolution)
      context.registerEntity("office", identifier, officeId);

    } else {
      // Office does not exist - create
      if (dryRun) {
        // DRY-RUN MODE: Plan creation
        log.info("[DRY-RUN] Would create office: {}", identifier);
        Map<String, Object> proposedData = buildRequest(office, context);
        PlannedChange change = PlannedChange.createChange("office", identifier, proposedData);
        result.addPlannedChange(change);

        // Register with mock ID for dependency resolution in dry-run mode
        context.registerEntity("office", identifier, -1L);
      } else {
        // REAL MODE: Create office
        Map<String, Object> request = buildRequest(office, context);
        Map<String, Object> response = apiClient.post("/api/v1/offices", request, Map.class);
        Long officeId = ((Number) response.get("resourceId")).longValue();

        log.info("Created office: {} (ID: {})", identifier, officeId);
        result.recordEntity("office", ImportResult.EntityAction.CREATED);

        // Store for reference
        context.registerEntity("office", identifier, officeId);
      }
    }
  }

  /**
   * Builds API request for office.
   *
   * @param office office
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(Office office, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", office.getName());
    request.put("dateFormat", "dd MMMM yyyy");
    request.put("locale", "en");
    request.put("openingDate", office.getOpeningDate().format(DATE_FORMATTER));

    if (office.getExternalId() != null) {
      request.put("externalId", office.getExternalId());
    }

    // Resolve parent office if specified
    if (office.getParentName() != null) {
      Long parentId = context.resolveEntityId("office", office.getParentName());
      if (parentId != null) {
        request.put("parentId", parentId);
      } else {
        log.warn(
            "Parent office '{}' not found for office '{}'",
            office.getParentName(),
            office.getName());
      }
    }

    return request;
  }

  /**
   * Detects changes between existing and proposed office data.
   *
   * @param existing existing office data from API
   * @param proposed proposed office data from YAML
   * @return map of changed fields
   */
  private Map<String, PlannedChange.FieldChange> detectChanges(
      Map<String, Object> existing, Map<String, Object> proposed) {

    Map<String, PlannedChange.FieldChange> changes = new LinkedHashMap<>();

    // Fields to ignore (immutable or system-managed)
    Set<String> ignoredFields = Set.of("id", "hierarchy", "openingDate", "dateFormat", "locale");

    for (Map.Entry<String, Object> entry : proposed.entrySet()) {
      String field = entry.getKey();

      if (ignoredFields.contains(field)) {
        continue;
      }

      Object proposedValue = entry.getValue();
      Object existingValue = existing.get(field);

      if (!Objects.equals(proposedValue, existingValue)) {
        PlannedChange.FieldChange fieldChange =
            PlannedChange.FieldChange.builder()
                .fieldName(field)
                .oldValue(existingValue)
                .newValue(proposedValue)
                .build();
        changes.put(field, fieldChange);
      }
    }

    return changes;
  }

  /**
   * Pre-registers existing offices in the ImportContext to enable parent resolution.
   *
   * <p>This is crucial for resolving parentName references (e.g., "Head Office") when creating new
   * branch offices. Without this, parentId cannot be determined and office creation fails with 403
   * FORBIDDEN.
   *
   * @param context import context to register offices into
   */
  private void registerExistingOffices(ImportContext context) {
    try {
      List<Map<String, Object>> existingOffices = apiClient.get("/api/v1/offices", List.class);

      for (Map<String, Object> office : existingOffices) {
        String name = (String) office.get("name");
        Long id = ((Number) office.get("id")).longValue();

        context.registerEntity("office", name, id);
        log.debug("Pre-registered existing office: {} (ID: {})", name, id);
      }

      log.info(
          "Pre-registered {} existing office(s) for parent resolution", existingOffices.size());
    } catch (Exception ex) {
      log.warn("Failed to pre-register existing offices: {}", ex.getMessage());
      // Continue anyway - existing offices in YAML will be registered as they're processed
    }
  }
}
