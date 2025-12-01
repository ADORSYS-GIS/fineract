package org.apache.fineract.config.cli;

import java.io.File;

import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.properties.ImportProperties;
import org.apache.fineract.config.service.ExportService;
import org.apache.fineract.config.service.ImportService;
import org.apache.fineract.config.service.ValidationService;
import org.apache.fineract.config.service.YamlParserService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Shell commands for Fineract Config CLI.
 *
 * <p>Provides command-line interface for import, export, and validation operations.
 */
@Slf4j
@ShellComponent
public class FineractConfigCommands {

  private final ImportService importService;
  private final ExportService exportService;
  private final ValidationService validationService;
  private final YamlParserService yamlParserService;
  private final ImportProperties importProperties;

  public FineractConfigCommands(
      ImportService importService,
      ExportService exportService,
      ValidationService validationService,
      YamlParserService yamlParserService,
      ImportProperties importProperties) {
    this.importService = importService;
    this.exportService = exportService;
    this.validationService = validationService;
    this.yamlParserService = yamlParserService;
    this.importProperties = importProperties;
  }

  /**
   * Import configuration from YAML file.
   *
   * @param file YAML configuration file path
   * @param dryRun whether to perform a dry run (preview changes without applying)
   * @param force force import even if no changes detected
   * @return result message
   */
  @ShellMethod(key = "import", value = "Import configuration from YAML file")
  public String importConfig(
      @ShellOption(help = "Path to YAML configuration file") String file,
      @ShellOption(help = "Perform dry run without making changes", defaultValue = "false")
          boolean dryRun,
      @ShellOption(help = "Force import even if no changes", defaultValue = "false")
          boolean force) {

    try {
      log.info("Starting import from file: {}", file);

      // Validate file exists
      File configFile = new File(file);
      if (!configFile.exists()) {
        return "❌ Error: File not found: " + file;
      }

      // Set properties
      importProperties.getFiles().setLocations(file);
      importProperties.setForce(force);
      importProperties.setDryRun(dryRun);

      if (dryRun) {
        log.info("🔍 DRY RUN MODE - No changes will be made");
      }

      // Execute import
      ImportResult result = importService.executeImport();

      // Format result
      return formatImportResult(result, dryRun);

    } catch (Exception ex) {
      log.error("Import failed", ex);
      return "❌ Import failed: " + ex.getMessage();
    }
  }

  /**
   * Validate YAML configuration file without importing.
   *
   * @param file YAML configuration file path
   * @return validation result message
   */
  @ShellMethod(key = "validate", value = "Validate YAML configuration file")
  public String validate(@ShellOption(help = "Path to YAML configuration file") String file) {

    try {
      log.info("Validating file: {}", file);

      // Validate file exists
      File configFile = new File(file);
      if (!configFile.exists()) {
        return "❌ Error: File not found: " + file;
      }

      // Set properties
      importProperties.getFiles().setLocations(file);

      // Parse YAML
      var config = yamlParserService.parse();

      // Validate
      validationService.validate(config);

      return "✅ Configuration is valid!\n"
          + "   Tenant: "
          + config.getTenant()
          + "\n"
          + "   Total entities defined: "
          + countEntities(config);

    } catch (Exception ex) {
      log.error("Validation failed", ex);
      return "❌ Validation failed: " + ex.getMessage();
    }
  }

  /**
   * Export Fineract configuration to YAML file.
   *
   * @param output output file path
   * @param phases comma-separated list of phases to export (e.g., "1,2,3" or "all")
   * @return result message
   */
  @ShellMethod(key = "export", value = "Export Fineract configuration to YAML file")
  public String export(
      @ShellOption(help = "Output YAML file path") String output,
      @ShellOption(help = "Phases to export (1-6 or 'all')", defaultValue = "all") String phases) {

    try {
      log.info("Exporting configuration to: {}", output);

      // Execute export
      exportService.exportConfiguration(output, phases);

      return "✅ Configuration exported successfully to: " + output;

    } catch (Exception ex) {
      log.error("Export failed", ex);
      return "❌ Export failed: " + ex.getMessage();
    }
  }

  /**
   * Display version information.
   *
   * @return version info
   */
  @ShellMethod(key = "version", value = "Display version information")
  public String version() {
    return "Fineract Config CLI v1.0.0-SNAPSHOT\n"
        + "Apache Fineract Configuration Management Tool\n"
        + "Spring Boot 3.2.1 | Java 17";
  }

  /**
   * Format import result for display.
   *
   * @param result import result
   * @param dryRun whether this was a dry run
   * @return formatted message
   */
  private String formatImportResult(ImportResult result, boolean dryRun) {
    StringBuilder sb = new StringBuilder();

    if (dryRun) {
      sb.append("🔍 DRY RUN COMPLETED\n");
      sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    } else {
      sb.append(result.getStatus() == ImportResult.ImportStatus.SUCCESS ? "✅ " : "❌ ");
      sb.append("IMPORT COMPLETED\n");
      sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    sb.append("Status: ").append(result.getStatus()).append("\n");
    sb.append("Checksum: ").append(result.getChecksum()).append("\n");
    sb.append("Duration: ").append(result.getDuration().toMillis()).append(" ms\n\n");

    sb.append("Entity Summary:\n");
    sb.append("  Total:     ").append(result.getTotalEntities()).append("\n");
    sb.append("  Created:   ").append(result.getCreated()).append("\n");
    sb.append("  Updated:   ").append(result.getUpdated()).append("\n");
    sb.append("  Unchanged: ").append(result.getUnchanged()).append("\n");
    sb.append("  Failed:    ").append(result.getFailed()).append("\n");

    if (dryRun && !result.getPlannedChanges().isEmpty()) {
      sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
      sb.append("Planned Changes:\n");
      sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

      // Group changes by type
      var changesSummary = result.getPlannedChangesSummary();
      changesSummary.forEach(
          (type, count) -> {
            String icon =
                switch (type) {
                  case CREATE -> "➕";
                  case UPDATE -> "📝";
                  case NO_CHANGE -> "✓";
                  case DELETE -> "🗑️";
                  case WOULD_FAIL -> "❌";
                };
            sb.append(String.format("%s %s: %d\n", icon, type, count));
          });

      sb.append("\nDetailed Changes:\n");
      sb.append("─────────────────────────────────────────\n");

      // Show first 20 planned changes
      int maxToShow = 20;
      int shown = 0;

      for (var change : result.getPlannedChanges()) {
        if (shown >= maxToShow) {
          int remaining = result.getPlannedChanges().size() - shown;
          sb.append(String.format("\n... and %d more changes\n", remaining));
          break;
        }

        sb.append("\n").append(change.getSummary()).append("\n");

        // Show changed fields for updates
        if (change.getChangeType()
                == org.apache.fineract.config.model.PlannedChange.ChangeType.UPDATE
            && change.getChangedFields() != null
            && !change.getChangedFields().isEmpty()) {
          change
              .getChangedFields()
              .values()
              .forEach(
                  fc -> {
                    sb.append("    ").append(fc.getFieldName()).append(": ");
                    sb.append(fc.getOldValue()).append(" → ").append(fc.getNewValue()).append("\n");
                  });
        }

        shown++;
      }

      sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
      sb.append("💡 This was a dry run. No changes were made.\n");
      sb.append("   Run without --dry-run to apply these changes.\n");
    } else if (dryRun) {
      sb.append("\n💡 This was a dry run. No changes were made.\n");
      sb.append("   Run without --dry-run to apply changes.\n");
    }

    return sb.toString();
  }

  /**
   * Count total entities in configuration.
   *
   * @param config configuration
   * @return entity count
   */
  private int countEntities(org.apache.fineract.config.model.FineractConfig config) {
    int count = 0;

    if (config.getOffices() != null) count += config.getOffices().size();
    if (config.getRoles() != null) count += config.getRoles().size();
    if (config.getUsers() != null) count += config.getUsers().size();
    if (config.getChartOfAccounts() != null) count += config.getChartOfAccounts().size();
    if (config.getCharges() != null) count += config.getCharges().size();
    if (config.getLoanProducts() != null) count += config.getLoanProducts().size();
    if (config.getSavingsProducts() != null) count += config.getSavingsProducts().size();
    if (config.getClients() != null) count += config.getClients().size();
    if (config.getGroups() != null) count += config.getGroups().size();
    if (config.getCenters() != null) count += config.getCenters().size();
    if (config.getSavingsAccounts() != null) count += config.getSavingsAccounts().size();
    if (config.getLoanAccounts() != null) count += config.getLoanAccounts().size();
    if (config.getSavingsTransactions() != null) {
      count += config.getSavingsTransactions().size();
    }
    if (config.getLoanTransactions() != null) count += config.getLoanTransactions().size();

    return count;
  }
}
