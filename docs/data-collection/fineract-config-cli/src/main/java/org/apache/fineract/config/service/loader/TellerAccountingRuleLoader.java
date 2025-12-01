package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.accounting.TellerAccountingRule;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for teller accounting rules.
 *
 * <p>Creates accounting rules for teller cash transactions.
 *
 * <p>API Endpoint: POST /api/v1/accountingrules
 */
@Slf4j
@Component
public class TellerAccountingRuleLoader {

  private final FineractApiClient apiClient;

  public TellerAccountingRuleLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads teller accounting rules.
   *
   * @param rules list of teller accounting rules
   * @param context import context
   * @param result import result
   */
  public void load(List<TellerAccountingRule> rules, ImportContext context, ImportResult result) {
    log.debug("Loading {} teller accounting rules", rules.size());

    for (TellerAccountingRule rule : rules) {
      try {
        loadSingleRule(rule, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load teller accounting rule for '{}': {}",
            rule.getTellerName(),
            ex.getMessage());
        result.recordEntity("tellerAccountingRule", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single teller accounting rule.
   *
   * @param rule rule to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleRule(
      TellerAccountingRule rule, ImportContext context, ImportResult result) {
    log.debug("Loading teller accounting rule for: {}", rule.getTellerName());

    // Resolve teller ID (optional - for naming)
    Long tellerId = context.resolveEntityId("teller", rule.getTellerName());
    if (tellerId == null) {
      log.warn(
          "Teller '{}' not found, creating rule without teller reference", rule.getTellerName());
    }

    // Build and post the accounting rule
    Map<String, Object> request = buildRequest(rule, context);

    // Check if rule already exists
    String ruleName = "Teller Rule - " + rule.getTellerName();
    Long existingRuleId = findExistingRule(ruleName);

    if (existingRuleId != null) {
      log.info("Teller accounting rule already exists: {} (ID: {})", ruleName, existingRuleId);
      result.recordEntity("tellerAccountingRule", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Create new rule
    Map<String, Object> response = apiClient.post("/api/v1/accountingrules", request, Map.class);
    Long ruleId = ((Number) response.get("resourceId")).longValue();

    log.info("Teller accounting rule created for '{}' (ID: {})", rule.getTellerName(), ruleId);
    result.recordEntity("tellerAccountingRule", ImportResult.EntityAction.CREATED);
  }

  /**
   * Finds an existing accounting rule by name.
   *
   * @param ruleName rule name
   * @return rule ID or null
   */
  @SuppressWarnings("unchecked")
  private Long findExistingRule(String ruleName) {
    try {
      List<Map<String, Object>> rules = apiClient.get("/api/v1/accountingrules", List.class);
      for (Map<String, Object> rule : rules) {
        if (ruleName.equals(rule.get("name"))) {
          return ((Number) rule.get("id")).longValue();
        }
      }
    } catch (Exception ex) {
      log.debug("Could not check for existing rule '{}': {}", ruleName, ex.getMessage());
    }
    return null;
  }

  /**
   * Builds API request for teller accounting rule.
   *
   * @param rule rule
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(TellerAccountingRule rule, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // Rule name based on teller
    request.put("name", "Teller Rule - " + rule.getTellerName());

    if (rule.getDescription() != null) {
      request.put("description", rule.getDescription());
    } else {
      request.put("description", "Accounting rule for teller: " + rule.getTellerName());
    }

    // Resolve office
    if (rule.getOfficeName() != null) {
      Long officeId = context.resolveEntityId("office", rule.getOfficeName());
      if (officeId != null) {
        request.put("officeId", officeId);
      }
    } else {
      // Default to head office
      Long headOfficeId = context.resolveEntityId("office", "Head Office");
      if (headOfficeId != null) {
        request.put("officeId", headOfficeId);
      }
    }

    // Resolve cash in GL account (debit) - Fineract API expects 'accountToDebit'
    if (rule.getCashInGlCode() != null) {
      Long cashInId = resolveGlAccountByCode(rule.getCashInGlCode(), context);
      if (cashInId != null) {
        request.put("accountToDebit", cashInId);
      } else {
        log.warn(
            "GL account with code '{}' not found for teller '{}'",
            rule.getCashInGlCode(),
            rule.getTellerName());
      }
    }

    // Resolve cash out GL account (credit) - Fineract API expects 'accountToCredit'
    if (rule.getCashOutGlCode() != null) {
      Long cashOutId = resolveGlAccountByCode(rule.getCashOutGlCode(), context);
      if (cashOutId != null) {
        request.put("accountToCredit", cashOutId);
      } else {
        log.warn(
            "GL account with code '{}' not found for teller '{}'",
            rule.getCashOutGlCode(),
            rule.getTellerName());
      }
    }

    return request;
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
    // Try from context first
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
}
