package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.accounting.PaymentTypeAccountingMapping;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for payment type accounting mappings.
 *
 * <p>Maps payment types to GL accounts for fund source and fee income.
 *
 * <p>API Endpoint: PUT /api/v1/paymenttypes/{paymentTypeId}
 */
@Slf4j
@Component
public class PaymentTypeAccountingMappingLoader {

  private final FineractApiClient apiClient;

  public PaymentTypeAccountingMappingLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads payment type accounting mappings.
   *
   * @param mappings list of payment type accounting mappings
   * @param context import context
   * @param result import result
   */
  public void load(
      List<PaymentTypeAccountingMapping> mappings, ImportContext context, ImportResult result) {
    log.debug("Loading {} payment type accounting mappings", mappings.size());

    for (PaymentTypeAccountingMapping mapping : mappings) {
      try {
        loadSingleMapping(mapping, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load payment type accounting mapping for '{}': {}",
            mapping.getPaymentTypeName(),
            ex.getMessage());
        result.recordEntity("paymentTypeAccountingMapping", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single payment type accounting mapping.
   *
   * @param mapping mapping to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleMapping(
      PaymentTypeAccountingMapping mapping, ImportContext context, ImportResult result) {
    log.debug("Loading payment type accounting mapping for: {}", mapping.getPaymentTypeName());

    // Resolve payment type ID
    Long paymentTypeId = context.resolveEntityId("paymentType", mapping.getPaymentTypeName());
    if (paymentTypeId == null) {
      throw new IllegalStateException(
          "Payment type '" + mapping.getPaymentTypeName() + "' not found");
    }

    // Build request with GL account mappings
    Map<String, Object> request = buildRequest(mapping, context);

    if (request.isEmpty()) {
      log.debug(
          "No accounting mappings specified for payment type: {}", mapping.getPaymentTypeName());
      result.recordEntity("paymentTypeAccountingMapping", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Update payment type with accounting mappings
    String endpoint = "/api/v1/paymenttypes/" + paymentTypeId;
    apiClient.put(endpoint, request, Map.class);

    log.info(
        "Payment type accounting mapping updated: {} (ID: {})",
        mapping.getPaymentTypeName(),
        paymentTypeId);
    result.recordEntity("paymentTypeAccountingMapping", ImportResult.EntityAction.UPDATED);
  }

  /**
   * Builds API request for payment type accounting mapping.
   *
   * @param mapping mapping
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(
      PaymentTypeAccountingMapping mapping, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    // Resolve fund source GL account
    if (mapping.getFundSourceGlCode() != null) {
      Long fundSourceId = resolveGlAccountByCode(mapping.getFundSourceGlCode(), context);
      if (fundSourceId != null) {
        request.put("fundSourceAccountId", fundSourceId);
      } else {
        log.warn(
            "GL account with code '{}' not found for payment type '{}'",
            mapping.getFundSourceGlCode(),
            mapping.getPaymentTypeName());
      }
    }

    // Resolve fee income GL account
    if (mapping.getFeeIncomeGlCode() != null) {
      Long feeIncomeId = resolveGlAccountByCode(mapping.getFeeIncomeGlCode(), context);
      if (feeIncomeId != null) {
        request.put("feeIncomeAccountId", feeIncomeId);
      } else {
        log.warn(
            "GL account with code '{}' not found for payment type '{}'",
            mapping.getFeeIncomeGlCode(),
            mapping.getPaymentTypeName());
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
