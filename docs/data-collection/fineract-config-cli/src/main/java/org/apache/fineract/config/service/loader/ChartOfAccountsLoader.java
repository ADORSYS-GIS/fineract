package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.accounting.GLAccount;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for chart of accounts (GL accounts).
 *
 * <p>Creates general ledger accounts in hierarchical structure.
 *
 * <p>API Endpoint: POST /api/v1/glaccounts
 */
@Slf4j
@Component
public class ChartOfAccountsLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public ChartOfAccountsLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads GL accounts.
   *
   * @param glAccounts list of GL accounts
   * @param context import context
   * @param result import result
   */
  public void load(List<GLAccount> glAccounts, ImportContext context, ImportResult result) {
    log.debug("Loading {} GL accounts", glAccounts.size());

    // Load accounts in order (headers before details, parents before children)
    for (GLAccount account : glAccounts) {
      try {
        loadSingleAccount(account, context, result);
      } catch (Exception ex) {
        log.error("Failed to load GL account '{}': {}", account.getName(), ex.getMessage());
        result.recordEntity("glAccount", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single GL account.
   *
   * <p>Uses upsert logic: updates existing account if found (by glCode), creates new if not.
   *
   * @param account GL account to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleAccount(GLAccount account, ImportContext context, ImportResult result) {
    log.debug("Loading GL account: {} ({})", account.getName(), account.getGlCode());

    // Build request data
    Map<String, Object> request = buildRequest(account, context);

    // Use upsert service (search by glCode, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/glaccounts",
            request,
            "glCode",
            account.getGlCode(),
            context,
            result,
            "glAccount");

    // Store for reference (important for dependency resolution)
    context.registerEntity("glAccount", account.getGlCode(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info(
          "✓ Created GL account: {} - {} (ID: {})",
          account.getGlCode(),
          account.getName(),
          upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info(
          "✓ Updated GL account: {} - {} (ID: {})",
          account.getGlCode(),
          account.getName(),
          upsertResult.getEntityId());
    } else {
      log.debug(
          "✓ GL account unchanged: {} - {} (ID: {})",
          account.getGlCode(),
          account.getName(),
          upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for GL account.
   *
   * @param account GL account
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(GLAccount account, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", account.getName());
    request.put("glCode", account.getGlCode());
    request.put("type", mapAccountType(account.getType()));
    request.put("usage", mapAccountUsage(account.getUsage()));

    if (account.getDescription() != null) {
      request.put("description", account.getDescription());
    }

    if (account.getManualEntriesAllowed() != null) {
      request.put("manualEntriesAllowed", account.getManualEntriesAllowed());
    }

    // Resolve parent account if specified
    if (account.getParentGLCode() != null) {
      Long parentId = context.resolveEntityId("glAccount", account.getParentGLCode());
      if (parentId != null) {
        request.put("parentId", parentId);
      } else {
        log.warn(
            "Parent GL account '{}' not found for account '{}'",
            account.getParentGLCode(),
            account.getGlCode());
      }
    }

    // Resolve tag if specified
    if (account.getTagName() != null) {
      Long tagId = context.resolveEntityId("accountTag", account.getTagName());
      if (tagId != null) {
        request.put("tagId", tagId);
      }
    }

    return request;
  }

  /**
   * Maps account type string to Fineract type ID.
   *
   * @param type account type
   * @return type ID
   */
  private Integer mapAccountType(String type) {
    // Fineract GL account types: 1=ASSET, 2=LIABILITY, 3=EQUITY, 4=INCOME, 5=EXPENSE
    return switch (type.toUpperCase()) {
      case "ASSET" -> 1;
      case "LIABILITY" -> 2;
      case "EQUITY" -> 3;
      case "INCOME" -> 4;
      case "EXPENSE" -> 5;
      default -> throw new IllegalArgumentException("Invalid GL account type: " + type);
    };
  }

  /**
   * Maps account usage string to Fineract usage ID.
   *
   * @param usage account usage
   * @return usage ID
   */
  private Integer mapAccountUsage(String usage) {
    // Fineract GL account usage: 1=DETAIL, 2=HEADER
    return switch (usage.toUpperCase()) {
      case "DETAIL" -> 1;
      case "HEADER" -> 2;
      default -> throw new IllegalArgumentException("Invalid GL account usage: " + usage);
    };
  }
}
