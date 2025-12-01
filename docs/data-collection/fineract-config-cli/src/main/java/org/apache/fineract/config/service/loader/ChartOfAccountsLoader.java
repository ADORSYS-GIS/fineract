package org.apache.fineract.config.service.loader;

import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.accounting.GLAccount;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
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
    RequestBuilder builder = RequestBuilder.create();

    builder.put("name", account.getName());
    builder.put("glCode", account.getGlCode());
    builder.put("type", FineractEnumMapper.mapGLAccountType(account.getType()));
    builder.put("usage", FineractEnumMapper.mapGLAccountUsage(account.getUsage()));

    builder.putIfNotNull("description", account.getDescription());
    builder.putIfNotNull("manualEntriesAllowed", account.getManualEntriesAllowed());

    // Resolve parent account if specified
    if (account.getParentGLCode() != null) {
      Long parentId = context.resolveEntityId("glAccount", account.getParentGLCode());
      if (parentId != null) {
        builder.put("parentId", parentId);
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
        builder.put("tagId", tagId);
      }
    }

    return builder.build();
  }
}
