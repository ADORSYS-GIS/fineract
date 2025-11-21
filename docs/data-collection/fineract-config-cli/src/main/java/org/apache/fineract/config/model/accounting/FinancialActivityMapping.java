package org.apache.fineract.config.model.accounting;

import lombok.Data;

/**
 * Financial Activity to GL Account Mapping.
 *
 * <p>Maps financial activities to specific GL accounts for automatic posting.
 */
@Data
public class FinancialActivityMapping {
  private String financialActivity; // Activity type (e.g., ASSET_TRANSFER, LIABILITY_TRANSFER)
  private String glAccountCode; // GL account code to map to
}
