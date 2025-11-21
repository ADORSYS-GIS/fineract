package org.apache.fineract.config.model.systemconfig;

import lombok.Data;

/** Scheduler job configuration. */
@Data
public class SchedulerJob {
  private String name;
  private String displayName;
  private String cronExpression;
}
