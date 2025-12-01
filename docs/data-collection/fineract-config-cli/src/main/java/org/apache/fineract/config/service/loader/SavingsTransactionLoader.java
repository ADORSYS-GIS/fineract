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
            transaction.getAccountExternalId(),
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
    log.debug("Loading savings transaction for account: {}", transaction.getAccountExternalId());

    // Resolve savings account
    Long accountId = context.resolveEntityId("savingsAccount", transaction.getAccountExternalId());
    if (accountId == null) {
      throw new IllegalStateException(
          "Savings account '" + transaction.getAccountExternalId() + "' not found");
    }

    // Check if similar transaction already exists (idempotency)
    String command =
        FineractEnumMapper.mapSavingsTransactionTypeToCommand(transaction.getTransactionType());
    if (transactionAlreadyExists(accountId, transaction, command)) {
      log.info(
          "Savings transaction already exists: {} {} on account {}, skipping",
          transaction.getTransactionType(),
          transaction.getTransactionAmount(),
          transaction.getAccountExternalId());
      result.recordEntity("savingsTransaction", ImportResult.EntityAction.UNCHANGED);
      return;
    }

    // Build request
    Map<String, Object> request = buildRequest(transaction, context);

    // Post transaction
    String endpoint = "/api/v1/savingsaccounts/" + accountId + "/transactions?command=" + command;
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long transactionId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Savings transaction created: {} on account {} (ID: {})",
        transaction.getTransactionType(),
        transaction.getAccountExternalId(),
        transactionId);
    result.recordEntity("savingsTransaction", ImportResult.EntityAction.CREATED);
  }

  /**
   * Checks if a similar transaction already exists for the account.
   *
   * <p>Checks for matching transaction type, date, and amount to avoid duplicates.
   *
   * @param accountId account ID
   * @param transaction transaction to check
   * @param transactionCommand transaction command (deposit/withdrawal)
   * @return true if similar transaction already exists
   */
  @SuppressWarnings("unchecked")
  private boolean transactionAlreadyExists(
      Long accountId, SavingsTransaction transaction, String transactionCommand) {
    try {
      // Get account with transactions - use String to avoid deserialization issues with large
      // responses
      String endpoint = "/api/v1/savingsaccounts/" + accountId + "?associations=transactions";
      String responseStr = apiClient.get(endpoint, String.class);

      // Parse response manually
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      Map<String, Object> account = mapper.readValue(responseStr, Map.class);

      List<Map<String, Object>> transactions =
          (List<Map<String, Object>>) account.get("transactions");
      if (transactions == null || transactions.isEmpty()) {
        return false;
      }

      java.math.BigDecimal expectedAmount = transaction.getTransactionAmount();

      // Map command to transaction type value
      String expectedType = "deposit".equals(transactionCommand) ? "Deposit" : "Withdrawal";

      for (Map<String, Object> existing : transactions) {
        // Check transaction type
        Map<String, Object> typeObj = (Map<String, Object>) existing.get("transactionType");
        if (typeObj == null) {
          continue;
        }
        String existingType = (String) typeObj.get("value");
        if (!expectedType.equalsIgnoreCase(existingType)) {
          continue;
        }

        // Check amount
        Object amountObj = existing.get("amount");
        if (amountObj == null) {
          continue;
        }
        java.math.BigDecimal existingAmount = new java.math.BigDecimal(amountObj.toString());
        if (existingAmount.compareTo(expectedAmount) != 0) {
          continue;
        }

        // Check date - Fineract returns date as array [year, month, day]
        List<Integer> dateArray = (List<Integer>) existing.get("date");
        if (dateArray != null && dateArray.size() >= 3) {
          java.time.LocalDate existingDate =
              java.time.LocalDate.of(dateArray.get(0), dateArray.get(1), dateArray.get(2));
          if (existingDate.equals(transaction.getTransactionDate())) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception ex) {
      log.debug("Error checking existing savings transactions: {}", ex.getMessage());
      return false;
    }
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
        .put("transactionAmount", transaction.getTransactionAmount())
        .putIfNotNull("paymentTypeId", paymentTypeId)
        .putIfNotNull("note", transaction.getNote())
        .build();
  }
}
