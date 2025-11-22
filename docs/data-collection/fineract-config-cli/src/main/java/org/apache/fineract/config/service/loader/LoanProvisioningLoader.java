package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.LoanProvisioning;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan provisioning criteria.
 *
 * <p>Creates loan provisioning criteria with categories for portfolio risk management.
 *
 * <p>API Endpoint: POST /api/v1/provisioningcriteria
 */
@Slf4j
@Component
public class LoanProvisioningLoader {

  private final FineractApiClient apiClient;

  public LoanProvisioningLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan provisioning criteria.
   *
   * @param provisioningCategories list of provisioning categories
   * @param context import context
   * @param result import result
   */
  public void load(
      List<LoanProvisioning> provisioningCategories, ImportContext context, ImportResult result) {
    log.info("Loading {} loan provisioning categories", provisioningCategories.size());

    if (provisioningCategories.isEmpty()) {
      return;
    }

    // Check if provisioning criteria already exists
    Map<String, Long> existingCriteria = fetchExistingCriteria();

    // Create a single provisioning criteria with all categories
    String criteriaName = "Default Provisioning Criteria";

    if (existingCriteria.containsKey(criteriaName)) {
      Long criteriaId = existingCriteria.get(criteriaName);
      log.info("Provisioning criteria already exists: {} (ID: {})", criteriaName, criteriaId);
      result.recordEntity("loanProvisioning", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("loanProvisioning", criteriaName, criteriaId);
      return;
    }

    // Build the request with all categories
    Map<String, Object> request = buildRequest(criteriaName, provisioningCategories, context);

    try {
      Map<String, Object> response =
          apiClient.post("/api/v1/provisioningcriteria", request, Map.class);
      Long criteriaId = ((Number) response.get("resourceId")).longValue();

      log.info(
          "Provisioning criteria created: {} (ID: {}) with {} categories",
          criteriaName,
          criteriaId,
          provisioningCategories.size());
      result.recordEntity("loanProvisioning", ImportResult.EntityAction.CREATED);
      context.registerEntity("loanProvisioning", criteriaName, criteriaId);

    } catch (Exception ex) {
      log.error("Failed to create provisioning criteria: {}", ex.getMessage());
      result.recordEntity("loanProvisioning", ImportResult.EntityAction.FAILED);
    }
  }

  /**
   * Fetches existing provisioning criteria.
   *
   * @return map of criteria name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingCriteria() {
    Map<String, Long> criteria = new HashMap<>();
    try {
      List<Map<String, Object>> criteriaList =
          apiClient.get("/api/v1/provisioningcriteria", List.class);
      for (Map<String, Object> c : criteriaList) {
        String name = (String) c.get("criteriaName");
        // API returns "criteriaId" not "id"
        Number id = (Number) c.get("criteriaId");
        if (id == null) {
          id = (Number) c.get("id"); // Fallback
        }
        if (name != null && id != null) {
          criteria.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing provisioning criteria: {}", ex.getMessage());
    }
    return criteria;
  }

  /**
   * Builds the API request for creating provisioning criteria.
   *
   * @param criteriaName criteria name
   * @param categories provisioning categories
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(
      String criteriaName, List<LoanProvisioning> categories, ImportContext context) {

    Map<String, Object> request = new HashMap<>();
    request.put("criteriaName", criteriaName);
    request.put("locale", "en");

    // Build definitions array
    List<Map<String, Object>> definitions = new ArrayList<>();

    for (LoanProvisioning category : categories) {
      Map<String, Object> def = new HashMap<>();

      def.put("categoryId", mapCategoryNameToId(category.getCategoryName()));
      def.put("categoryName", category.getCategoryName());

      // minAge and maxAge define the days overdue range for this category
      // Ranges must NOT overlap, so we use category-based defaults
      if (category.getMinDaysOverdue() != null) {
        def.put("minAge", category.getMinDaysOverdue());
      } else {
        // Default minAge based on category (non-overlapping ranges)
        def.put("minAge", getDefaultMinAge(category.getCategoryName()));
      }

      // maxAge is mandatory - use default ranges if not specified
      if (category.getMaxDaysOverdue() != null) {
        def.put("maxAge", category.getMaxDaysOverdue());
      } else {
        // Default maxAge based on category (typical provisioning ranges)
        def.put("maxAge", getDefaultMaxAge(category.getCategoryName()));
      }

      if (category.getProvisioningPercentage() != null) {
        def.put("provisioningPercentage", category.getProvisioningPercentage());
      } else {
        def.put("provisioningPercentage", 0);
      }

      // Resolve GL account IDs - YAML has glCode, need to resolve to actual ID
      Long liabilityId = null;
      if (category.getLiabilityAccountId() != null) {
        // Try to resolve as glCode first
        liabilityId =
            resolveGlAccountByCode(String.valueOf(category.getLiabilityAccountId()), context);
      }
      if (liabilityId == null) {
        // Fallback to name lookup
        liabilityId = resolveGlAccount("Provision for Loan Losses", context);
      }
      if (liabilityId != null) {
        def.put("liabilityAccount", liabilityId);
      }

      Long expenseId = null;
      if (category.getExpenseAccountId() != null) {
        // Try to resolve as glCode first
        expenseId = resolveGlAccountByCode(String.valueOf(category.getExpenseAccountId()), context);
      }
      if (expenseId == null) {
        // Fallback to name lookup
        expenseId = resolveGlAccount("Loan Loss Provision Expense", context);
      }
      if (expenseId != null) {
        def.put("expenseAccount", expenseId);
      }

      definitions.add(def);
    }

    request.put("definitions", definitions);

    // Add all loan products (required by API)
    // Format: [{"id": 1}, {"id": 2}, ...] - NOT just [1, 2, ...]
    List<Map<String, Object>> loanProducts = fetchLoanProducts();
    if (!loanProducts.isEmpty()) {
      request.put("loanProducts", loanProducts);
    }

    return request;
  }

  /**
   * Fetches all loan products as objects with id field.
   *
   * @return list of loan product objects with id field
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> fetchLoanProducts() {
    List<Map<String, Object>> products = new ArrayList<>();
    try {
      List<Map<String, Object>> productList = apiClient.get("/api/v1/loanproducts", List.class);
      for (Map<String, Object> product : productList) {
        Number id = (Number) product.get("id");
        if (id != null) {
          // API expects objects with "id" field, not just the ID value
          Map<String, Object> productRef = new HashMap<>();
          productRef.put("id", id.longValue());
          products.add(productRef);
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch loan products: {}", ex.getMessage());
    }
    return products;
  }

  /**
   * Resolves a GL account by code.
   *
   * @param glCode GL account code
   * @param context import context
   * @return account ID or null
   */
  @SuppressWarnings("unchecked")
  private Long resolveGlAccountByCode(String glCode, ImportContext context) {
    // Try from context by glCode
    Long id = context.getEntityId("glAccount", glCode);
    if (id != null) {
      return id;
    }

    // Fetch from API
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
      log.debug("Could not look up GL account by code '{}': {}", glCode, ex.getMessage());
    }

    return null;
  }

  /**
   * Maps category name to Fineract category ID.
   *
   * @param categoryName category name
   * @return category ID
   */
  private Integer mapCategoryNameToId(String categoryName) {
    if (categoryName == null) {
      return 1; // Default to STANDARD
    }

    return switch (categoryName.toUpperCase().trim()) {
      case "STANDARD", "PERFORMING" -> 1;
      case "SUB-STANDARD", "SUBSTANDARD", "SUB_STANDARD" -> 2;
      case "DOUBTFUL" -> 3;
      case "LOSS" -> 4;
      default -> 1;
    };
  }

  /**
   * Gets default minAge (days overdue) based on category name.
   *
   * @param categoryName category name
   * @return default minAge value
   */
  private Integer getDefaultMinAge(String categoryName) {
    if (categoryName == null) {
      return 0;
    }

    return switch (categoryName.toUpperCase().trim()) {
      case "STANDARD", "PERFORMING" -> 0; // 0-30 days
      case "SUB-STANDARD", "SUBSTANDARD", "SUB_STANDARD" -> 31; // 31-90 days
      case "DOUBTFUL" -> 91; // 91-180 days
      case "LOSS" -> 181; // 181+ days
      default -> 0;
    };
  }

  /**
   * Gets default maxAge (days overdue) based on category name.
   *
   * @param categoryName category name
   * @return default maxAge value
   */
  private Integer getDefaultMaxAge(String categoryName) {
    if (categoryName == null) {
      return 30;
    }

    return switch (categoryName.toUpperCase().trim()) {
      case "STANDARD", "PERFORMING" -> 30; // 0-30 days
      case "SUB-STANDARD", "SUBSTANDARD", "SUB_STANDARD" -> 90; // 31-90 days
      case "DOUBTFUL" -> 180; // 91-180 days
      case "LOSS" -> 365; // 181+ days
      default -> 30;
    };
  }

  /**
   * Resolves a GL account by name.
   *
   * @param accountName account name
   * @param context import context
   * @return account ID or null
   */
  @SuppressWarnings("unchecked")
  private Long resolveGlAccount(String accountName, ImportContext context) {
    // Try from context
    Long id = context.getEntityId("glAccount", accountName);
    if (id != null) {
      return id;
    }

    // Fetch from API
    try {
      List<Map<String, Object>> accounts = apiClient.get("/api/v1/glaccounts", List.class);
      for (Map<String, Object> account : accounts) {
        if (accountName.equals(account.get("name"))) {
          Long accountId = ((Number) account.get("id")).longValue();
          context.registerEntity("glAccount", accountName, accountId);
          return accountId;
        }
      }
    } catch (Exception ex) {
      log.debug("Could not look up GL account '{}': {}", accountName, ex.getMessage());
    }

    return null;
  }
}
