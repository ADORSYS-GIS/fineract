package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** SMS/Email provider configuration. */
@Data
public class SmsEmailConfig {
  private String name;
  private String type; // SMS Gateway, Email SMTP, Notification Settings
  private String provider; // Twilio, Infobip, Gmail, System
  private String value;
  private Boolean isActive;
  private String description;
}
