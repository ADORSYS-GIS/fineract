package org.apache.fineract.config.model.account;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Loan guarantor configuration.
 *
 * <p>Represents a guarantor for a loan account.
 *
 * <p>API Endpoint: POST /api/v1/loans/{loanId}/guarantors
 *
 * <p>YAML Example:
 *
 * <pre>
 * loanGuarantors:
 *   - loanExternalId: LOAN-001
 *     guarantorType: CLIENT
 *     clientExternalId: CLI-002
 *     amount: 100000.00
 * </pre>
 */
@Slf4j
@Data
public class LoanGuarantor {
  /** External ID of the loan account. */
  @JsonProperty("loanExternalId")
  private String loanExternalId;

  /**
   * Type of guarantor.
   *
   * <p>Valid values:
   *
   * <ul>
   *   <li>CLIENT - An existing client in the system
   *   <li>STAFF - A staff member
   *   <li>EXTERNAL - An external person (not in system)
   * </ul>
   */
  @JsonProperty("guarantorType")
  private String guarantorType;

  /** External ID of the client (if guarantorType is CLIENT). */
  @JsonProperty("clientExternalId")
  private String clientExternalId;

  /** Name of the staff member (if guarantorType is STAFF). */
  @JsonProperty("staffName")
  private String staffName;

  /** First name of external guarantor (if guarantorType is EXTERNAL). */
  @JsonProperty("firstname")
  private String firstname;

  /** Last name of external guarantor (if guarantorType is EXTERNAL). */
  @JsonProperty("lastname")
  private String lastname;

  /** Address of external guarantor. */
  @JsonProperty("addressLine1")
  private String addressLine1;

  /** City of external guarantor. */
  @JsonProperty("city")
  private String city;

  /** Guaranteed amount. */
  @JsonProperty("amount")
  private BigDecimal amount;

  /** Savings account external ID to hold as collateral. */
  @JsonProperty("savingsExternalId")
  private String savingsExternalId;

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    log.warn(
        "Unknown field '{}' with value '{}' in {} (will be ignored). "
            + "This may indicate a missing field in the model class.",
        key,
        value,
        this.getClass().getSimpleName());
  }
}
