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

    // Account type (CLIENT, LOAN, SAVINGS, etc.)
    request.put("accountType", preference.getAccountType());

    // Prefix type (1=None, 2=Office, 3=Client, etc.)
    if (preference.getPrefixType() != null) {
      request.put("prefixType", preference.getPrefixType());
    }

    return request;
  }
}
