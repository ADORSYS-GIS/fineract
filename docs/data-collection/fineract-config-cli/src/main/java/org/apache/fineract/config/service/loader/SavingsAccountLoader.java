package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.account.SavingsAccount;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for savings accounts.
 *
 * <p>Creates savings account records for clients or groups.
 *
 * <p>IMPORTANT: Savings accounts have very limited update support. Most fields are immutable after
 * creation (productId, clientId, groupId, submittedOnDate, etc.). Updates are generally NOT
 * recommended. This loader detects existing accounts and skips them to prevent errors.
 *
 * <p>API Endpoint: POST /api/v1/savingsaccounts
 */
@Slf4j
@Component
public class SavingsAccountLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;

  public SavingsAccountLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads savings accounts.
   *
   * @param savingsAccounts list of savings accounts
   * @param context import context
   * @param result import result
   */
  public void load(
      List<SavingsAccount> savingsAccounts, ImportContext context, ImportResult result) {
    log.debug("Loading {} savings accounts", savingsAccounts.size());

    for (SavingsAccount savingsAccount : savingsAccounts) {
      try {
        loadSingleSavingsAccount(savingsAccount, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load savings account '{}': {}",
            savingsAccount.getExternalId(),
            ex.getMessage());
        result.recordEntity("savingsAccount", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single savings account.
   *
   * @param savingsAccount savings account to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleSavingsAccount(
      SavingsAccount savingsAccount, ImportContext context, ImportResult result) {
    log.debug("Loading savings account: {}", savingsAccount.getExternalId());

    // Check if savings account already exists by external ID
    if (savingsAccount.getExternalId() != null) {
      try {
        Map<String, Object> existingAccount =
            apiClient.get(
                "/api/v1/savingsaccounts?externalId=" + savingsAccount.getExternalId(), Map.class);

        if (existingAccount != null && existingAccount.containsKey("id")) {
          Long accountId = ((Number) existingAccount.get("id")).longValue();
          log.debug(
              "Savings account already exists: {} (ID: {})",
              savingsAccount.getExternalId(),
              accountId);
          result.recordEntity("savingsAccount", ImportResult.EntityAction.UNCHANGED);

          // Store for reference
          context.registerEntity("savingsAccount", savingsAccount.getExternalId(), accountId);
          return;
        }
      } catch (Exception ex) {
        // Account not found, proceed with creation
        log.debug("Savings account not found by external ID, will create new account");
      }
    }

    // Create new savings account
    Map<String, Object> request = buildRequest(savingsAccount, context);
    Map<String, Object> response = apiClient.post("/api/v1/savingsaccounts", request, Map.class);
    Long accountId = ((Number) response.get("savingsId")).longValue();

    log.info("Savings account created: {} (ID: {})", savingsAccount.getExternalId(), accountId);
    result.recordEntity("savingsAccount", ImportResult.EntityAction.CREATED);

    // Store for reference
    if (savingsAccount.getExternalId() != null) {
      context.registerEntity("savingsAccount", savingsAccount.getExternalId(), accountId);
    }

    // Activate account if requested
    if (Boolean.TRUE.equals(savingsAccount.getActive())
        && savingsAccount.getActivationDate() != null) {
      activateSavingsAccount(accountId, savingsAccount);
    }
  }

  /**
   * Builds API request for savings account.
   *
   * @param savingsAccount savings account
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(SavingsAccount savingsAccount, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);

    if (savingsAccount.getExternalId() != null) {
      request.put("externalId", savingsAccount.getExternalId());
    }

    // Resolve product
    if (savingsAccount.getProductName() != null) {
      Long productId = context.resolveEntityId("savingsProduct", savingsAccount.getProductName());
      if (productId != null) {
        request.put("productId", productId);
      } else {
        throw new IllegalStateException(
            "Savings product '" + savingsAccount.getProductName() + "' not found");
      }
    }

    // Resolve client or group
    if (savingsAccount.getClientExternalId() != null) {
      Long clientId = context.resolveEntityId("client", savingsAccount.getClientExternalId());
      if (clientId != null) {
        request.put("clientId", clientId);
      } else {
        throw new IllegalStateException(
            "Client '" + savingsAccount.getClientExternalId() + "' not found");
      }
    } else if (savingsAccount.getGroupName() != null) {
      Long groupId = context.resolveEntityId("group", savingsAccount.getGroupName());
      if (groupId != null) {
        request.put("groupId", groupId);
      } else {
        throw new IllegalStateException("Group '" + savingsAccount.getGroupName() + "' not found");
      }
    } else {
      throw new IllegalStateException(
          "Savings account must have either clientExternalId or groupName");
    }

    // Resolve field officer
    if (savingsAccount.getFieldOfficerName() != null) {
      Long fieldOfficerId = context.resolveEntityId("staff", savingsAccount.getFieldOfficerName());
      if (fieldOfficerId != null) {
        request.put("fieldOfficerId", fieldOfficerId);
      }
    }

    // Submitted date
    if (savingsAccount.getSubmittedOnDate() != null) {
      request.put("submittedOnDate", savingsAccount.getSubmittedOnDate().format(DATE_FORMATTER));
    }

    // Optional interest rate override
    if (savingsAccount.getNominalAnnualInterestRate() != null) {
      request.put("nominalAnnualInterestRate", savingsAccount.getNominalAnnualInterestRate());
    }

    return request;
  }

  /**
   * Activates a savings account.
   *
   * @param accountId account ID
   * @param savingsAccount savings account
   */
  private void activateSavingsAccount(Long accountId, SavingsAccount savingsAccount) {
    try {
      // First approve the account
      Map<String, Object> approvalRequest = new HashMap<>();
      approvalRequest.put("locale", LOCALE);
      approvalRequest.put("dateFormat", DATE_FORMAT);
      approvalRequest.put(
          "approvedOnDate", savingsAccount.getActivationDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/savingsaccounts/" + accountId + "?command=approve", approvalRequest, Map.class);

      // Then activate the account
      Map<String, Object> activationRequest = new HashMap<>();
      activationRequest.put("locale", LOCALE);
      activationRequest.put("dateFormat", DATE_FORMAT);
      activationRequest.put(
          "activatedOnDate", savingsAccount.getActivationDate().format(DATE_FORMATTER));

      apiClient.post(
          "/api/v1/savingsaccounts/" + accountId + "?command=activate",
          activationRequest,
          Map.class);

      log.info("Savings account activated: {} (ID: {})", savingsAccount.getExternalId(), accountId);
    } catch (Exception ex) {
      log.warn("Failed to activate savings account {}: {}", accountId, ex.getMessage());
    }
  }
}
