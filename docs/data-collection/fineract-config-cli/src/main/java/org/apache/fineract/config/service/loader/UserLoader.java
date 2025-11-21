package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.PlannedChange;
import org.apache.fineract.config.model.security.User;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.ChangeDetectionService;
import org.apache.fineract.config.util.InputValidator;
import org.apache.fineract.config.util.InputValidator.ValidationResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for users.
 *
 * <p>Creates user accounts with role assignments.
 *
 * <p>API Endpoint: POST /api/v1/users
 */
@Slf4j
@Component
public class UserLoader {

  private final FineractApiClient apiClient;
  private final ChangeDetectionService changeDetectionService;
  private final InputValidator inputValidator;

  public UserLoader(
      FineractApiClient apiClient,
      ChangeDetectionService changeDetectionService,
      InputValidator inputValidator) {
    this.apiClient = apiClient;
    this.changeDetectionService = changeDetectionService;
    this.inputValidator = inputValidator;
  }

  /**
   * Loads users.
   *
   * @param users list of users
   * @param context import context
   * @param result import result
   */
  public void load(List<User> users, ImportContext context, ImportResult result) {
    log.debug("Loading {} users", users.size());

    for (User user : users) {
      try {
        loadSingleUser(user, context, result);
      } catch (Exception ex) {
        log.error("Failed to load user '{}': {}", user.getUsername(), ex.getMessage());
        result.recordEntity("user", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single user.
   *
   * @param user user to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleUser(User user, ImportContext context, ImportResult result) {
    log.debug("Loading user: {}", user.getUsername());

    // Validate input data
    List<ValidationResult> validationResults = new ArrayList<>();
    validationResults.add(inputValidator.validateEntityIdentifier(user.getUsername(), "username"));
    validationResults.add(inputValidator.validateName(user.getFirstname(), "firstName"));
    validationResults.add(inputValidator.validateName(user.getLastname(), "lastName"));
    validationResults.add(inputValidator.validateEmail(user.getEmail(), "email"));
    validationResults.add(
        inputValidator.validateEntityIdentifier(user.getOfficeName(), "officeName"));

    ValidationResult validation = inputValidator.validateAll(validationResults);
    if (!validation.isValid()) {
      String errors = validation.getErrorMessage();
      log.error("Validation failed for user '{}': {}", user.getUsername(), errors);
      throw new IllegalArgumentException("Invalid user data: " + errors);
    }

    // Check if user already exists
    List<Map<String, Object>> existingUsers = apiClient.get("/api/v1/users", List.class);

    Map<String, Object> existingUser =
        existingUsers.stream()
            .filter(u -> user.getUsername().equals(u.get("username")))
            .findFirst()
            .orElse(null);

    Long userId;

    if (existingUser != null) {
      // User exists - check for updates
      userId = ((Number) existingUser.get("id")).longValue();
      log.debug("User already exists: {} (ID: {})", user.getUsername(), userId);

      // Build proposed data (excluding password fields for update)
      Map<String, Object> proposedData = buildUpdateRequest(user, context);

      // Detect changes
      Map<String, PlannedChange.FieldChange> changes =
          changeDetectionService.detectChangesForEntityType("user", existingUser, proposedData);

      if (!changes.isEmpty()) {
        // Update user
        log.info("Updating user: {} ({} fields changed)", user.getUsername(), changes.size());
        apiClient.put("/api/v1/users/" + userId, proposedData, Map.class);
        result.recordEntity("user", ImportResult.EntityAction.UPDATED);
      } else {
        log.debug("User unchanged: {}", user.getUsername());
        result.recordEntity("user", ImportResult.EntityAction.UNCHANGED);
      }
    } else {
      // Create new user
      Map<String, Object> request = buildRequest(user, context);
      Map<String, Object> response = apiClient.post("/api/v1/users", request, Map.class);
      userId = ((Number) response.get("resourceId")).longValue();

      log.info("User created: {} (ID: {})", user.getUsername(), userId);
      result.recordEntity("user", ImportResult.EntityAction.CREATED);
    }

    // Store for reference
    context.registerEntity("user", user.getUsername(), userId);
  }

  /**
   * Builds API request for user creation.
   *
   * @param user user
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(User user, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("username", user.getUsername());
    request.put("firstname", user.getFirstname());
    request.put("lastname", user.getLastname());
    request.put("email", user.getEmail());
    request.put("password", user.getPassword());
    request.put("repeatPassword", user.getPassword());

    if (user.getPasswordNeverExpires() != null) {
      request.put("passwordNeverExpires", user.getPasswordNeverExpires());
    }

    if (user.getIsSelfServiceUser() != null) {
      request.put("isSelfServiceUser", user.getIsSelfServiceUser());
    }

    // Resolve office
    if (user.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", user.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        log.warn("Office '{}' not found for user '{}'", user.getOfficeName(), user.getUsername());
      }
    }

    // Resolve roles
    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
      List<Long> roleIds = new ArrayList<>();
      for (String roleName : user.getRoles()) {
        Long roleId = context.resolveEntityId("role", roleName);
        if (roleId != null) {
          roleIds.add(roleId);
        } else {
          log.warn("Role '{}' not found for user '{}'", roleName, user.getUsername());
        }
      }
      request.put("roles", roleIds);
    }

    // TODO: Add staff assignment support when staff loader is implemented

    request.put("sendPasswordToEmail", false);

    return request;
  }

  /**
   * Builds API request for user update.
   *
   * <p>Note: Password updates are excluded - they require a separate endpoint for security.
   *
   * @param user user
   * @param context import context
   * @return request map for update
   */
  private Map<String, Object> buildUpdateRequest(User user, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // Username is immutable - not included
    request.put("firstname", user.getFirstname());
    request.put("lastname", user.getLastname());
    request.put("email", user.getEmail());

    if (user.getPasswordNeverExpires() != null) {
      request.put("passwordNeverExpires", user.getPasswordNeverExpires());
    }

    if (user.getIsSelfServiceUser() != null) {
      request.put("isSelfServiceUser", user.getIsSelfServiceUser());
    }

    // Resolve office
    if (user.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", user.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      } else {
        log.warn("Office '{}' not found for user '{}'", user.getOfficeName(), user.getUsername());
      }
    }

    // Resolve roles
    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
      List<Long> roleIds = new ArrayList<>();
      for (String roleName : user.getRoles()) {
        Long roleId = context.resolveEntityId("role", roleName);
        if (roleId != null) {
          roleIds.add(roleId);
        } else {
          log.warn("Role '{}' not found for user '{}'", roleName, user.getUsername());
        }
      }
      request.put("roles", roleIds);
    }

    // TODO: Add staff assignment support when staff loader is implemented

    return request;
  }
}
