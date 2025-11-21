package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.AccountNumberPreference;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for account number preferences.
 *
 * <p>Configures account numbering schemes for different entity types.
 *
 * <p>API Endpoints: - POST /api/v1/accountnumberformats - PUT /api/v1/accountnumberformats/{id}
 */
@Slf4j
@Component
public class AccountNumberPreferenceLoader {

  private final FineractApiClient apiClient;

  public AccountNumberPreferenceLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads account number preferences.
   *
   * @param preferences list of account number preferences
   * @param context import context
   * @param result import result
   */
  public void load(
      List<AccountNumberPreference> preferences, ImportContext context, ImportResult result) {
    log.debug("Loading {} account number preferences", preferences.size());

    for (AccountNumberPreference preference : preferences) {
      try {
        loadSinglePreference(preference, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load account number preference for '{}': {}",
            preference.getAccountType(),
            ex.getMessage());
        result.recordEntity("accountNumberPreference", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single account number preference.
   *
   * @param preference preference to load
   * @param context import context
   * @param result import result
   */
  private void loadSinglePreference(
      AccountNumberPreference preference, ImportContext context, ImportResult result) {
    log.debug("Loading account number preference for: {}", preference.getAccountType());

    // Get existing preferences
    List<Map<String, Object>> existingPreferences =
        apiClient.get("/api/v1/accountnumberformats", List.class);

    // Find preference for this account type
    Map<String, Object> existingPreference =
        existingPreferences.stream()
            .filter(p -> preference.getAccountType().equals(p.get("accountType")))
            .findFirst()
            .orElse(null);

    Map<String, Object> request = buildRequest(preference);

    if (existingPreference == null) {
      // Create new preference
      Map<String, Object> response =
          apiClient.post("/api/v1/accountnumberformats", request, Map.class);

      log.info(
          "Account number preference created: {} ({})",
          preference.getAccountType(),
          preference.getPrefixType());
      result.recordEntity("accountNumberPreference", ImportResult.EntityAction.CREATED);
    } else {
      // Update existing preference
      Long preferenceId = ((Number) existingPreference.get("id")).longValue();
      apiClient.put("/api/v1/accountnumberformats/" + preferenceId, request, Map.class);

      log.info(
          "Account number preference updated: {} ({})",
          preference.getAccountType(),
          preference.getPrefixType());
      result.recordEntity("accountNumberPreference", ImportResult.EntityAction.UPDATED);
    }
  }

  /**
   * Builds API request for account number preference.
   *
   * @param preference preference
   * @return request map
   */
  private Map<String, Object> buildRequest(AccountNumberPreference preference) {
    Map<String, Object> request = new HashMap<>();

    // Account type must be integer: 1=CLIENT, 2=LOAN, 3=SAVINGS, 4=GROUPS, 5=CENTERS
    request.put("accountType", convertAccountTypeToId(preference.getAccountType()));

    // Prefix type (1=None, 101=Office, 102=Client, etc.) - already Integer in model
    if (preference.getPrefixType() != null) {
      request.put("prefixType", preference.getPrefixType());
    }

    return request;
  }

  /**
   * Converts account type string to Fineract account type ID.
   *
   * @param accountType account type string (CLIENT, LOAN, SAVINGS, etc.)
   * @return account type ID
   */
  private Integer convertAccountTypeToId(String accountType) {
    if (accountType == null) {
      return null;
    }
    return switch (accountType.toUpperCase()) {
      case "CLIENT" -> 1;
      case "LOAN" -> 2;
      case "SAVINGS" -> 3;
      case "GROUPS", "GROUP" -> 4;
      case "CENTERS", "CENTER" -> 5;
      default -> throw new IllegalArgumentException("Unknown account type: " + accountType);
    };
  }
}
