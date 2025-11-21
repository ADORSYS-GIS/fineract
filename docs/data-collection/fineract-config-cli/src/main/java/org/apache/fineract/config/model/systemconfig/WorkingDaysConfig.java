package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** Working days calendar configuration. */
@Data
public class WorkingDaysConfig {
  private String recurrence;
  private String repaymentReschedulingType;
  private boolean extendTermForDailyRepayments;
}
