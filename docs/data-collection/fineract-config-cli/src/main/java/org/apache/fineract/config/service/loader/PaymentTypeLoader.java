package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.PaymentType;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.UpsertService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for payment types.
 *
 * <p>Creates payment methods available in the system.
 *
 * <p>API Endpoint: POST /api/v1/paymenttypes
 */
@Slf4j
@Component
public class PaymentTypeLoader {

  private final FineractApiClient apiClient;
  private final UpsertService upsertService;

  public PaymentTypeLoader(FineractApiClient apiClient, UpsertService upsertService) {
    this.apiClient = apiClient;
    this.upsertService = upsertService;
  }

  /**
   * Loads payment types.
   *
   * @param paymentTypes list of payment types
   * @param context import context
   * @param result import result
   */
  public void load(List<PaymentType> paymentTypes, ImportContext context, ImportResult result) {
    log.debug("Loading {} payment types", paymentTypes.size());

    for (PaymentType paymentType : paymentTypes) {
      try {
        loadSinglePaymentType(paymentType, context, result);
      } catch (Exception ex) {
        log.error("Failed to load payment type '{}': {}", paymentType.getName(), ex.getMessage());
        result.recordEntity("paymentType", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single payment type.
   *
   * <p>Uses upsert logic: updates existing payment type if found (by name), creates new if not.
   *
   * @param paymentType payment type to load
   * @param context import context
   * @param result import result
   */
  private void loadSinglePaymentType(
      PaymentType paymentType, ImportContext context, ImportResult result) {
    log.debug("Loading payment type: {}", paymentType.getName());

    // Build request data
    Map<String, Object> request = buildRequest(paymentType);

    // Use upsert service (search by name, update if exists, create if not)
    UpsertService.UpsertResult upsertResult =
        upsertService.upsert(
            "/api/v1/paymenttypes",
            request,
            "name",
            paymentType.getName(),
            context,
            result,
            "paymentType");

    // Store for reference (important for dependency resolution)
    context.registerEntity("paymentType", paymentType.getName(), upsertResult.getEntityId());

    if (upsertResult.wasCreated()) {
      log.info(
          "✓ Created payment type: {} (ID: {})", paymentType.getName(), upsertResult.getEntityId());
    } else if (upsertResult.wasUpdated()) {
      log.info(
          "✓ Updated payment type: {} (ID: {})", paymentType.getName(), upsertResult.getEntityId());
    } else {
      log.debug(
          "✓ Payment type unchanged: {} (ID: {})",
          paymentType.getName(),
          upsertResult.getEntityId());
    }
  }

  /**
   * Builds API request for payment type.
   *
   * @param paymentType payment type
   * @return request map
   */
  private Map<String, Object> buildRequest(PaymentType paymentType) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", paymentType.getName());

    if (paymentType.getDescription() != null) {
      request.put("description", paymentType.getDescription());
    }

    if (paymentType.getIsCashPayment() != null) {
      request.put("isCashPayment", paymentType.getIsCashPayment());
    }

    if (paymentType.getPosition() != null) {
      request.put("position", paymentType.getPosition());
    }

    return request;
  }
}
