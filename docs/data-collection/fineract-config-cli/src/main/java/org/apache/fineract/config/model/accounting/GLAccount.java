package org.apache.fineract.config.model.accounting;

import lombok.Data;

/**
 * General Ledger Account.
 *
 * <p>Represents a chart of accounts entry in the accounting system.
 */
@Data
public class GLAccount {
  private String name;
  private String glCode;
  private String type; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
  private String usage; // DETAIL, HEADER
  private String description;
  private Boolean manualEntriesAllowed;
  private String parentGLCode; // Reference to parent account for hierarchy
  private String tagName; // Account tag for grouping
}
