package org.apache.fineract.config.service.loader;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.transaction.LoanTransaction;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for loan transactions.
 *
 * <p>Creates transactions on loan accounts (repayments, waivers, writeoffs).
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/transactions
 */
@Slf4j
@Component
public class LoanTransactionLoader {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final String LOCALE = "en";
  private static final String DATE_FORMAT = "dd MMMM yyyy";

  private final FineractApiClient apiClient;

  public LoanTransactionLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads loan transactions.
   *
   * @param transactions list of loan transactions
   * @param context import context
   * @param result import result
   */
  public void load(List<LoanTransaction> transactions, ImportContext context, ImportResult result) {
    log.debug("Loading {} loan transactions", transactions.size());

    for (LoanTransaction transaction : transactions) {
      try {
        loadSingleTransaction(transaction, context, result);
      } catch (Exception ex) {
        log.error(
            "Failed to load loan transaction for account '{}': {}",
            transaction.getLoanAccountExternalId(),
            ex.getMessage());
        result.recordEntity("loanTransaction", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single loan transaction.
   *
   * @param transaction transaction to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleTransaction(
      LoanTransaction transaction, ImportContext context, ImportResult result) {
    log.debug("Loading loan transaction for account: {}", transaction.getLoanAccountExternalId());

    // Resolve loan account
    Long loanId = context.resolveEntityId("loanAccount", transaction.getLoanAccountExternalId());
    if (loanId == null) {
      throw new IllegalStateException(
          "Loan account '" + transaction.getLoanAccountExternalId() + "' not found");
    }

    // Build request
    Map<String, Object> request = buildRequest(transaction, context);

    // Determine transaction command
    String command = mapTransactionTypeToCommand(transaction.getTransactionType());

    // Post transaction
    String endpoint = "/api/v1/loans/" + loanId + "/transactions?command=" + command;
    Map<String, Object> response = apiClient.post(endpoint, request, Map.class);
    Long transactionId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Loan transaction created: {} on account {} (ID: {})",
        transaction.getTransactionType(),
        transaction.getLoanAccountExternalId(),
        transactionId);
    result.recordEntity("loanTransaction", ImportResult.EntityAction.CREATED);
  }

  /**
   * Builds API request for loan transaction.
   *
   * @param transaction transaction
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildRequest(LoanTransaction transaction, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("locale", LOCALE);
    request.put("dateFormat", DATE_FORMAT);
    request.put("transactionDate", transaction.getTransactionDate().format(DATE_FORMATTER));

    // Amount is only needed for repayments
    if ("REPAYMENT".equalsIgnoreCase(transaction.getTransactionType())
        && transaction.getAmount() != null) {
      request.put("transactionAmount", transaction.getAmount());
    }

    // Resolve payment type
    if (transaction.getPaymentTypeName() != null) {
      Long paymentTypeId = context.resolveEntityId("paymentType", transaction.getPaymentTypeName());
      if (paymentTypeId != null) {
        request.put("paymentTypeId", paymentTypeId);
      }
    }

    // Add note
    if (transaction.getNote() != null) {
      request.put("note", transaction.getNote());
    }

    return request;
  }

  /**
   * Maps transaction type to Fineract API command.
   *
   * @param transactionType transaction type
   * @return API command
   */
  private String mapTransactionTypeToCommand(String transactionType) {
    return switch (transactionType.toUpperCase()) {
      case "REPAYMENT" -> "repayment";
      case "WAIVER" -> "waiveinterest";
      case "WRITEOFF" -> "writeoff";
      default -> throw new IllegalArgumentException(
          "Invalid loan transaction type: " + transactionType);
    };
  }
}
