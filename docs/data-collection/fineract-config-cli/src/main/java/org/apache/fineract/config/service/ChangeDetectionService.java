package org.apache.fineract.config.service;

import java.util.*;

import org.apache.fineract.config.model.PlannedChange;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for detecting changes between existing and proposed entity data.
 *
 * <p>Provides intelligent field-by-field comparison with support for:
 *
 * <ul>
 *   <li>Configurable ignored fields (immutable or system-managed)
 *   <li>Type-aware comparison (numbers, dates, booleans, strings)
 *   <li>Null-safe comparison
 *   <li>Deep comparison for nested objects and lists
 *   <li>Entity-specific configurations
 * </ul>
 */
@Slf4j
@Service
public class ChangeDetectionService {

  /** Immutable fields configuration per entity type */
  private final Map<String, Set<String>> immutableFieldsByEntityType;

  public ChangeDetectionService() {
    this.immutableFieldsByEntityType = initializeImmutableFields();
  }

  /**
   * Detects changes between existing and proposed entity data.
   *
   * @param existing current data from Fineract API
   * @param proposed new data from YAML configuration
   * @param ignoredFields fields to skip (immutable or system-managed)
   * @return map of changed fields with old/new values
   */
  public Map<String, PlannedChange.FieldChange> detectChanges(
      Map<String, Object> existing, Map<String, Object> proposed, Set<String> ignoredFields) {

    Map<String, PlannedChange.FieldChange> changes = new LinkedHashMap<>();

    if (existing == null || proposed == null) {
      return changes;
    }

    for (Map.Entry<String, Object> entry : proposed.entrySet()) {
      String field = entry.getKey();

      // Skip ignored fields
      if (ignoredFields.contains(field)) {
        continue;
      }

      Object proposedValue = entry.getValue();
      Object existingValue = existing.get(field);

      // Compare values
      if (!valuesAreEqual(existingValue, proposedValue)) {
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
   * Detects changes using entity-specific immutable fields configuration.
   *
   * @param entityType entity type (e.g., "office", "client", "loanProduct")
   * @param existing current data from API
   * @param proposed new data from YAML
   * @return map of changed fields
   */
  public Map<String, PlannedChange.FieldChange> detectChangesForEntityType(
      String entityType, Map<String, Object> existing, Map<String, Object> proposed) {

    Set<String> ignoredFields = getImmutableFieldsForEntityType(entityType);
    return detectChanges(existing, proposed, ignoredFields);
  }

  /**
   * Compares two values with type awareness and null safety.
   *
   * @param existing existing value
   * @param proposed proposed value
   * @return true if values are equal, false otherwise
   */
  private boolean valuesAreEqual(Object existing, Object proposed) {
    // Both null
    if (existing == null && proposed == null) {
      return true;
    }

    // One null, one not
    if (existing == null || proposed == null) {
      return false;
    }

    // Lists - compare ignoring order for some cases, preserving order for others
    if (existing instanceof List && proposed instanceof List) {
      return listsAreEqual((List<?>) existing, (List<?>) proposed);
    }

    // Maps - recursive comparison
    if (existing instanceof Map && proposed instanceof Map) {
      return mapsAreEqual((Map<?, ?>) existing, (Map<?, ?>) proposed);
    }

    // Numbers - handle different numeric types (Integer vs Long, etc.)
    if (existing instanceof Number && proposed instanceof Number) {
      return numbersAreEqual((Number) existing, (Number) proposed);
    }

    // Default: use Objects.equals
    return Objects.equals(existing, proposed);
  }

  /**
   * Compares two lists for equality (preserves order).
   *
   * @param existing existing list
   * @param proposed proposed list
   * @return true if lists are equal
   */
  private boolean listsAreEqual(List<?> existing, List<?> proposed) {
    if (existing.size() != proposed.size()) {
      return false;
    }

    for (int i = 0; i < existing.size(); i++) {
      if (!valuesAreEqual(existing.get(i), proposed.get(i))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Compares two maps for equality (recursive).
   *
   * @param existing existing map
   * @param proposed proposed map
   * @return true if maps are equal
   */
  private boolean mapsAreEqual(Map<?, ?> existing, Map<?, ?> proposed) {
    if (existing.size() != proposed.size()) {
      return false;
    }

    for (Map.Entry<?, ?> entry : proposed.entrySet()) {
      Object key = entry.getKey();
      if (!existing.containsKey(key)) {
        return false;
      }

      if (!valuesAreEqual(existing.get(key), entry.getValue())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Compares two numbers for equality across different numeric types.
   *
   * <p>Uses context-aware comparison logic:
   *
   * <ul>
   *   <li>Integers/Longs: Exact equality (no tolerance)
   *   <li>Floating point: Relative tolerance based on magnitude
   *   <li>Small values (< 1.0): Absolute tolerance of 1e-6
   *   <li>Large values: Relative tolerance of 1e-9 (0.0000001%)
   * </ul>
   *
   * @param existing existing number
   * @param proposed proposed number
   * @return true if numbers are equal within appropriate tolerance
   */
  private boolean numbersAreEqual(Number existing, Number proposed) {
    // Handle null cases
    if (existing == null && proposed == null) {
      return true;
    }
    if (existing == null || proposed == null) {
      return false;
    }

    // If both are integer types (Integer, Long, Short, Byte), use exact comparison
    if (isIntegerType(existing) && isIntegerType(proposed)) {
      return existing.longValue() == proposed.longValue();
    }

    // For floating point numbers, use relative tolerance
    double existingValue = existing.doubleValue();
    double proposedValue = proposed.doubleValue();

    // Handle exact zero case
    if (existingValue == 0.0 && proposedValue == 0.0) {
      return true;
    }

    // Calculate absolute difference
    double diff = Math.abs(existingValue - proposedValue);

    // For very small values (< 1.0), use absolute tolerance
    if (Math.abs(existingValue) < 1.0 || Math.abs(proposedValue) < 1.0) {
      return diff < 1e-6; // Absolute tolerance for small values
    }

    // For larger values, use relative tolerance (0.0000001% = 1e-9)
    double maxValue = Math.max(Math.abs(existingValue), Math.abs(proposedValue));
    double relativeTolerance = 1e-9;
    return diff <= maxValue * relativeTolerance;
  }

  /**
   * Checks if a number is an integer type.
   *
   * @param number number to check
   * @return true if integer type (Integer, Long, Short, Byte)
   */
  private boolean isIntegerType(Number number) {
    return number instanceof Integer
        || number instanceof Long
        || number instanceof Short
        || number instanceof Byte;
  }

  /**
   * Gets immutable fields for an entity type.
   *
   * @param entityType entity type
   * @return set of immutable field names
   */
  public Set<String> getImmutableFieldsForEntityType(String entityType) {
    return immutableFieldsByEntityType.getOrDefault(
        entityType.toLowerCase(), getDefaultImmutableFields());
  }

  /**
   * Default immutable fields common to most entities.
   *
   * @return set of default immutable fields
   */
  private Set<String> getDefaultImmutableFields() {
    return Set.of("id", "dateFormat", "locale");
  }

  /**
   * Initializes immutable fields configuration for all entity types.
   *
   * @return map of entity type to immutable fields
   */
  private Map<String, Set<String>> initializeImmutableFields() {
    Map<String, Set<String>> config = new HashMap<>();

    // Phase 2: Security & Organization
    config.put("office", Set.of("id", "hierarchy", "openingDate", "dateFormat", "locale"));

    config.put("role", Set.of("id"));

    config.put("user", Set.of("id", "username")); // Username is identifier, can't change

    config.put(
        "staff",
        Set.of("id", "externalId")); // External ID is identifier, joining date may be immutable

    // Phase 3: Accounting Foundation
    config.put(
        "glaccount", Set.of("id", "glCode", "type", "classification", "dateFormat", "locale"));

    config.put("paymenttype", Set.of("id"));

    config.put("fundsource", Set.of("id"));

    config.put("financialactivitymapping", Set.of("id"));

    // Phase 4: Financial Products
    config.put("charge", Set.of("id", "currencyCode")); // Currency can't change after creation

    config.put(
        "loanproduct",
        Set.of(
            "id",
            "shortName", // Used for lookup, can't change
            "currencyCode", // Currency can't change
            "dateFormat",
            "locale"));

    config.put(
        "savingsproduct",
        Set.of(
            "id",
            "shortName", // Used for lookup
            "currencyCode", // Currency can't change
            "dateFormat",
            "locale"));

    config.put("code", Set.of("id"));

    // Phase 5: Client Operations & Accounts
    config.put(
        "client",
        Set.of(
            "id",
            "externalId", // Used for lookup, can't change
            "accountNo",
            "activationDate", // Can't change after activation
            "dateFormat",
            "locale"));

    config.put(
        "group",
        Set.of(
            "id",
            "externalId", // Used for lookup
            "accountNo",
            "activationDate",
            "dateFormat",
            "locale"));

    config.put(
        "center",
        Set.of(
            "id",
            "externalId", // Used for lookup
            "accountNo",
            "activationDate",
            "dateFormat",
            "locale"));

    config.put(
        "savingsaccount",
        Set.of(
            "id",
            "externalId", // Used for lookup
            "accountNo",
            "productId", // Can't change product after creation
            "clientId", // Can't change client
            "groupId", // Can't change group
            "submittedOnDate",
            "approvedOnDate",
            "activationDate",
            "dateFormat",
            "locale"));

    config.put(
        "loanaccount",
        Set.of(
            "id",
            "externalId", // Used for lookup
            "accountNo",
            "productId", // Can't change product
            "clientId", // Can't change client
            "groupId", // Can't change group
            "principal", // Can't change after disbursement
            "submittedOnDate",
            "approvedOnDate",
            "disbursementDate",
            "expectedDisbursementDate",
            "dateFormat",
            "locale"));

    // System singletons
    config.put("currency", Set.of()); // No immutable fields (it's a system config update)

    config.put("workingdays", Set.of()); // No immutable fields (it's a system config update)

    config.put("globalconfig", Set.of("id", "name")); // Name is the key

    config.put("accountnumberpreference", Set.of("id", "accountType")); // Account type is the key

    config.put("notificationtemplate", Set.of("id"));

    return config;
  }

  /**
   * Checks if an entity can be updated based on its status.
   *
   * @param entityType entity type
   * @param existing existing entity data
   * @return true if entity can be updated, false if status prevents updates
   */
  public boolean canUpdate(String entityType, Map<String, Object> existing) {
    if (existing == null) {
      return false;
    }

    // Check status field for activation-restricted entities
    Object statusObj = existing.get("status");
    if (statusObj == null) {
      return true; // No status field, assume can update
    }

    String status = statusObj.toString().toLowerCase();

    switch (entityType.toLowerCase()) {
      case "loanaccount":
        // Can only update if not disbursed (pending or approved status)
        return !status.contains("active") && !status.contains("disbursed");

      case "savingsaccount":
        // Can only update if not activated
        return !status.contains("active");

      case "client":
      case "group":
      case "center":
        // Limited updates allowed even after activation
        // But full updates only allowed before activation
        return true; // Will use restricted field list if active

      default:
        return true; // No status restrictions
    }
  }

  /**
   * Gets restricted fields that CAN be updated even after activation.
   *
   * @param entityType entity type
   * @return set of updateable fields for activated entities
   */
  public Set<String> getUpdatableFieldsWhenActive(String entityType) {
    Map<String, Set<String>> updatableFields = new HashMap<>();

    updatableFields.put(
        "client", Set.of("staffId", "savingsProductId", "genderId", "clientTypeId"));

    updatableFields.put("group", Set.of("staffId", "externalId"));

    updatableFields.put("center", Set.of("staffId", "externalId"));

    return updatableFields.getOrDefault(entityType.toLowerCase(), Set.of());
  }
}
