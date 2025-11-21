package org.apache.fineract.config.model.systemconfig;

import java.util.ArrayList;
import java.util.List;

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
}
