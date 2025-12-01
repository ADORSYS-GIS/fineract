package org.apache.fineract.config.model.systemconfig;

import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.config.model.security.MakerCheckerConfig;

import lombok.Data;

/**
 * System-level configuration.
 *
 * <p>Phase 1: System Configuration entities that must be loaded first.
 */
@Data
public class SystemConfig {
  // Phase 1: System Configuration (8 entity types)
  private CurrencyConfig currency;
  private WorkingDaysConfig workingDays;
  private List<GlobalConfigItem> globalConfig = new ArrayList<>();
  private List<Code> codes = new ArrayList<>();
  private List<AccountNumberPreference> accountNumberPreferences = new ArrayList<>();
  private NotificationConfig notificationConfig;
  private List<NotificationTemplate> notificationTemplates = new ArrayList<>();
  private List<DataTable> dataTables = new ArrayList<>();
  private List<SchedulerJob> schedulerJobs = new ArrayList<>();
  private List<SmsEmailConfig> smsEmailConfig = new ArrayList<>();
  private List<Holiday> holidays = new ArrayList<>();
  private List<MakerCheckerConfig> makerCheckerConfig = new ArrayList<>();

  /**
   * Captures unknown fields from YAML to warn about potential model gaps. This helps identify when
   * the YAML contains fields not mapped in the model class.
   */
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void handleUnknownField(String key, Object value) {
    org.slf4j.LoggerFactory.getLogger(this.getClass())
        .warn(
            "Unknown field '{}' with value '{}' in {} (will be ignored). "
                + "This may indicate a missing field in the model class.",
            key,
            value,
            this.getClass().getSimpleName());
  }
}
