package org.apache.fineract.config.model.product;

import java.math.BigDecimal;

import lombok.Data;

/** Loan provisioning criteria configuration. */
@Data
public class LoanProvisioning {
  private String categoryName;
  private BigDecimal provisioningPercentage;
  private Integer liabilityAccountId;
  private Integer expenseAccountId;
}
