package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** SMS and Email notification configuration. */
@Data
public class NotificationConfig {
  private SmsConfig sms;
  private EmailConfig email;

  /** SMS gateway configuration. */
  @Data
  public static class SmsConfig {
    private String providerId;
    private String hostName;
    private Integer port;
    private String accountSId;
    private String authToken;
  }

  /** Email SMTP configuration. */
  @Data
  public static class EmailConfig {
    private String smtpServer;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromEmail;
    private String fromName;
    private Boolean useTLS;
  }
}
