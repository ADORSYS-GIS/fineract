package org.apache.fineract.config.model.accounting;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * General Ledger Account.
 *
 * <p>Represents a chart of accounts entry in the accounting system.
 */
@Slf4j
@Data
public class GLAccount {
  private String name;
  private String glCode;

  @JsonAlias("accountType")
  private String type; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE

  @JsonAlias("accountUsage")
  private String usage; // DETAIL, HEADER

  private String description;
  private Boolean manualEntriesAllowed;
  private String parentGLCode; // Reference to parent account for hierarchy
  private String tagName; // Account tag for grouping

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
