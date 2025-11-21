package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/** Working days calendar configuration. */
@Data
public class WorkingDaysConfig {
  private String recurrence;

  @JsonProperty("repaymentRescheduleType")
  @JsonAlias("repaymentReschedulingType")
  private String repaymentRescheduleType;

  private boolean extendTermForDailyRepayments;
}
