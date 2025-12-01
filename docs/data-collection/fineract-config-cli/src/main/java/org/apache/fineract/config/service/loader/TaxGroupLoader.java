package org.apache.fineract.config.service.loader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.TaxComponent;
import org.apache.fineract.config.model.product.TaxGroup;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for tax group configuration.
 *
 * <p>Creates tax components and groups for tax calculations on products.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>POST /api/v1/taxes/component - Create tax component
 *   <li>POST /api/v1/taxes/group - Create tax group
 * </ul>
 */
@Slf4j
@Component
public class TaxGroupLoader {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final FineractApiClient apiClient;

  public TaxGroupLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads tax group configuration.
   *
   * @param taxGroups list of tax groups
   * @param context import context
   * @param result import result
   */
  public void load(List<TaxGroup> taxGroups, ImportContext context, ImportResult result) {
    log.info("Loading {} tax groups", taxGroups.size());

    // Fetch existing tax components and groups
    Map<String, Long> existingComponents = fetchExistingTaxComponents();
    Map<String, Long> existingGroups = fetchExistingTaxGroups();

    for (TaxGroup taxGroup : taxGroups) {
      try {
        loadSingleTaxGroup(taxGroup, existingComponents, existingGroups, context, result);
      } catch (Exception ex) {
        log.error("Failed to load tax group '{}': {}", taxGroup.getName(), ex.getMessage());
        result.recordEntity("taxGroup", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing tax components from Fineract.
   *
   * @return map of component name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingTaxComponents() {
    Map<String, Long> components = new HashMap<>();
    try {
      List<Map<String, Object>> componentList =
          apiClient.get("/api/v1/taxes/component", List.class);
      for (Map<String, Object> component : componentList) {
        String name = (String) component.get("name");
        Number id = (Number) component.get("id");
        if (name != null && id != null) {
          components.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing tax components: {}", ex.getMessage());
    }
    return components;
  }

  /**
   * Fetches existing tax groups from Fineract.
   *
   * @return map of group name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingTaxGroups() {
    Map<String, Long> groups = new HashMap<>();
    try {
      List<Map<String, Object>> groupList = apiClient.get("/api/v1/taxes/group", List.class);
      for (Map<String, Object> group : groupList) {
        String name = (String) group.get("name");
        Number id = (Number) group.get("id");
        if (name != null && id != null) {
          groups.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing tax groups: {}", ex.getMessage());
    }
    return groups;
  }

  /**
   * Loads a single tax group with its components.
   *
   * @param taxGroup tax group configuration
   * @param existingComponents existing tax components
   * @param existingGroups existing tax groups
   * @param context import context
   * @param result import result
   */
  private void loadSingleTaxGroup(
      TaxGroup taxGroup,
      Map<String, Long> existingComponents,
      Map<String, Long> existingGroups,
      ImportContext context,
      ImportResult result) {

    String name = taxGroup.getName();
    log.debug("Loading tax group: {}", name);

    // Check if tax group already exists
    if (existingGroups.containsKey(name)) {
      Long groupId = existingGroups.get(name);
      log.info("Tax group already exists: {} (ID: {})", name, groupId);
      result.recordEntity("taxGroup", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("taxGroup", name, groupId);
      return;
    }

    // First, create tax components if they don't exist
    List<Long> componentIds = new ArrayList<>();
    for (TaxComponent component : taxGroup.getTaxComponents()) {
      Long componentId = ensureTaxComponentExists(component, existingComponents, context, result);
      if (componentId != null) {
        componentIds.add(componentId);
      }
    }

    // Create tax group
    Map<String, Object> request = new HashMap<>();
    request.put("name", name);
    request.put("locale", "en");
    request.put("dateFormat", "yyyy-MM-dd");

    // Add tax component associations
    if (!componentIds.isEmpty()) {
      List<Map<String, Object>> taxComponentsList = new ArrayList<>();
      for (Long componentId : componentIds) {
        Map<String, Object> assoc = new HashMap<>();
        assoc.put("taxComponentId", componentId);
        assoc.put("startDate", LocalDate.now().format(DATE_FORMAT));
        taxComponentsList.add(assoc);
      }
      request.put("taxComponents", taxComponentsList);
    }

    Map<String, Object> response = apiClient.post("/api/v1/taxes/group", request, Map.class);
    Long groupId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Tax group created: {} (ID: {}) with {} components", name, groupId, componentIds.size());
    result.recordEntity("taxGroup", ImportResult.EntityAction.CREATED);
    context.registerEntity("taxGroup", name, groupId);
  }

  /**
   * Ensures a tax component exists, creating it if necessary.
   *
   * @param component tax component configuration
   * @param existingComponents existing components map
   * @param context import context
   * @param result import result
   * @return component ID
   */
  private Long ensureTaxComponentExists(
      TaxComponent component,
      Map<String, Long> existingComponents,
      ImportContext context,
      ImportResult result) {

    String name = component.getName();

    // Check if already exists
    if (existingComponents.containsKey(name)) {
      Long componentId = existingComponents.get(name);
      log.debug("Tax component already exists: {} (ID: {})", name, componentId);
      return componentId;
    }

    // Create component
    Map<String, Object> request = buildComponentRequest(component, context);

    try {
      Map<String, Object> response = apiClient.post("/api/v1/taxes/component", request, Map.class);
      Long componentId = ((Number) response.get("resourceId")).longValue();

      log.info("Tax component created: {} (ID: {})", name, componentId);
      result.recordEntity("taxComponent", ImportResult.EntityAction.CREATED);
      context.registerEntity("taxComponent", name, componentId);
      existingComponents.put(name, componentId);

      return componentId;
    } catch (Exception ex) {
      log.error("Failed to create tax component '{}': {}", name, ex.getMessage());
      result.recordEntity("taxComponent", ImportResult.EntityAction.FAILED);
      return null;
    }
  }

  /**
   * Builds the API request for creating a tax component.
   *
   * @param component tax component configuration
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildComponentRequest(TaxComponent component, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", component.getName());
    request.put("locale", "en");
    request.put("dateFormat", "yyyy-MM-dd");

    // Percentage - must be > 0, default to 10% if not specified
    if (component.getPercentage() != null
        && component.getPercentage().compareTo(BigDecimal.ZERO) > 0) {
      request.put("percentage", component.getPercentage());
    } else {
      request.put("percentage", 10); // Default to 10% (must be > 0)
    }

    // Start date - handle both string and array formats
    String startDateStr = formatDate(component.getStartDate());
    if (startDateStr == null) {
      startDateStr = LocalDate.now().format(DATE_FORMAT);
    }
    request.put("startDate", startDateStr);

    // Credit account
    if (component.getCreditGlAccount() != null) {
      Long glAccountId = resolveGlAccountId(component.getCreditGlAccount(), context);
      if (glAccountId != null) {
        request.put("creditAccountType", mapAccountType(component.getCreditAccountType()));
        request.put("creditAcountId", glAccountId);
      }
    }

    return request;
  }

  /**
   * Formats a date for the API.
   *
   * <p>Handles both string format ("2024-01-01") and array format [2024, 1, 1].
   *
   * @param date date object (String, List, or LocalDate)
   * @return formatted date string or null
   */
  @SuppressWarnings("unchecked")
  private String formatDate(Object date) {
    if (date == null) {
      return null;
    }

    if (date instanceof String) {
      return (String) date;
    }

    if (date instanceof LocalDate) {
      return ((LocalDate) date).format(DATE_FORMAT);
    }

    if (date instanceof List) {
      List<Integer> parts = (List<Integer>) date;
      if (parts.size() >= 3) {
        return String.format("%04d-%02d-%02d", parts.get(0), parts.get(1), parts.get(2));
      }
    }

    return date.toString();
  }

  /**
   * Resolves a GL account code to ID.
   *
   * @param glCode GL account code
   * @param context import context
   * @return GL account ID or null
   */
  @SuppressWarnings("unchecked")
  private Long resolveGlAccountId(String glCode, ImportContext context) {
    // Try from context
    Long id = context.getEntityId("glAccount", glCode);
    if (id != null) {
      return id;
    }

    // Lookup from API
    try {
      List<Map<String, Object>> accounts = apiClient.get("/api/v1/glaccounts", List.class);
      for (Map<String, Object> account : accounts) {
        if (glCode.equals(account.get("glCode"))) {
          Long accountId = ((Number) account.get("id")).longValue();
          context.registerEntity("glAccount", glCode, accountId);
          return accountId;
        }
      }
    } catch (Exception ex) {
      log.debug("Could not look up GL account '{}': {}", glCode, ex.getMessage());
    }

    return null;
  }

  /**
   * Maps account type string to Fineract account type ID.
   *
   * @param accountType account type string
   * @return account type ID
   */
  private Integer mapAccountType(String accountType) {
    if (accountType == null) {
      return 2; // Default to LIABILITY
    }
    return switch (accountType.toUpperCase().trim()) {
      case "ASSET" -> 1;
      case "LIABILITY" -> 2;
      case "EQUITY" -> 3;
      case "INCOME" -> 4;
      case "EXPENSE" -> 5;
      default -> 2;
    };
  }
}
