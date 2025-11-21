package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Account number format preference. */
@Slf4j
@Data
public class AccountNumberPreference {
  private String accountType; // CLIENT, LOAN, SAVINGS, etc.
  private Integer prefixType; // 1=None, 2=Office, 3=Client, etc.

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
