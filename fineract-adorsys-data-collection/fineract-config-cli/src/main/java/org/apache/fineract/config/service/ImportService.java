package org.apache.fineract.config.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.fineract.config.model.FineractConfig;
import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.ImportState;
import org.apache.fineract.config.properties.ImportProperties;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.service.loader.AccountNumberPreferenceLoader;
import org.apache.fineract.config.service.loader.BusinessDateLoader;
import org.apache.fineract.config.service.loader.CenterLoader;
import org.apache.fineract.config.service.loader.ChargeLoader;
import org.apache.fineract.config.service.loader.ChartOfAccountsLoader;
import org.apache.fineract.config.service.loader.ClientLoader;
import org.apache.fineract.config.service.loader.CodesLoader;
import org.apache.fineract.config.service.loader.CollateralTypeLoader;
import org.apache.fineract.config.service.loader.CurrencyLoader;
import org.apache.fineract.config.service.loader.DataTableLoader;
import org.apache.fineract.config.service.loader.DelinquencyBucketLoader;
import org.apache.fineract.config.service.loader.FinancialActivityMappingLoader;
import org.apache.fineract.config.service.loader.FloatingRateLoader;
import org.apache.fineract.config.service.loader.FundSourceLoader;
import org.apache.fineract.config.service.loader.GlobalConfigLoader;
import org.apache.fineract.config.service.loader.GroupLoader;
import org.apache.fineract.config.service.loader.GuarantorTypeLoader;
import org.apache.fineract.config.service.loader.HolidayLoader;
import org.apache.fineract.config.service.loader.LoanAccountLoader;
import org.apache.fineract.config.service.loader.LoanCollateralLoader;
import org.apache.fineract.config.service.loader.LoanGuarantorLoader;
import org.apache.fineract.config.service.loader.LoanProductLoader;
import org.apache.fineract.config.service.loader.LoanProvisioningLoader;
import org.apache.fineract.config.service.loader.LoanTransactionLoader;
import org.apache.fineract.config.service.loader.MakerCheckerConfigLoader;
import org.apache.fineract.config.service.loader.NotificationConfigLoader;
import org.apache.fineract.config.service.loader.NotificationTemplateLoader;
import org.apache.fineract.config.service.loader.OfficeLoader;
import org.apache.fineract.config.service.loader.PaymentTypeAccountingMappingLoader;
import org.apache.fineract.config.service.loader.PaymentTypeLoader;
import org.apache.fineract.config.service.loader.RoleLoader;
import org.apache.fineract.config.service.loader.SavingsAccountLoader;
import org.apache.fineract.config.service.loader.SavingsProductLoader;
import org.apache.fineract.config.service.loader.SavingsTransactionLoader;
import org.apache.fineract.config.service.loader.SchedulerJobLoader;
import org.apache.fineract.config.service.loader.StaffLoader;
import org.apache.fineract.config.service.loader.TaxGroupLoader;
import org.apache.fineract.config.service.loader.TellerAccountingRuleLoader;
import org.apache.fineract.config.service.loader.TellerCashierLoader;
import org.apache.fineract.config.service.loader.TellerLoader;
import org.apache.fineract.config.service.loader.UserLoader;
import org.apache.fineract.config.service.loader.WorkingDaysLoader;
import org.apache.fineract.config.util.ChecksumUtil;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Main service for orchestrating configuration import.
 *
 * <p>Executes the import process:
 *
 * <ol>
 *   <li>Parse YAML configuration
 *   <li>Validate configuration
 *   <li>Calculate checksum
 *   <li>Check for changes (compare with previous checksum)
 *   <li>Execute entity loaders in dependency order
 *   <li>Save state
 * </ol>
 */
@Slf4j
@Service
public class ImportService {

  private final YamlParserService yamlParser;
  private final ValidationService validationService;
  private final ChecksumUtil checksumUtil;
  private final ImportProperties importProperties;
  private final FineractApiClient fineractApiClient;
  private final StateService stateService;

  // Entity loaders (Phase 1: System Config)
  private final CurrencyLoader currencyLoader;
  private final WorkingDaysLoader workingDaysLoader;
  private final GlobalConfigLoader globalConfigLoader;
  private final CodesLoader codesLoader;
  private final AccountNumberPreferenceLoader accountNumberPreferenceLoader;
  private final NotificationConfigLoader notificationConfigLoader;
  private final NotificationTemplateLoader notificationTemplateLoader;
  private final DataTableLoader dataTableLoader;
  private final HolidayLoader holidayLoader;
  private final SchedulerJobLoader schedulerJobLoader;
  private final MakerCheckerConfigLoader makerCheckerConfigLoader;

  // Entity loaders (Phase 2: Security & Organization)
  private final OfficeLoader officeLoader;
  private final RoleLoader roleLoader;
  private final UserLoader userLoader;
  private final StaffLoader staffLoader;
  private final TellerLoader tellerLoader;
  private final TellerCashierLoader tellerCashierLoader;

  // Entity loaders (Phase 3: Accounting Foundation)
  private final ChartOfAccountsLoader chartOfAccountsLoader;
  private final FinancialActivityMappingLoader financialActivityMappingLoader;
  private final PaymentTypeLoader paymentTypeLoader;
  private final PaymentTypeAccountingMappingLoader paymentTypeAccountingMappingLoader;
  private final FundSourceLoader fundSourceLoader;
  private final TellerAccountingRuleLoader tellerAccountingRuleLoader;

  // Entity loaders (Phase 4: Financial Products)
  private final ChargeLoader chargeLoader;
  private final LoanProductLoader loanProductLoader;
  private final SavingsProductLoader savingsProductLoader;
  private final FloatingRateLoader floatingRateLoader;
  private final DelinquencyBucketLoader delinquencyBucketLoader;
  private final TaxGroupLoader taxGroupLoader;
  private final CollateralTypeLoader collateralTypeLoader;
  private final GuarantorTypeLoader guarantorTypeLoader;
  private final LoanProvisioningLoader loanProvisioningLoader;

  // Entity loaders (Phase 5: Client Operations & Accounts)
  private final CenterLoader centerLoader;
  private final ClientLoader clientLoader;
  private final GroupLoader groupLoader;
  private final SavingsAccountLoader savingsAccountLoader;
  private final LoanAccountLoader loanAccountLoader;
  private final LoanCollateralLoader loanCollateralLoader;
  private final LoanGuarantorLoader loanGuarantorLoader;

  // Entity loaders (Phase 6: Transactions & Operations)
  private final SavingsTransactionLoader savingsTransactionLoader;
  private final LoanTransactionLoader loanTransactionLoader;
  private final BusinessDateLoader businessDateLoader;

  public ImportService(
      YamlParserService yamlParser,
      ValidationService validationService,
      ChecksumUtil checksumUtil,
      ImportProperties importProperties,
      FineractApiClient fineractApiClient,
      StateService stateService,
      CurrencyLoader currencyLoader,
      WorkingDaysLoader workingDaysLoader,
      GlobalConfigLoader globalConfigLoader,
      CodesLoader codesLoader,
      AccountNumberPreferenceLoader accountNumberPreferenceLoader,
      NotificationConfigLoader notificationConfigLoader,
      NotificationTemplateLoader notificationTemplateLoader,
      DataTableLoader dataTableLoader,
      HolidayLoader holidayLoader,
      SchedulerJobLoader schedulerJobLoader,
      MakerCheckerConfigLoader makerCheckerConfigLoader,
      OfficeLoader officeLoader,
      RoleLoader roleLoader,
      UserLoader userLoader,
      StaffLoader staffLoader,
      TellerLoader tellerLoader,
      TellerCashierLoader tellerCashierLoader,
      ChartOfAccountsLoader chartOfAccountsLoader,
      FinancialActivityMappingLoader financialActivityMappingLoader,
      PaymentTypeLoader paymentTypeLoader,
      PaymentTypeAccountingMappingLoader paymentTypeAccountingMappingLoader,
      FundSourceLoader fundSourceLoader,
      TellerAccountingRuleLoader tellerAccountingRuleLoader,
      ChargeLoader chargeLoader,
      LoanProductLoader loanProductLoader,
      SavingsProductLoader savingsProductLoader,
      FloatingRateLoader floatingRateLoader,
      DelinquencyBucketLoader delinquencyBucketLoader,
      TaxGroupLoader taxGroupLoader,
      CollateralTypeLoader collateralTypeLoader,
      GuarantorTypeLoader guarantorTypeLoader,
      LoanProvisioningLoader loanProvisioningLoader,
      CenterLoader centerLoader,
      ClientLoader clientLoader,
      GroupLoader groupLoader,
      SavingsAccountLoader savingsAccountLoader,
      LoanAccountLoader loanAccountLoader,
      LoanCollateralLoader loanCollateralLoader,
      LoanGuarantorLoader loanGuarantorLoader,
      SavingsTransactionLoader savingsTransactionLoader,
      LoanTransactionLoader loanTransactionLoader,
      BusinessDateLoader businessDateLoader) {
    this.yamlParser = yamlParser;
    this.validationService = validationService;
    this.checksumUtil = checksumUtil;
    this.importProperties = importProperties;
    this.fineractApiClient = fineractApiClient;
    this.stateService = stateService;
    this.currencyLoader = currencyLoader;
    this.workingDaysLoader = workingDaysLoader;
    this.globalConfigLoader = globalConfigLoader;
    this.codesLoader = codesLoader;
    this.accountNumberPreferenceLoader = accountNumberPreferenceLoader;
    this.notificationConfigLoader = notificationConfigLoader;
    this.notificationTemplateLoader = notificationTemplateLoader;
    this.dataTableLoader = dataTableLoader;
    this.holidayLoader = holidayLoader;
    this.schedulerJobLoader = schedulerJobLoader;
    this.makerCheckerConfigLoader = makerCheckerConfigLoader;
    this.officeLoader = officeLoader;
    this.roleLoader = roleLoader;
    this.userLoader = userLoader;
    this.staffLoader = staffLoader;
    this.tellerLoader = tellerLoader;
    this.tellerCashierLoader = tellerCashierLoader;
    this.chartOfAccountsLoader = chartOfAccountsLoader;
    this.financialActivityMappingLoader = financialActivityMappingLoader;
    this.paymentTypeLoader = paymentTypeLoader;
    this.paymentTypeAccountingMappingLoader = paymentTypeAccountingMappingLoader;
    this.fundSourceLoader = fundSourceLoader;
    this.tellerAccountingRuleLoader = tellerAccountingRuleLoader;
    this.chargeLoader = chargeLoader;
    this.loanProductLoader = loanProductLoader;
    this.savingsProductLoader = savingsProductLoader;
    this.floatingRateLoader = floatingRateLoader;
    this.delinquencyBucketLoader = delinquencyBucketLoader;
    this.taxGroupLoader = taxGroupLoader;
    this.collateralTypeLoader = collateralTypeLoader;
    this.guarantorTypeLoader = guarantorTypeLoader;
    this.loanProvisioningLoader = loanProvisioningLoader;
    this.centerLoader = centerLoader;
    this.clientLoader = clientLoader;
    this.groupLoader = groupLoader;
    this.savingsAccountLoader = savingsAccountLoader;
    this.loanAccountLoader = loanAccountLoader;
    this.loanCollateralLoader = loanCollateralLoader;
    this.loanGuarantorLoader = loanGuarantorLoader;
    this.savingsTransactionLoader = savingsTransactionLoader;
    this.loanTransactionLoader = loanTransactionLoader;
    this.businessDateLoader = businessDateLoader;
  }

  /**
   * Executes configuration import.
   *
   * @return import result
   */
  public ImportResult executeImport() {
    boolean dryRun = importProperties.isDryRun();
    ImportResult result = new ImportResult(dryRun);
    result.setStartTime(Instant.now());

    // Generate unique import instance ID for concurrent import detection
    String importInstanceId = UUID.randomUUID().toString();

    try {
      if (dryRun) {
        log.info("========================================");
        log.info("DRY RUN MODE - No changes will be made");
        log.info("========================================");
      } else {
        log.info("========================================");
        log.info("Starting Fineract Configuration Import");
        log.info("Import Instance ID: {}", importInstanceId);
        log.info("========================================");
      }

      // Step 1: Test Fineract connection
      if (!testConnection()) {
        return ImportResult.failure("Cannot connect to Fineract");
      }

      // Step 2: Parse configuration
      log.info("Step 1/6: Parsing configuration files...");
      FineractConfig config = yamlParser.parse();
      log.info("Configuration parsed successfully for tenant: {}", config.getTenant());

      // Step 3: Validate configuration
      if (importProperties.isValidate()) {
        log.info("Step 2/6: Validating configuration...");
        validationService.validate(config);
      } else {
        log.info("Step 2/6: Validation skipped (disabled)");
      }

      // Step 4: Calculate checksum
      log.info("Step 3/6: Calculating checksum...");
      String checksum = calculateChecksum();
      result.setChecksum(checksum);
      log.info("Configuration checksum: {}", checksum);

      // Step 5: Check for changes
      log.info("Step 4/6: Checking for changes...");
      ImportState previousState = stateService.loadState();
      boolean hasChanged = stateService.hasChanged(checksum, previousState);

      if (!hasChanged && !importProperties.isForce()) {
        log.info("No changes detected, skipping import");
        result.setStatus(ImportResult.ImportStatus.SUCCESS);
        result.setMessage("No changes detected");
        return result;
      }

      log.info("Changes detected, proceeding with import");

      // Step 6: Execute loaders
      if (dryRun) {
        log.info("Step 5/6: Analyzing configuration changes (dry-run)...");
      } else {
        log.info("Step 5/6: Executing entity loaders...");
      }
      ImportContext context = new ImportContext();
      context.setConfig(config);

      executeLoaders(config, context, result, dryRun);

      // Step 7: Save state (skip in dry-run mode)
      if (!dryRun) {
        log.info("Step 6/6: Saving import state...");
        ImportState newState = new ImportState();
        newState.setChecksum(checksum);
        newState.setImportInstanceId(importInstanceId);
        newState.setStateCreatedAt(Instant.now());
        newState.setLastImportStatus(ImportResult.ImportStatus.SUCCESS.toString());
        newState.setManagedResources(context.getManagedResources());
        stateService.saveState(newState);
        log.info("State saved successfully");
      } else {
        log.info("Step 6/6: Skipping state save (dry-run mode)");
      }

      // Complete result
      result.complete();
      result.setStatus(ImportResult.ImportStatus.SUCCESS);
      result.setMessage("Import completed successfully");

      // Log cache statistics
      logCacheStatistics(context);

      log.info("========================================");
      log.info("Import Summary:");
      log.info("  Total Entities: {}", result.getTotalEntities());
      log.info("  Created: {}", result.getCreated());
      log.info("  Updated: {}", result.getUpdated());
      log.info("  Unchanged: {}", result.getUnchanged());
      log.info("  Failed: {}", result.getFailed());
      log.info("  Duration: {} ms", result.getDuration().toMillis());
      log.info("========================================");

      return result;

    } catch (org.apache.fineract.config.exception.ConcurrentImportException ex) {
      log.error("Import failed due to concurrent execution: {}", ex.getMessage());
      result.complete();
      result.setStatus(ImportResult.ImportStatus.FAILED);
      result.setMessage("Concurrent import detected: " + ex.getMessage());
      return result;
    } catch (Exception ex) {
      log.error("Import failed: {}", ex.getMessage(), ex);
      result.complete();
      result.setStatus(ImportResult.ImportStatus.FAILED);
      result.setMessage("Import failed: " + ex.getMessage());
      return result;
    }
  }

  /**
   * Tests connection to Fineract.
   *
   * @return true if connection successful, false otherwise
   */
  private boolean testConnection() {
    log.info("Testing connection to Fineract...");
    boolean connected = fineractApiClient.testConnection();

    if (connected) {
      log.info("✓ Connected to Fineract successfully");
    } else {
      log.error("✗ Failed to connect to Fineract");
    }

    return connected;
  }

  /**
   * Calculates checksum of configuration files with support for multiple files, glob patterns, and
   * directories.
   *
   * @return combined checksum of all configuration files
   */
  private String calculateChecksum() {
    String locations = importProperties.getFiles().getLocations();

    // Parse multiple locations (comma-separated, glob patterns, directories)
    List<File> files = parseFileLocations(locations);

    if (files.isEmpty()) {
      log.warn("No configuration files found at: {}", locations);
      return "";
    }

    log.info("Calculating checksum for {} file(s)", files.size());

    // Sort files by absolute path for deterministic ordering
    files.sort(Comparator.comparing(File::getAbsolutePath));

    // Read all file contents
    List<String> fileContents = new ArrayList<>();
    for (File file : files) {
      try {
        String content = yamlParser.readFileContent(file);
        fileContents.add(content);
        log.debug("  - {} ({} bytes)", file.getName(), content.length());
      } catch (Exception ex) {
        log.error("Failed to read file {}: {}", file.getAbsolutePath(), ex.getMessage());
        throw new IllegalStateException("Failed to read configuration file: " + file.getName(), ex);
      }
    }

    // Calculate combined checksum
    String checksum = checksumUtil.calculateCombined(fileContents);
    log.debug("Combined checksum: {}", checksum);

    return checksum;
  }

  /**
   * Parses file locations supporting comma-separated paths, glob patterns, and directories.
   *
   * @param locations file location specification
   * @return list of resolved files
   */
  private List<File> parseFileLocations(String locations) {
    List<File> allFiles = new ArrayList<>();

    // Split by comma for multiple locations
    String[] locationPaths = locations.split(",");

    for (String location : locationPaths) {
      location = location.trim();

      // Check if it's a glob pattern
      if (location.contains("*") || location.contains("?")) {
        allFiles.addAll(expandGlobPattern(location));
      } else {
        File file = new File(location);
        if (file.exists() && file.isFile()) {
          allFiles.add(file);
        } else if (file.isDirectory()) {
          // Add all .yml and .yaml files in directory
          File[] yamlFiles =
              file.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
          if (yamlFiles != null) {
            allFiles.addAll(Arrays.asList(yamlFiles));
          }
        } else {
          log.warn("File not found: {}", location);
        }
      }
    }

    return allFiles;
  }

  /**
   * Expands glob pattern to list of matching files.
   *
   * @param pattern glob pattern (e.g., /config/*.yml)
   * @return list of matching files
   */
  private List<File> expandGlobPattern(String pattern) {
    List<File> matchedFiles = new ArrayList<>();

    try {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

      // Determine base directory from pattern
      Path basePath = determineBasePath(pattern);

      // Walk directory tree and find matches
      Files.walk(basePath, 10)
          .filter(path -> matcher.matches(path))
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .forEach(matchedFiles::add);

    } catch (IOException ex) {
      log.error("Failed to expand glob pattern '{}': {}", pattern, ex.getMessage());
    }

    return matchedFiles;
  }

  /**
   * Determines base directory path from glob pattern.
   *
   * @param pattern glob pattern
   * @return base directory path
   */
  private Path determineBasePath(String pattern) {
    // Extract the directory part before any wildcard characters
    String basePathStr = pattern;
    int wildcardIndex = pattern.indexOf('*');
    if (wildcardIndex > 0) {
      basePathStr = pattern.substring(0, wildcardIndex);
      // Remove trailing path separator if present
      if (basePathStr.endsWith("/") || basePathStr.endsWith(File.separator)) {
        basePathStr = basePathStr.substring(0, basePathStr.length() - 1);
      }
      // Get parent directory
      int lastSeparator =
          Math.max(basePathStr.lastIndexOf('/'), basePathStr.lastIndexOf(File.separator));
      if (lastSeparator > 0) {
        basePathStr = basePathStr.substring(0, lastSeparator);
      }
    }

    File baseFile = new File(basePathStr);
    return baseFile.exists() && baseFile.isDirectory() ? baseFile.toPath() : new File(".").toPath();
  }

  /**
   * Executes entity loaders in dependency order.
   *
   * @param config configuration
   * @param context import context
   * @param result import result
   * @param dryRun dry-run mode flag
   */
  private void executeLoaders(
      FineractConfig config, ImportContext context, ImportResult result, boolean dryRun) {

    // Phase 1: System Configuration
    if (config.getSystemConfig() != null) {
      log.info("Loading Phase 1: System Configuration");

      // 1. Currency
      if (config.getSystemConfig().getCurrency() != null) {
        log.info("  → Loading currency configuration...");
        try {
          currencyLoader.load(config.getSystemConfig().getCurrency(), context, result);
          log.info("  ✓ Currency configuration loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load currency: {}", ex.getMessage());
          result.recordEntity("currency", ImportResult.EntityAction.FAILED);
        }
      }

      // 2. Working Days
      if (config.getSystemConfig().getWorkingDays() != null) {
        log.info("  → Loading working days configuration...");
        try {
          workingDaysLoader.load(config.getSystemConfig().getWorkingDays(), context, result);
          log.info("  ✓ Working days configuration loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load working days: {}", ex.getMessage());
          result.recordEntity("workingDays", ImportResult.EntityAction.FAILED);
        }
      }

      // 3. Global Configuration
      if (config.getSystemConfig().getGlobalConfig() != null
          && !config.getSystemConfig().getGlobalConfig().isEmpty()) {
        log.info("  → Loading global configuration...");
        try {
          globalConfigLoader.load(config.getSystemConfig().getGlobalConfig(), context, result);
          log.info("  ✓ Global configuration loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load global configuration: {}", ex.getMessage());
        }
      }

      // 4. Codes and Code Values
      if (config.getSystemConfig().getCodes() != null
          && !config.getSystemConfig().getCodes().isEmpty()) {
        log.info("  → Loading codes and code values...");
        try {
          codesLoader.load(config.getSystemConfig().getCodes(), context, result);
          log.info("  ✓ Codes and code values loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load codes: {}", ex.getMessage());
        }
      }

      // 5. Account Number Preferences
      if (config.getSystemConfig().getAccountNumberPreferences() != null
          && !config.getSystemConfig().getAccountNumberPreferences().isEmpty()) {
        log.info("  → Loading account number preferences...");
        try {
          accountNumberPreferenceLoader.load(
              config.getSystemConfig().getAccountNumberPreferences(), context, result);
          log.info("  ✓ Account number preferences loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load account number preferences: {}", ex.getMessage());
        }
      }

      // 6. Notification Configuration (SMS/Email)
      if (config.getSystemConfig().getNotificationConfig() != null) {
        log.info("  → Loading notification configuration...");
        try {
          notificationConfigLoader.load(
              config.getSystemConfig().getNotificationConfig(), context, result);
          log.info("  ✓ Notification configuration loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load notification configuration: {}", ex.getMessage());
        }
      }

      // 7. Notification Templates
      if (config.getSystemConfig().getNotificationTemplates() != null
          && !config.getSystemConfig().getNotificationTemplates().isEmpty()) {
        log.info("  → Loading notification templates...");
        try {
          notificationTemplateLoader.load(
              config.getSystemConfig().getNotificationTemplates(), context, result);
          log.info("  ✓ Notification templates loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load notification templates: {}", ex.getMessage());
        }
      }

      // 8. Data Tables
      if (config.getSystemConfig().getDataTables() != null
          && !config.getSystemConfig().getDataTables().isEmpty()) {
        log.info("  → Loading data tables...");
        try {
          dataTableLoader.load(config.getSystemConfig().getDataTables(), context, result);
          log.info("  ✓ Data tables loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load data tables: {}", ex.getMessage());
        }
      }

      // 9. Holidays
      if (config.getSystemConfig().getHolidays() != null
          && !config.getSystemConfig().getHolidays().isEmpty()) {
        log.info("  → Loading holidays...");
        try {
          holidayLoader.load(config.getSystemConfig().getHolidays(), context, result);
          log.info("  ✓ Holidays loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load holidays: {}", ex.getMessage());
        }
      }

      // 10. Scheduler Jobs
      if (config.getSystemConfig().getSchedulerJobs() != null
          && !config.getSystemConfig().getSchedulerJobs().isEmpty()) {
        log.info("  → Loading scheduler jobs...");
        try {
          schedulerJobLoader.load(config.getSystemConfig().getSchedulerJobs(), context, result);
          log.info("  ✓ Scheduler jobs loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load scheduler jobs: {}", ex.getMessage());
        }
      }

      // 11. Maker-Checker Configuration
      if (config.getSystemConfig().getMakerCheckerConfig() != null
          && !config.getSystemConfig().getMakerCheckerConfig().isEmpty()) {
        log.info("  → Loading maker-checker configuration...");
        try {
          makerCheckerConfigLoader.load(
              config.getSystemConfig().getMakerCheckerConfig(), context, result);
          log.info("  ✓ Maker-checker configuration loaded");
        } catch (Exception ex) {
          log.error("  ✗ Failed to load maker-checker configuration: {}", ex.getMessage());
        }
      }
    }

    // Phase 2: Security & Organization
    log.info("Loading Phase 2: Security & Organization");

    // 1. Offices (must be first - other entities depend on it)
    if (config.getOffices() != null && !config.getOffices().isEmpty()) {
      log.info("  → Loading offices...");
      try {
        officeLoader.load((List) config.getOffices(), context, result, dryRun);
        log.info("  ✓ Offices loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load offices: {}", ex.getMessage());
      }
    }

    // 2. Roles (must be before users)
    if (config.getRoles() != null && !config.getRoles().isEmpty()) {
      log.info("  → Loading roles...");
      try {
        roleLoader.load((List) config.getRoles(), context, result);
        log.info("  ✓ Roles loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load roles: {}", ex.getMessage());
      }
    }

    // 3. Staff (depends on offices - must be before users for staff linking)
    if (config.getStaff() != null && !config.getStaff().isEmpty()) {
      log.info("  → Loading staff...");
      try {
        staffLoader.load((List) config.getStaff(), context, result);
        log.info("  ✓ Staff loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load staff: {}", ex.getMessage());
      }
    }

    // 4. Users (depends on offices, roles, and optionally staff)
    if (config.getUsers() != null && !config.getUsers().isEmpty()) {
      log.info("  → Loading users...");
      try {
        userLoader.load((List) config.getUsers(), context, result);
        log.info("  ✓ Users loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load users: {}", ex.getMessage());
      }
    }

    // 5. Tellers (depends on offices and staff)
    if (config.getTellers() != null && !config.getTellers().isEmpty()) {
      log.info("  → Loading tellers...");
      try {
        tellerLoader.load((List) config.getTellers(), context, result);
        log.info("  ✓ Tellers loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load tellers: {}", ex.getMessage());
      }
    }

    // 6. Teller Cashier Mappings (depends on tellers and staff)
    if (config.getTellerCashierMappings() != null && !config.getTellerCashierMappings().isEmpty()) {
      log.info("  → Loading teller cashier mappings...");
      try {
        tellerCashierLoader.load((List) config.getTellerCashierMappings(), context, result);
        log.info("  ✓ Teller cashier mappings loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load teller cashier mappings: {}", ex.getMessage());
      }
    }

    // Phase 3: Accounting Foundation
    log.info("Loading Phase 3: Accounting Foundation");

    // 1. Chart of Accounts (GL Accounts)
    if (config.getChartOfAccounts() != null && !config.getChartOfAccounts().isEmpty()) {
      log.info("  → Loading chart of accounts...");
      try {
        chartOfAccountsLoader.load((List) config.getChartOfAccounts(), context, result);
        log.info("  ✓ Chart of accounts loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load chart of accounts: {}", ex.getMessage());
      }
    }

    // 2. Payment Types
    if (config.getPaymentTypes() != null && !config.getPaymentTypes().isEmpty()) {
      log.info("  → Loading payment types...");
      try {
        paymentTypeLoader.load((List) config.getPaymentTypes(), context, result);
        log.info("  ✓ Payment types loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load payment types: {}", ex.getMessage());
      }
    }

    // 3. Fund Sources
    if (config.getFundSources() != null && !config.getFundSources().isEmpty()) {
      log.info("  → Loading fund sources...");
      try {
        fundSourceLoader.load((List) config.getFundSources(), context, result);
        log.info("  ✓ Fund sources loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load fund sources: {}", ex.getMessage());
      }
    }

    // 4. Financial Activity Mappings (depends on GL accounts)
    if (config.getFinancialActivityMappings() != null
        && !config.getFinancialActivityMappings().isEmpty()) {
      log.info("  → Loading financial activity mappings...");
      try {
        financialActivityMappingLoader.load(
            (List) config.getFinancialActivityMappings(), context, result);
        log.info("  ✓ Financial activity mappings loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load financial activity mappings: {}", ex.getMessage());
      }
    }

    // 5. Payment Type Accounting Mappings (depends on payment types and GL accounts)
    if (config.getPaymentTypeAccountingMappings() != null
        && !config.getPaymentTypeAccountingMappings().isEmpty()) {
      log.info("  → Loading payment type accounting mappings...");
      try {
        paymentTypeAccountingMappingLoader.load(
            (List) config.getPaymentTypeAccountingMappings(), context, result);
        log.info("  ✓ Payment type accounting mappings loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load payment type accounting mappings: {}", ex.getMessage());
      }
    }

    // 6. Teller Accounting Rules (depends on tellers and GL accounts)
    if (config.getTellerAccountingRules() != null && !config.getTellerAccountingRules().isEmpty()) {
      log.info("  → Loading teller accounting rules...");
      try {
        tellerAccountingRuleLoader.load((List) config.getTellerAccountingRules(), context, result);
        log.info("  ✓ Teller accounting rules loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load teller accounting rules: {}", ex.getMessage());
      }
    }

    // Phase 4: Financial Products
    log.info("Loading Phase 4: Financial Products");

    // 1. Floating Rates (must be before loan products)
    if (config.getFloatingRates() != null && !config.getFloatingRates().isEmpty()) {
      log.info("  → Loading floating rates...");
      try {
        floatingRateLoader.load((List) config.getFloatingRates(), context, result);
        log.info("  ✓ Floating rates loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load floating rates: {}", ex.getMessage());
      }
    }

    // 2. Delinquency Buckets (must be before loan products)
    if (config.getDelinquencyBuckets() != null && !config.getDelinquencyBuckets().isEmpty()) {
      log.info("  → Loading delinquency buckets...");
      try {
        delinquencyBucketLoader.load((List) config.getDelinquencyBuckets(), context, result);
        log.info("  ✓ Delinquency buckets loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load delinquency buckets: {}", ex.getMessage());
      }
    }

    // 3. Tax Groups (for products with tax)
    if (config.getTaxGroups() != null && !config.getTaxGroups().isEmpty()) {
      log.info("  → Loading tax groups...");
      try {
        taxGroupLoader.load((List) config.getTaxGroups(), context, result);
        log.info("  ✓ Tax groups loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load tax groups: {}", ex.getMessage());
      }
    }

    // 4. Collateral Types (for loan collateral management)
    if (config.getCollateralTypes() != null && !config.getCollateralTypes().isEmpty()) {
      log.info("  → Loading collateral types...");
      try {
        collateralTypeLoader.load((List) config.getCollateralTypes(), context, result);
        log.info("  ✓ Collateral types loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load collateral types: {}", ex.getMessage());
      }
    }

    // 4b. Guarantor Types (for loan guarantor management)
    if (config.getGuarantorTypes() != null && !config.getGuarantorTypes().isEmpty()) {
      log.info("  → Loading guarantor types...");
      try {
        guarantorTypeLoader.load((List) config.getGuarantorTypes(), context, result);
        log.info("  ✓ Guarantor types loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load guarantor types: {}", ex.getMessage());
      }
    }

    // 4c. Loan Provisioning (for portfolio risk management)
    if (config.getLoanProvisioning() != null && !config.getLoanProvisioning().isEmpty()) {
      log.info("  → Loading loan provisioning criteria...");
      try {
        loanProvisioningLoader.load((List) config.getLoanProvisioning(), context, result);
        log.info("  ✓ Loan provisioning criteria loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan provisioning criteria: {}", ex.getMessage());
      }
    }

    // 5. Charges (depends on GL accounts)
    if (config.getCharges() != null && !config.getCharges().isEmpty()) {
      log.info("  → Loading charges...");
      try {
        chargeLoader.load((List) config.getCharges(), context, result);
        log.info("  ✓ Charges loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load charges: {}", ex.getMessage());
      }
    }

    // 6. Loan Products (depends on charges, GL accounts, fund sources, floating rates, delinquency)
    if (config.getLoanProducts() != null && !config.getLoanProducts().isEmpty()) {
      log.info("  → Loading loan products...");
      try {
        loanProductLoader.load((List) config.getLoanProducts(), context, result);
        log.info("  ✓ Loan products loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan products: {}", ex.getMessage());
      }
    }

    // 7. Savings Products (depends on charges, GL accounts, tax groups)
    if (config.getSavingsProducts() != null && !config.getSavingsProducts().isEmpty()) {
      log.info("  → Loading savings products...");
      try {
        savingsProductLoader.load((List) config.getSavingsProducts(), context, result);
        log.info("  ✓ Savings products loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load savings products: {}", ex.getMessage());
      }
    }

    // Phase 5: Client Operations & Accounts
    log.info("Loading Phase 5: Client Operations & Accounts");

    // 1. Centers (must be first - groups can belong to centers)
    if (config.getCenters() != null && !config.getCenters().isEmpty()) {
      log.info("  → Loading centers...");
      try {
        centerLoader.load((List) config.getCenters(), context, result);
        log.info("  ✓ Centers loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load centers: {}", ex.getMessage());
      }
    }

    // 2. Clients (individual borrowers/savers)
    if (config.getClients() != null && !config.getClients().isEmpty()) {
      log.info("  → Loading clients...");
      try {
        clientLoader.load((List) config.getClients(), context, result);
        log.info("  ✓ Clients loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load clients: {}", ex.getMessage());
      }
    }

    // 3. Groups (depends on clients and centers)
    if (config.getGroups() != null && !config.getGroups().isEmpty()) {
      log.info("  → Loading groups...");
      try {
        groupLoader.load((List) config.getGroups(), context, result);
        log.info("  ✓ Groups loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load groups: {}", ex.getMessage());
      }
    }

    // 4. Savings Accounts (depends on clients/groups and savings products)
    if (config.getSavingsAccounts() != null && !config.getSavingsAccounts().isEmpty()) {
      log.info("  → Loading savings accounts...");
      try {
        savingsAccountLoader.load((List) config.getSavingsAccounts(), context, result);
        log.info("  ✓ Savings accounts loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load savings accounts: {}", ex.getMessage());
      }
    }

    // 5. Loan Accounts (depends on clients/groups and loan products)
    if (config.getLoanAccounts() != null && !config.getLoanAccounts().isEmpty()) {
      log.info("  → Loading loan accounts...");
      try {
        loanAccountLoader.load((List) config.getLoanAccounts(), context, result);
        log.info("  ✓ Loan accounts loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan accounts: {}", ex.getMessage());
      }
    }

    // 6. Loan Collaterals (depends on loan accounts and collateral types)
    if (config.getLoanCollaterals() != null && !config.getLoanCollaterals().isEmpty()) {
      log.info("  → Loading loan collaterals...");
      try {
        loanCollateralLoader.load((List) config.getLoanCollaterals(), context, result);
        log.info("  ✓ Loan collaterals loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan collaterals: {}", ex.getMessage());
      }
    }

    // 7. Loan Guarantors (depends on loan accounts and clients/staff)
    if (config.getLoanGuarantors() != null && !config.getLoanGuarantors().isEmpty()) {
      log.info("  → Loading loan guarantors...");
      try {
        loanGuarantorLoader.load((List) config.getLoanGuarantors(), context, result);
        log.info("  ✓ Loan guarantors loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan guarantors: {}", ex.getMessage());
      }
    }

    // Phase 6: Transactions & Operations
    log.info("Loading Phase 6: Transactions & Operations");

    // 1. Savings Transactions (depends on savings accounts)
    if (config.getSavingsTransactions() != null && !config.getSavingsTransactions().isEmpty()) {
      log.info("  → Loading savings transactions...");
      try {
        savingsTransactionLoader.load((List) config.getSavingsTransactions(), context, result);
        log.info("  ✓ Savings transactions loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load savings transactions: {}", ex.getMessage());
      }
    }

    // 2. Loan Transactions (depends on loan accounts)
    if (config.getLoanTransactions() != null && !config.getLoanTransactions().isEmpty()) {
      log.info("  → Loading loan transactions...");
      try {
        loanTransactionLoader.load((List) config.getLoanTransactions(), context, result);
        log.info("  ✓ Loan transactions loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load loan transactions: {}", ex.getMessage());
      }
    }

    // 3. Business Date (should be last to ensure all transactions are processed)
    if (config.getBusinessDate() != null) {
      log.info("  → Loading business date...");
      try {
        businessDateLoader.load(config.getBusinessDate());
        log.info("  ✓ Business date loaded");
      } catch (Exception ex) {
        log.error("  ✗ Failed to load business date: {}", ex.getMessage());
      }
    }
  }

  /**
   * Logs cache statistics for monitoring and debugging.
   *
   * @param context import context
   */
  private void logCacheStatistics(ImportContext context) {
    log.info("========================================");
    log.info("Cache Statistics:");

    // Overall statistics
    ImportContext.CacheStatistics stats = context.getCacheStatistics();
    int totalHits = stats.getTotalHits();
    int totalMisses = stats.getTotalMisses();
    int totalPuts = stats.getTotalPuts();
    double hitRate = stats.getHitRate();

    log.info("  Total Cache Operations:");
    log.info("    Puts: {}", totalPuts);
    log.info("    Hits: {}", totalHits);
    log.info("    Misses: {}", totalMisses);
    log.info("    Hit Rate: {}", String.format("%.1f%%", hitRate * 100));

    // Per-type statistics
    Map<String, Map<String, Integer>> detailedStats = stats.getDetailedStats();
    if (!detailedStats.isEmpty()) {
      log.info("  Per-Entity Type:");
      for (Map.Entry<String, Map<String, Integer>> entry : detailedStats.entrySet()) {
        String entityType = entry.getKey();
        Map<String, Integer> typeStats = entry.getValue();
        log.info(
            "    {}: {} puts, {} hits, {} misses ({}% hit rate)",
            entityType,
            typeStats.get("puts"),
            typeStats.get("hits"),
            typeStats.get("misses"),
            typeStats.get("hitRatePercent"));
      }
    }

    // Cache size metrics
    Map<String, Integer> sizeMetrics = context.getCacheSizeMetrics();
    log.info("  Cache Sizes:");
    log.info("    Total Entity Cache Entries: {}", sizeMetrics.get("entityCache.total"));
    log.info("    Managed Resources: {}", sizeMetrics.get("managedResources.total"));
    log.info("    Custom Data Entries: {}", sizeMetrics.get("customData.total"));

    // Memory efficiency check
    int totalEntries = sizeMetrics.get("entityCache.total");
    if (totalEntries > 50000) {
      log.warn(
          "  ⚠ Large cache size detected ({} entries). Consider splitting configuration.",
          totalEntries);
    }

    log.info("========================================");
  }
}
