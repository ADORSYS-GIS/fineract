package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** SMS/Email provider configuration. */
@Slf4j
@Data
public class SmsEmailConfig {
  private String name;
  private String type; // SMS Gateway, Email SMTP, Notification Settings
  private String provider; // Twilio, Infobip, Gmail, System
  private String value;
  private Boolean isActive;
  private String description;

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
