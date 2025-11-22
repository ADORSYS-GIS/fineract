package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler job configuration.
 *
 * <p>Configures Fineract scheduler jobs for background tasks like interest posting, loan
 * delinquency updates, etc.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/jobs - List all jobs
 *   <li>PUT /api/v1/jobs/{id} - Update job configuration
 *   <li>POST /api/v1/jobs/{id}?command=executeJob - Run job immediately
 * </ul>
 */
@Slf4j
@Data
public class SchedulerJob {
  /** Job name (used to identify the job in Fineract). */
  private String name;

  /** Human-readable display name. */
  private String displayName;

  /**
   * Cron expression for job scheduling.
   *
   * <p>Example: "0 0 1 * * ?" (every day at 1 AM)
   */
  private String cronExpression;

  /**
   * Whether the job is active/enabled.
   *
   * <p>When true, the job will be scheduled according to the cron expression.
   */
  @JsonProperty("active")
  private Boolean active;

  /**
   * Whether to run the job immediately after import.
   *
   * <p>When true, the job will be triggered to run once after being configured.
   */
  @JsonProperty("runOnImport")
  private Boolean runOnImport;

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    log.warn(
        "Unknown field '{}' with value '{}' in {} (will be ignored). "
            + "This may indicate a missing field in the model class.",
        key,
        value,
        this.getClass().getSimpleName());
  }
}
