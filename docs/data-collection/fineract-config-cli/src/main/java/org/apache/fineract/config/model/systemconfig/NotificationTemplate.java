package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** Notification template (SMS/Email). */
@Data
public class NotificationTemplate {
  private String name;
  private String type; // SMS or EMAIL
  private String channel; // SMS or Email
  private String subject;
  private String text;
  private String messageBody; // Alternative to text
  private String entity; // CLIENT, LOAN, SAVINGS, etc.
  private String eventTrigger;
  private Boolean isActive;
}
