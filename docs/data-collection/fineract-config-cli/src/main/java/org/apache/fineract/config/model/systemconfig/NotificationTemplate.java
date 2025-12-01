package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Notification template (SMS/Email). */
@Slf4j
@Data
public class NotificationTemplate {
  private String name;

  @JsonAlias("channel")
  private String type; // SMS or EMAIL

  private String channel; // SMS or Email (kept for backward compatibility)
  private String subject;

  @JsonAlias("messageBody")
  private String text;

  private String messageBody; // Alternative to text (kept for backward compatibility)

  @JsonAlias("entityType")
  private String entity; // CLIENT, LOAN, SAVINGS, etc.

  private String eventTrigger;
  private Boolean isActive;

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
