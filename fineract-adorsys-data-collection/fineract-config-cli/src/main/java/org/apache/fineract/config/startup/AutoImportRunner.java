package org.apache.fineract.config.startup;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.properties.ImportProperties;
import org.apache.fineract.config.service.ImportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Automatically imports YAML configuration files on application startup. This component runs before
 * the Spring Shell interactive prompt starts, allowing the application to be used as a one-time
 * import job or as an interactive CLI tool.
 *
 * <p>Behavior:
 *
 * <ul>
 *   <li>Auto-detects YAML files based on configured glob pattern (default: /config/*.yml)
 *   <li>Executes import if files are found
 *   <li>Logs detailed results
 *   <li>Optionally exits after successful import (useful for init containers)
 *   <li>On failure: logs error and continues to shell for troubleshooting
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AutoImportRunner implements ApplicationRunner {

  private final ImportService importService;
  private final ImportProperties importProperties;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // Check if auto-import is enabled
    if (!importProperties.getStartup().isAutoImportEnabled()) {
      log.info("Auto-import is disabled. Skipping startup import.");
      return;
    }

    log.info("=== Fineract Config CLI Auto-Import ===");
    log.info("Checking for configuration files...");

    // Resolve YAML files from configured location
    List<File> configFiles = resolveConfigFiles();

    if (configFiles.isEmpty()) {
      log.info("No configuration files found. Skipping auto-import.");
      log.info("Place YAML files in /config directory to enable automatic import.");
      return;
    }

    log.info("Found {} configuration file(s):", configFiles.size());
    configFiles.forEach(file -> log.info("  - {}", file.getAbsolutePath()));

    try {
      log.info("Starting automatic import...");

      // Execute the import
      ImportResult result = importService.executeImport();

      // Log results
      logImportResults(result);

      // Handle success
      if (result.getStatus() == ImportResult.ImportStatus.SUCCESS) {
        log.info("✓ Auto-import completed successfully!");

        if (importProperties.getStartup().isExitAfterImport()) {
          log.info("Exiting after successful import (exit-after-import=true)");
          System.exit(0);
        } else {
          log.info("Continuing to interactive shell...");
        }
      } else {
        handleImportFailure(result);
      }

    } catch (Exception e) {
      handleImportException(e);
    }
  }

  /**
   * Resolves configuration files from the configured glob pattern.
   *
   * @return List of files matching the pattern
   */
  private List<File> resolveConfigFiles() {
    List<File> files = new ArrayList<>();
    String locations = importProperties.getFiles().getLocations();

    if (locations == null || locations.isEmpty()) {
      log.warn("No file locations configured for import");
      return files;
    }

    // Support multiple locations separated by comma
    String[] locationArray = locations.split(",");

    for (String location : locationArray) {
      location = location.trim();

      File locationFile = new File(location);

      // Direct file reference
      if (locationFile.exists() && locationFile.isFile()) {
        files.add(locationFile);
        continue;
      }

      // Directory with glob pattern
      if (location.contains("*") || location.contains("?")) {
        files.addAll(resolveGlobPattern(location));
      } else if (locationFile.isDirectory()) {
        // Directory - look for all .yml and .yaml files
        File[] yamlFiles =
            locationFile.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (yamlFiles != null) {
          files.addAll(List.of(yamlFiles));
        }
      }
    }

    return files;
  }

  /**
   * Resolves files matching a glob pattern.
   *
   * @param pattern Glob pattern (e.g., /config/*.yml)
   * @return List of matching files
   */
  private List<File> resolveGlobPattern(String pattern) {
    List<File> files = new ArrayList<>();

    try {
      // Extract directory and pattern
      int lastSlash = pattern.lastIndexOf('/');
      String dir = lastSlash > 0 ? pattern.substring(0, lastSlash) : ".";
      String filePattern = pattern.substring(lastSlash + 1);

      File directory = new File(dir);
      if (!directory.exists() || !directory.isDirectory()) {
        return files;
      }

      // Create path matcher for glob pattern
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);

      File[] matchingFiles = directory.listFiles((dir1, name) -> matcher.matches(Paths.get(name)));

      if (matchingFiles != null) {
        files.addAll(List.of(matchingFiles));
      }

    } catch (Exception e) {
      log.error("Error resolving glob pattern '{}': {}", pattern, e.getMessage());
    }

    return files;
  }

  /**
   * Logs detailed import results.
   *
   * @param result Import result
   */
  private void logImportResults(ImportResult result) {
    log.info("=== Import Results ===");
    log.info("Status: {}", result.getStatus());
    log.info("Total entities: {}", result.getTotalEntities());
    log.info("Created: {}", result.getCreated());
    log.info("Updated: {}", result.getUpdated());
    log.info("Unchanged: {}", result.getUnchanged());
    log.info("Failed: {}", result.getFailed());

    if (result.getDuration() != null) {
      log.info("Duration: {}ms", result.getDuration().toMillis());
    }

    // Log entity statistics
    if (result.getEntityStats() != null && !result.getEntityStats().isEmpty()) {
      log.info("Entity Statistics:");
      result
          .getEntityStats()
          .forEach(
              (entityType, stats) -> {
                log.info(
                    "  {}: {} total ({} created, {} updated, {} unchanged, {} failed)",
                    entityType,
                    stats.getTotal(),
                    stats.getCreated(),
                    stats.getUpdated(),
                    stats.getUnchanged(),
                    stats.getFailed());
              });
    }

    // Log planned changes if in dry-run mode
    if (result.isDryRun() && !result.getPlannedChanges().isEmpty()) {
      log.info("Planned Changes (Dry Run):");
      result
          .getPlannedChangesSummary()
          .forEach((changeType, count) -> log.info("  {}: {}", changeType, count));
    }
  }

  /**
   * Handles import failure based on configuration.
   *
   * @param result Import result
   */
  private void handleImportFailure(ImportResult result) {
    log.error("✗ Auto-import failed with status: {}", result.getStatus());

    if (importProperties.getStartup().isFailOnError()) {
      log.error("Exiting with error code (fail-on-error=true)");
      System.exit(1);
    } else {
      log.info("Continuing to interactive shell for troubleshooting (fail-on-error=false)");
      log.info("You can run 'import --file /config/yourfile.yml' to retry manually");
    }
  }

  /**
   * Handles exceptions during import.
   *
   * @param e Exception that occurred
   */
  private void handleImportException(Exception e) {
    log.error("✗ Auto-import failed with exception: {}", e.getMessage(), e);

    if (importProperties.getStartup().isFailOnError()) {
      log.error("Exiting with error code (fail-on-error=true)");
      System.exit(1);
    } else {
      log.info("Continuing to interactive shell for troubleshooting (fail-on-error=false)");
      log.info("You can run 'import --file /config/yourfile.yml' to retry manually");
    }
  }
}
