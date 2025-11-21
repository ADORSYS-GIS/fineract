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
}
