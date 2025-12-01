package org.apache.fineract.config.service;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.config.model.ExportOptions;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for exporting Fineract configuration to YAML.
 *
 * <p>Retrieves configuration from Fineract API and exports to YAML format.
 */
@Slf4j
@Service
public class ExportService {

  private final FineractApiClient apiClient;

  public ExportService(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Export Fineract configuration to YAML file (legacy method).
   *
   * @param outputPath output file path
   * @param phases phases to export (comma-separated or "all")
   */
  public void exportConfiguration(String outputPath, String phases) {
    List<Integer> phasesList = parsePhases(phases);
    ExportOptions.ExportOptionsBuilder optionsBuilder = ExportOptions.builder();

    if (!phasesList.isEmpty()) {
      optionsBuilder.phases(Set.copyOf(phasesList));
    }

    exportConfiguration(outputPath, optionsBuilder.build());
  }

  /**
   * Export Fineract configuration to YAML file with advanced options.
   *
   * @param outputPath output file path
   * @param options export options for filtering and customization
   */
  public void exportConfiguration(String outputPath, ExportOptions options) {
    log.info("Exporting configuration to: {}", outputPath);
    if (options.isActiveOnly()) {
      log.info("Filter: Active entities only");
    }
    if (!options.getExcludeEntityTypes().isEmpty()) {
      log.info("Excluding entity types: {}", options.getExcludeEntityTypes());
    }

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("tenant", "default");

    // Phase 1: System Configuration
    if (options.shouldExportPhase(1)) {
      log.info("Exporting Phase 1: System Configuration");
      exportSystemConfig(config, options);
    }

    // Phase 2: Security & Organization
    if (options.shouldExportPhase(2)) {
      log.info("Exporting Phase 2: Security & Organization");
      exportSecurityAndOrganization(config, options);
    }

    // Phase 3: Accounting Foundation
    if (options.shouldExportPhase(3)) {
      log.info("Exporting Phase 3: Accounting Foundation");
      exportAccountingFoundation(config, options);
    }

    // Phase 4: Financial Products
    if (options.shouldExportPhase(4)) {
      log.info("Exporting Phase 4: Financial Products");
      exportFinancialProducts(config, options);
    }

    // Phase 5: Client Operations & Accounts
    if (options.shouldExportPhase(5)) {
      log.info("Exporting Phase 5: Client Operations & Accounts");
      exportClientOperations(config, options);
    }

    // Phase 6: Transactions & Operations
    if (options.shouldExportPhase(6)) {
      log.info("Exporting Phase 6: Transactions & Operations");
      // Note: Transactions are typically not exported as they're operational data
      log.info("Skipping transactions export (operational data)");
    }

    // Write to YAML file
    writeYamlFile(config, outputPath, options);

    log.info("Export completed successfully");
  }

  private void exportSystemConfig(Map<String, Object> config, ExportOptions options) {
    Map<String, Object> systemConfig = new LinkedHashMap<>();

    try {
      // 1. Currency Configuration
      if (options.shouldExportEntityType("currency")) {
        exportCurrency(systemConfig, options);
      }

      // 2. Working Days Configuration
      if (options.shouldExportEntityType("workingDays")) {
        exportWorkingDays(systemConfig, options);
      }

      // 3. Global Configuration Items
      if (options.shouldExportEntityType("globalConfig")) {
        exportGlobalConfig(systemConfig, options);
      }

      // 4. Codes and Code Values
      if (options.shouldExportEntityType("codes")) {
        exportCodes(systemConfig, options);
      }

      // 5. Account Number Preferences
      if (options.shouldExportEntityType("accountNumberPreferences")) {
        exportAccountNumberPreferences(systemConfig, options);
      }

      // 6. Notification Configuration
      if (options.shouldExportEntityType("notificationConfig")) {
        exportNotificationConfig(systemConfig, options);
      }

      // 7. Notification Templates
      if (options.shouldExportEntityType("notificationTemplates")) {
        exportNotificationTemplates(systemConfig, options);
      }

      // 8. Data Tables (Custom Fields)
      if (options.shouldExportEntityType("dataTables")) {
        exportDataTables(systemConfig, options);
      }

      if (!systemConfig.isEmpty()) {
        config.put("systemConfig", systemConfig);
      }

    } catch (Exception ex) {
      log.warn("Failed to export system config: {}", ex.getMessage());
    }
  }

  // Wrapper methods for backward compatibility
  private void exportCurrency(Map<String, Object> systemConfig, ExportOptions options) {
    exportCurrency(systemConfig);
  }

  private void exportCurrency(Map<String, Object> systemConfig) {
    try {
      // Get organization currency
      Map<String, Object> currencies = apiClient.get("/api/v1/currencies", Map.class);
      if (currencies != null && currencies.containsKey("selectedCurrencyOptions")) {
        List<Map<String, Object>> selected =
            (List<Map<String, Object>>) currencies.get("selectedCurrencyOptions");
        if (selected != null && !selected.isEmpty()) {
          // Use first selected currency as organization currency
          Map<String, Object> primaryCurrency = selected.get(0);
          Map<String, Object> currencyData = new LinkedHashMap<>();
          currencyData.put("code", primaryCurrency.get("code"));
          currencyData.put("name", primaryCurrency.get("name"));
          currencyData.put("decimalPlaces", primaryCurrency.get("decimalPlaces"));
          currencyData.put("inMultiplesOf", primaryCurrency.get("inMultiplesOf"));
          systemConfig.put("currency", currencyData);
        }
      }
    } catch (Exception ex) {
      log.debug("Failed to export currency: {}", ex.getMessage());
    }
  }

  private void exportWorkingDays(Map<String, Object> systemConfig, ExportOptions options) {
    exportWorkingDays(systemConfig);
  }

  private void exportWorkingDays(Map<String, Object> systemConfig) {
    try {
      Map<String, Object> workingDays = apiClient.get("/api/v1/workingdays", Map.class);
      if (workingDays != null) {
        Map<String, Object> workingDaysData = new LinkedHashMap<>();
        workingDaysData.put("recurrence", workingDays.get("recurrence"));
        workingDaysData.put(
            "repaymentReschedulingType",
            mapRepaymentReschedulingType((Integer) workingDays.get("repaymentReschedulingType")));
        workingDaysData.put(
            "extendTermForDailyRepayments", workingDays.get("extendTermForDailyRepayments"));
        systemConfig.put("workingDays", workingDaysData);
      }
    } catch (Exception ex) {
      log.debug("Failed to export working days: {}", ex.getMessage());
    }
  }

  private void exportGlobalConfig(Map<String, Object> systemConfig, ExportOptions options) {
    exportGlobalConfig(systemConfig);
  }

  private void exportGlobalConfig(Map<String, Object> systemConfig) {
    try {
      List<Map<String, Object>> globalConfigs = apiClient.get("/api/v1/configurations", List.class);
      if (globalConfigs != null && !globalConfigs.isEmpty()) {
        List<Map<String, Object>> exportedConfigs = new ArrayList<>();
        for (Map<String, Object> item : globalConfigs) {
          // Only export user-configurable items (not system defaults)
          if (Boolean.TRUE.equals(item.get("enabled"))) {
            Map<String, Object> configData = new LinkedHashMap<>();
            configData.put("name", item.get("name"));
            configData.put("enabled", item.get("enabled"));
            if (item.get("value") != null) {
              configData.put("value", item.get("value"));
            }
            if (item.get("description") != null) {
              configData.put("description", item.get("description"));
            }
            exportedConfigs.add(configData);
          }
        }
        if (!exportedConfigs.isEmpty()) {
          systemConfig.put("globalConfig", exportedConfigs);
        }
      }
    } catch (Exception ex) {
      log.debug("Failed to export global config: {}", ex.getMessage());
    }
  }

  private void exportCodes(Map<String, Object> systemConfig, ExportOptions options) {
    exportCodes(systemConfig);
  }

  private void exportCodes(Map<String, Object> systemConfig) {
    try {
      List<Map<String, Object>> codes = apiClient.get("/api/v1/codes", List.class);
      if (codes != null && !codes.isEmpty()) {
        List<Map<String, Object>> exportedCodes = new ArrayList<>();
        for (Map<String, Object> code : codes) {
          // Skip system codes
          if (Boolean.TRUE.equals(code.get("isSystemDefined"))) {
            continue;
          }

          Map<String, Object> codeData = new LinkedHashMap<>();
          codeData.put("name", code.get("name"));

          // Get code values
          Long codeId = ((Number) code.get("id")).longValue();
          List<Map<String, Object>> codeValues =
              apiClient.get("/api/v1/codes/" + codeId + "/codevalues", List.class);

          if (codeValues != null && !codeValues.isEmpty()) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (Map<String, Object> value : codeValues) {
              Map<String, Object> valueData = new LinkedHashMap<>();
              valueData.put("name", value.get("name"));
              valueData.put("position", value.get("position"));
              valueData.put("isActive", value.get("isActive"));
              if (value.get("description") != null) {
                valueData.put("description", value.get("description"));
              }
              values.add(valueData);
            }
            codeData.put("values", values);
          }

          exportedCodes.add(codeData);
        }
        if (!exportedCodes.isEmpty()) {
          systemConfig.put("codes", exportedCodes);
        }
      }
    } catch (Exception ex) {
      log.debug("Failed to export codes: {}", ex.getMessage());
    }
  }

  private void exportAccountNumberPreferences(
      Map<String, Object> systemConfig, ExportOptions options) {
    exportAccountNumberPreferences(systemConfig);
  }

  private void exportNotificationConfig(Map<String, Object> systemConfig, ExportOptions options) {
    exportNotificationConfig(systemConfig);
  }

  private void exportNotificationTemplates(
      Map<String, Object> systemConfig, ExportOptions options) {
    exportNotificationTemplates(systemConfig);
  }

  private void exportDataTables(Map<String, Object> systemConfig, ExportOptions options) {
    exportDataTables(systemConfig);
  }

  private void exportSecurityAndOrganization(Map<String, Object> config, ExportOptions options) {
    exportSecurityAndOrganization(config);
  }

  private void exportAccountingFoundation(Map<String, Object> config, ExportOptions options) {
    exportAccountingFoundation(config);
  }

  private void exportFinancialProducts(Map<String, Object> config, ExportOptions options) {
    exportFinancialProducts(config);
  }

  private void exportClientOperations(Map<String, Object> config, ExportOptions options) {
    exportClientOperations(config);
  }

  private void exportAccountNumberPreferences(Map<String, Object> systemConfig) {
    try {
      List<Map<String, Object>> preferences =
          apiClient.get("/api/v1/accountnumberformats", List.class);
      if (preferences != null && !preferences.isEmpty()) {
        List<Map<String, Object>> exportedPrefs = new ArrayList<>();
        for (Map<String, Object> pref : preferences) {
          Map<String, Object> prefData = new LinkedHashMap<>();
          prefData.put("accountType", mapAccountTypeFromId((Integer) pref.get("accountType")));
          prefData.put("prefixType", mapPrefixTypeFromId((Integer) pref.get("prefixType")));
          exportedPrefs.add(prefData);
        }
        systemConfig.put("accountNumberPreferences", exportedPrefs);
      }
    } catch (Exception ex) {
      log.debug("Failed to export account number preferences: {}", ex.getMessage());
    }
  }

  private void exportNotificationConfig(Map<String, Object> systemConfig) {
    try {
      Map<String, Object> config = apiClient.get("/api/v1/notification", Map.class);
      if (config != null) {
        Map<String, Object> notificationData = new LinkedHashMap<>();
        notificationData.put("smsEnabled", config.get("smsEnabled"));
        notificationData.put("emailEnabled", config.get("emailEnabled"));
        systemConfig.put("notificationConfig", notificationData);
      }
    } catch (Exception ex) {
      log.debug("Failed to export notification config: {}", ex.getMessage());
    }
  }

  private void exportNotificationTemplates(Map<String, Object> systemConfig) {
    try {
      List<Map<String, Object>> templates = apiClient.get("/api/v1/templates", List.class);
      if (templates != null && !templates.isEmpty()) {
        List<Map<String, Object>> exportedTemplates = new ArrayList<>();
        for (Map<String, Object> template : templates) {
          Map<String, Object> templateData = new LinkedHashMap<>();
          templateData.put("name", template.get("name"));
          templateData.put("type", template.get("type"));
          templateData.put("text", template.get("text"));
          if (template.get("subject") != null) {
            templateData.put("subject", template.get("subject"));
          }
          exportedTemplates.add(templateData);
        }
        systemConfig.put("notificationTemplates", exportedTemplates);
      }
    } catch (Exception ex) {
      log.debug("Failed to export notification templates: {}", ex.getMessage());
    }
  }

  private void exportDataTables(Map<String, Object> systemConfig) {
    try {
      List<Map<String, Object>> dataTables = apiClient.get("/api/v1/datatables", List.class);
      if (dataTables != null && !dataTables.isEmpty()) {
        List<Map<String, Object>> exportedTables = new ArrayList<>();
        for (Map<String, Object> dataTable : dataTables) {
          Map<String, Object> tableData = new LinkedHashMap<>();
          tableData.put("registeredTableName", dataTable.get("registeredTableName"));
          tableData.put("applicationTableName", dataTable.get("applicationTableName"));

          // Get full data table definition
          String tableName = (String) dataTable.get("registeredTableName");
          Map<String, Object> fullTable =
              apiClient.get("/api/v1/datatables/" + tableName, Map.class);
          if (fullTable != null && fullTable.containsKey("columnHeaderData")) {
            List<Map<String, Object>> columns =
                (List<Map<String, Object>>) fullTable.get("columnHeaderData");
            if (columns != null && !columns.isEmpty()) {
              List<Map<String, Object>> columnData = new ArrayList<>();
              for (Map<String, Object> column : columns) {
                // Skip system columns
                String columnName = (String) column.get("columnName");
                if (columnName != null && !columnName.equals("id")) {
                  Map<String, Object> colData = new LinkedHashMap<>();
                  colData.put("name", column.get("columnName"));
                  colData.put("type", column.get("columnType"));
                  colData.put("mandatory", column.get("isColumnNullable"));
                  if (column.get("columnLength") != null) {
                    colData.put("length", column.get("columnLength"));
                  }
                  columnData.add(colData);
                }
              }
              tableData.put("columns", columnData);
            }
          }

          exportedTables.add(tableData);
        }
        if (!exportedTables.isEmpty()) {
          systemConfig.put("dataTables", exportedTables);
        }
      }
    } catch (Exception ex) {
      log.debug("Failed to export data tables: {}", ex.getMessage());
    }
  }

  private String mapRepaymentReschedulingType(Integer typeId) {
    if (typeId == null) return "MOVE_TO_NEXT_WORKING_DAY";
    return switch (typeId) {
      case 1 -> "SAME_DAY";
      case 2 -> "MOVE_TO_NEXT_WORKING_DAY";
      case 3 -> "MOVE_TO_NEXT_REPAYMENT_MEETING_DAY";
      case 4 -> "MOVE_TO_PREVIOUS_WORKING_DAY";
      default -> "MOVE_TO_NEXT_WORKING_DAY";
    };
  }

  private String mapAccountTypeFromId(Integer typeId) {
    if (typeId == null) return "CLIENT";
    return switch (typeId) {
      case 1 -> "CLIENT";
      case 2 -> "LOAN";
      case 3 -> "SAVINGS";
      case 4 -> "CENTER";
      case 5 -> "GROUP";
      default -> "CLIENT";
    };
  }

  private String mapPrefixTypeFromId(Integer prefixId) {
    if (prefixId == null) return "NONE";
    return switch (prefixId) {
      case 1 -> "CLIENT_TYPE";
      case 2 -> "OFFICE_NAME";
      case 3 -> "LOAN_PRODUCT_SHORT_NAME";
      case 4 -> "SAVINGS_PRODUCT_SHORT_NAME";
      default -> "NONE";
    };
  }

  private void exportSecurityAndOrganization(Map<String, Object> config) {
    try {
      // 1. Export offices
      List<Map<String, Object>> offices = apiClient.get("/api/v1/offices", List.class);
      if (offices != null && !offices.isEmpty()) {
        List<Map<String, Object>> exportedOffices = new ArrayList<>();
        for (Map<String, Object> office : offices) {
          Map<String, Object> officeData = new LinkedHashMap<>();
          officeData.put("name", office.get("name"));
          if (office.get("externalId") != null) {
            officeData.put("externalId", office.get("externalId"));
          }
          officeData.put("openingDate", office.get("openingDate"));
          if (office.get("parentId") != null) {
            officeData.put("parentName", office.get("parentName"));
          }
          exportedOffices.add(officeData);
        }
        config.put("offices", exportedOffices);
      }

      // 2. Export roles
      List<Map<String, Object>> roles = apiClient.get("/api/v1/roles", List.class);
      if (roles != null && !roles.isEmpty()) {
        List<Map<String, Object>> exportedRoles = new ArrayList<>();
        for (Map<String, Object> role : roles) {
          // Skip disabled roles
          if (Boolean.TRUE.equals(role.get("disabled"))) {
            continue;
          }

          Map<String, Object> roleData = new LinkedHashMap<>();
          roleData.put("name", role.get("name"));
          roleData.put("description", role.get("description"));

          // Get role permissions
          Long roleId = ((Number) role.get("id")).longValue();
          Map<String, Object> roleDetails = apiClient.get("/api/v1/roles/" + roleId, Map.class);
          if (roleDetails != null && roleDetails.containsKey("permissions")) {
            List<Map<String, Object>> permissions =
                (List<Map<String, Object>>) roleDetails.get("permissions");
            if (permissions != null && !permissions.isEmpty()) {
              List<String> permissionCodes = new ArrayList<>();
              for (Map<String, Object> perm : permissions) {
                if (Boolean.TRUE.equals(perm.get("selected"))) {
                  permissionCodes.add((String) perm.get("code"));
                }
              }
              if (!permissionCodes.isEmpty()) {
                roleData.put("permissions", permissionCodes);
              }
            }
          }

          exportedRoles.add(roleData);
        }
        config.put("roles", exportedRoles);
      }

      // 3. Export users (with password sanitization)
      List<Map<String, Object>> users = apiClient.get("/api/v1/users", List.class);
      if (users != null && !users.isEmpty()) {
        List<Map<String, Object>> exportedUsers = new ArrayList<>();
        for (Map<String, Object> user : users) {
          // Skip system users
          if ("system".equalsIgnoreCase((String) user.get("username"))) {
            continue;
          }

          Map<String, Object> userData = new LinkedHashMap<>();
          userData.put("username", user.get("username"));
          userData.put("firstname", user.get("firstname"));
          userData.put("lastname", user.get("lastname"));
          userData.put("email", user.get("email"));

          // PASSWORD SANITIZED - Use placeholder that forces reset
          userData.put(
              "password", "***EXPORTED***"); // Must be changed when importing to different system
          userData.put("passwordNeverExpires", user.get("passwordNeverExpire"));

          if (user.get("officeName") != null) {
            userData.put("officeName", user.get("officeName"));
          }

          // Get user roles
          if (user.get("selectedRoles") != null) {
            List<Map<String, Object>> selectedRoles =
                (List<Map<String, Object>>) user.get("selectedRoles");
            if (selectedRoles != null && !selectedRoles.isEmpty()) {
              List<String> roleNames = new ArrayList<>();
              for (Map<String, Object> selectedRole : selectedRoles) {
                roleNames.add((String) selectedRole.get("name"));
              }
              userData.put("roles", roleNames);
            }
          }

          userData.put("isSelfServiceUser", user.get("selfServiceUser"));

          exportedUsers.add(userData);
        }
        config.put("users", exportedUsers);
      }

      // 4. Export staff
      List<Map<String, Object>> staff = apiClient.get("/api/v1/staff", List.class);
      if (staff != null && !staff.isEmpty()) {
        List<Map<String, Object>> exportedStaff = new ArrayList<>();
        for (Map<String, Object> staffMember : staff) {
          Map<String, Object> staffData = new LinkedHashMap<>();

          if (staffMember.get("externalId") != null) {
            staffData.put("externalId", staffMember.get("externalId"));
          }
          if (staffMember.get("officeName") != null) {
            staffData.put("officeName", staffMember.get("officeName"));
          }
          staffData.put("firstName", staffMember.get("firstname"));
          staffData.put("lastName", staffMember.get("lastname"));
          staffData.put("isLoanOfficer", staffMember.get("isLoanOfficer"));
          staffData.put("isActive", staffMember.get("isActive"));

          if (staffMember.get("joiningDate") != null) {
            // Convert date array to list format
            List<Integer> joiningDate = (List<Integer>) staffMember.get("joiningDate");
            staffData.put("joiningDate", joiningDate);
          }

          if (staffMember.get("mobileNo") != null) {
            staffData.put("mobileNo", staffMember.get("mobileNo"));
          }
          if (staffMember.get("emailAddress") != null) {
            staffData.put("emailAddress", staffMember.get("emailAddress"));
          }

          exportedStaff.add(staffData);
        }
        config.put("staff", exportedStaff);
      }

    } catch (Exception ex) {
      log.warn("Failed to export security and organization data: {}", ex.getMessage());
    }
  }

  private void exportAccountingFoundation(Map<String, Object> config) {
    try {
      // 1. Export GL accounts
      List<Map<String, Object>> glAccounts = apiClient.get("/api/v1/glaccounts", List.class);
      if (glAccounts != null && !glAccounts.isEmpty()) {
        List<Map<String, Object>> exportedAccounts = new ArrayList<>();
        for (Map<String, Object> account : glAccounts) {
          Map<String, Object> accountData = new LinkedHashMap<>();
          accountData.put("name", account.get("name"));
          accountData.put("glCode", account.get("glCode"));
          accountData.put("type", mapGLAccountTypeFromId((Integer) account.get("type")));
          accountData.put("usage", mapAccountUsageFromId((Integer) account.get("usage")));
          if (account.get("description") != null) {
            accountData.put("description", account.get("description"));
          }
          if (account.get("parentId") != null) {
            accountData.put("parentGlCode", account.get("parentGlCode"));
          }
          if (account.get("tagId") != null) {
            accountData.put("tagName", account.get("tagName"));
          }
          accountData.put("manualEntriesAllowed", account.get("manualEntriesAllowed"));
          exportedAccounts.add(accountData);
        }
        config.put("chartOfAccounts", exportedAccounts);
      }

      // 2. Export payment types
      List<Map<String, Object>> paymentTypes = apiClient.get("/api/v1/paymenttypes", List.class);
      if (paymentTypes != null && !paymentTypes.isEmpty()) {
        List<Map<String, Object>> exportedPaymentTypes = new ArrayList<>();
        for (Map<String, Object> paymentType : paymentTypes) {
          Map<String, Object> paymentTypeData = new LinkedHashMap<>();
          paymentTypeData.put("name", paymentType.get("name"));
          if (paymentType.get("description") != null) {
            paymentTypeData.put("description", paymentType.get("description"));
          }
          paymentTypeData.put("isCashPayment", paymentType.get("isCashPayment"));
          if (paymentType.get("position") != null) {
            paymentTypeData.put("position", paymentType.get("position"));
          }
          exportedPaymentTypes.add(paymentTypeData);
        }
        config.put("paymentTypes", exportedPaymentTypes);
      }

      // 3. Export fund sources
      List<Map<String, Object>> fundSources = apiClient.get("/api/v1/funds", List.class);
      if (fundSources != null && !fundSources.isEmpty()) {
        List<Map<String, Object>> exportedFunds = new ArrayList<>();
        for (Map<String, Object> fund : fundSources) {
          Map<String, Object> fundData = new LinkedHashMap<>();
          fundData.put("name", fund.get("name"));
          if (fund.get("externalId") != null) {
            fundData.put("externalId", fund.get("externalId"));
          }
          exportedFunds.add(fundData);
        }
        config.put("fundSources", exportedFunds);
      }

      // 4. Export financial activity mappings
      List<Map<String, Object>> mappings =
          apiClient.get("/api/v1/financialactivityaccounts", List.class);
      if (mappings != null && !mappings.isEmpty()) {
        List<Map<String, Object>> exportedMappings = new ArrayList<>();
        for (Map<String, Object> mapping : mappings) {
          Map<String, Object> mappingData = new LinkedHashMap<>();
          mappingData.put(
              "financialActivityName",
              mapFinancialActivityFromId((Integer) mapping.get("financialActivityId")));

          // Get GL account code
          if (mapping.get("glAccountId") != null) {
            Long glAccountId = ((Number) mapping.get("glAccountId")).longValue();
            Map<String, Object> glAccount =
                apiClient.get("/api/v1/glaccounts/" + glAccountId, Map.class);
            if (glAccount != null && glAccount.get("glCode") != null) {
              mappingData.put("glAccountCode", glAccount.get("glCode"));
            }
          }

          exportedMappings.add(mappingData);
        }
        config.put("financialActivityMappings", exportedMappings);
      }

    } catch (Exception ex) {
      log.warn("Failed to export accounting foundation data: {}", ex.getMessage());
    }
  }

  private String mapGLAccountTypeFromId(Integer typeId) {
    return switch (typeId) {
      case 1 -> "ASSET";
      case 2 -> "LIABILITY";
      case 3 -> "EQUITY";
      case 4 -> "INCOME";
      case 5 -> "EXPENSE";
      default -> "UNKNOWN";
    };
  }

  private String mapFinancialActivityFromId(Integer activityId) {
    return switch (activityId) {
      case 100 -> "ASSET_TRANSFER";
      case 101 -> "LIABILITY_TRANSFER";
      case 102 -> "CASH_AT_MAINVAULT";
      case 103 -> "CASH_AT_TELLER";
      case 200 -> "OPENING_BALANCES_TRANSFER_CONTRA";
      case 201 -> "ASSET_FUND_SOURCE";
      default -> "UNKNOWN_" + activityId;
    };
  }

  private void exportFinancialProducts(Map<String, Object> config) {
    try {
      // Export charges
      List<Map<String, Object>> charges = apiClient.get("/api/v1/charges", List.class);
      if (charges != null && !charges.isEmpty()) {
        List<Map<String, Object>> exportedCharges = new ArrayList<>();
        for (Map<String, Object> charge : charges) {
          Map<String, Object> chargeData = new LinkedHashMap<>();
          chargeData.put("name", charge.get("name"));
          chargeData.put("currencyCode", charge.get("currency"));
          chargeData.put("amount", charge.get("amount"));
          exportedCharges.add(chargeData);
        }
        config.put("charges", exportedCharges);
      }

      // Export loan products
      List<Map<String, Object>> loanProducts = apiClient.get("/api/v1/loanproducts", List.class);
      if (loanProducts != null && !loanProducts.isEmpty()) {
        List<Map<String, Object>> exportedProducts = new ArrayList<>();
        for (Map<String, Object> product : loanProducts) {
          Map<String, Object> productData = new LinkedHashMap<>();
          productData.put("name", product.get("name"));
          productData.put("shortName", product.get("shortName"));
          productData.put("currencyCode", product.get("currency"));
          exportedProducts.add(productData);
        }
        config.put("loanProducts", exportedProducts);
      }

      // Export savings products
      List<Map<String, Object>> savingsProducts =
          apiClient.get("/api/v1/savingsproducts", List.class);
      if (savingsProducts != null && !savingsProducts.isEmpty()) {
        List<Map<String, Object>> exportedProducts = new ArrayList<>();
        for (Map<String, Object> product : savingsProducts) {
          Map<String, Object> productData = new LinkedHashMap<>();
          productData.put("name", product.get("name"));
          productData.put("shortName", product.get("shortName"));
          productData.put("currencyCode", product.get("currency"));
          exportedProducts.add(productData);
        }
        config.put("savingsProducts", exportedProducts);
      }

    } catch (Exception ex) {
      log.warn("Failed to export financial products data: {}", ex.getMessage());
    }
  }

  private void exportClientOperations(Map<String, Object> config) {
    try {
      // Export clients
      List<Map<String, Object>> clients = apiClient.get("/api/v1/clients", List.class);
      if (clients != null && !clients.isEmpty()) {
        List<Map<String, Object>> exportedClients = new ArrayList<>();
        for (Map<String, Object> client : clients) {
          Map<String, Object> clientData = new LinkedHashMap<>();
          clientData.put("externalId", client.get("externalId"));
          clientData.put("firstName", client.get("firstname"));
          clientData.put("lastName", client.get("lastname"));
          exportedClients.add(clientData);
        }
        config.put("clients", exportedClients);
      }

    } catch (Exception ex) {
      log.warn("Failed to export client operations data: {}", ex.getMessage());
    }
  }

  private List<Integer> parsePhases(String phases) {
    List<Integer> result = new ArrayList<>();
    if ("all".equalsIgnoreCase(phases)) {
      for (int i = 1; i <= 6; i++) {
        result.add(i);
      }
      return result;
    }

    String[] parts = phases.split(",");
    for (String part : parts) {
      try {
        int phase = Integer.parseInt(part.trim());
        if (phase >= 1 && phase <= 6) {
          result.add(phase);
        }
      } catch (NumberFormatException ex) {
        log.warn("Invalid phase number: {}", part);
      }
    }
    return result;
  }

  private String mapAccountUsageFromId(Integer usageId) {
    return switch (usageId) {
      case 1 -> "DETAIL";
      case 2 -> "HEADER";
      default -> "UNKNOWN";
    };
  }

  private void writeYamlFile(Map<String, Object> config, String outputPath, ExportOptions options) {
    try {
      // Configure YAML dumper options
      DumperOptions dumperOptions = new DumperOptions();
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      dumperOptions.setPrettyFlow(options.isPrettyPrint());
      dumperOptions.setIndent(options.isPrettyPrint() ? 2 : 0);

      Yaml yaml = new Yaml(dumperOptions);

      // Write to file
      File outputFile = new File(outputPath);
      outputFile.getParentFile().mkdirs();

      try (FileWriter writer = new FileWriter(outputFile)) {
        if (options.isIncludeComments()) {
          writer.write("# ============================================\n");
          writer.write("# Fineract Configuration Export\n");
          writer.write("# ============================================\n");
          writer.write("# Generated by: Fineract Config CLI\n");
          writer.write("# Export Date: " + java.time.LocalDateTime.now() + "\n");
          writer.write("# Format Version: " + options.getFormatVersion() + "\n");
          writer.write("#\n");
          writer.write("# SECURITY WARNING:\n");
          writer.write("# - User passwords are sanitized (***EXPORTED***)\n");
          writer.write("# - Replace passwords before importing\n");
          writer.write("# - Do not commit sensitive data to version control\n");
          writer.write("# ============================================\n\n");
        } else {
          writer.write("# Fineract Configuration Export\n");
          writer.write("# Generated: " + java.time.LocalDateTime.now() + "\n\n");
        }
        yaml.dump(config, writer);
      }

      log.info("YAML file written to: {}", outputPath);

    } catch (Exception ex) {
      log.error("Failed to write YAML file", ex);
      throw new RuntimeException("Failed to write YAML file: " + ex.getMessage(), ex);
    }
  }
}
