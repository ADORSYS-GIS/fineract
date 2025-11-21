package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** Account number format preference. */
@Data
public class AccountNumberPreference {
  private String accountType; // CLIENT, LOAN, SAVINGS, etc.
  private Integer prefixType; // 1=None, 2=Office, 3=Client, etc.
}
