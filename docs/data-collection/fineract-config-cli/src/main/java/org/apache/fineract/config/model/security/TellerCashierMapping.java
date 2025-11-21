package org.apache.fineract.config.model.security;

import lombok.Data;

/** Teller-cashier mapping configuration. */
@Data
public class TellerCashierMapping {
  private String tellerName;
  private String description;
}
