package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** SMS and Email notification configuration. */
@Slf4j
@Data
public class NotificationConfig {
  private SmsConfig sms;
  private EmailConfig email;

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

  /** SMS gateway configuration. */
  @Slf4j
  @Data
  public static class SmsConfig {
    private String providerId;
    private String hostName;
    private Integer port;
    private String accountSId;
    private String authToken;

    /**
     * Captures unknown fields from YAML to warn about potential model gaps. This helps identify
     * when the YAML contains fields not mapped in the model class.
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

  /** Email SMTP configuration. */
  @Slf4j
  @Data
  public static class EmailConfig {
    private String smtpServer;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromEmail;
    private String fromName;
    private Boolean useTLS;

    /**
     * Captures unknown fields from YAML to warn about potential model gaps. This helps identify
     * when the YAML contains fields not mapped in the model class.
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
}
