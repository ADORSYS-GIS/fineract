package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.transaction.SavingsTransaction;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for savings transactions.
 *
 * <p>Creates transactions on savings accounts (deposits, withdrawals).
 *
 * <p>API Endpoint: POST /api/v1/savingsaccounts/{accountId}/transactions
 */
@Slf4j
@Component
public class SavingsTransactionLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");

  private final FineractApiClient apiClient;

  public SavingsTransactionLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads savings transactions.
   *
   * @param transactions list of savings transactions
   * @param context import context
   * @param result import result
   */
  public void load(
      List<SavingsTransaction> transactions, ImportContext context, ImportResult result) {
    log.debug("Loading {} savings transactions", transactions.size());

    for (SavingsTransaction transaction : transactions) {
      try {
        loadSingleTransaction(transaction, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load savings transaction for account '{}': {}",
            transaction.getSavingsAccountExternalId(),
            ex.getMessage());
        result.recordEntity("savingsTransaction", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single savings transaction.
   *
   * @param transaction transaction to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleTransaction(
      SavingsTransaction transaction, ImportContext context, ImportResult result) {
    log.debug(
        "Loading savings transaction for account: {}", transaction.getSavingsAccountExternalId());

    // Resolve savings account
    Long accountId =
        context.resolveEntityId("savingsAccount", transaction.getSavingsAccountExternalId());
    if (accountId == null) {
      throw new IllegalStateException(
          "Savings account '" + transaction.getSavingsAccountExternalId() + "' not found");
    }

    // Build request
    Map<String, Object> request = buildRequest(transaction, context);

    // Determine transaction command
    String command =
        FineractEnumMapper.mapSavingsTransactionTypeToCommand(transaction.getTransactionType());

    // Post transaction
    String endpoint = "/api/v1/savingsaccounts/" + accountId + "/transactions?command=" + command;
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long transactionId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Savings transaction created: {} on account {} (ID: {})",
        transaction.getTransactionType(),
        transaction.getSavingsAccountExternalId(),
        transactionId);
    result.recordEntity("savingsTransaction", ImportResult.EntityAction.CREATED);
  }

  /**
   * Builds API request for savings transaction.
   *
   * @param transaction transaction
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(SavingsTransaction transaction, ImportContext context) {
    // Resolve payment type
    Long paymentTypeId = null;
    if (transaction.getPaymentTypeName() != null) {
      paymentTypeId = context.resolveEntityId("paymentType", transaction.getPaymentTypeName());
    }

    return RequestBuilder.forTransaction()
        .put("transactionDate", transaction.getTransactionDate().format(DATE_FORMATTER))
        .put("transactionAmount", transaction.getAmount())
        .putIfNotNull("paymentTypeId", paymentTypeId)
        .putIfNotNull("note", transaction.getNote())
        .build();
  }
}
