package org.apache.fineract.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import lombok.extern.slf4j.Slf4j;

/**
 * Main application class for Fineract Config CLI.
 *
 * <p>This is a Spring Boot CLI application that enables declarative configuration management for
 * Apache Fineract. It follows the Configuration-as-Code pattern inspired by keycloak-config-cli.
 *
 * <p>Key Features:
 *
 * <ul>
 *   <li>Interactive CLI with Spring Shell
 *   <li>Idempotent operations (safe to run multiple times)
 *   <li>Checksum-based change detection
 *   <li>Remote state management
 *   <li>Dependency resolution
 *   <li>Support for 40+ entity types
 *   <li>Export functionality
 *   <li>Dry-run mode
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>
 * # Start interactive shell
 * java -jar fineract-config-cli.jar
 *
 * # Available commands:
 * import --file config.yml                 # Import configuration
 * import --file config.yml --dry-run       # Preview changes
 * validate --file config.yml               # Validate configuration
 * export --output exported.yml             # Export configuration
 * version                                  # Show version
 * help                                     # Show all commands
 * </pre>
 *
 * @see <a href="https://github.com/adorsys/keycloak-config-cli">keycloak-config-cli</a>
 * @author Fineract Config CLI Team
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class FineractConfigCliApplication {

  /**
   * Application entry point.
   *
   * <p>Starts the Spring Shell interactive CLI.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    log.info("Starting Fineract Config CLI...");
    log.info("Type 'help' to see available commands");
    SpringApplication.run(FineractConfigCliApplication.class, args);
  }
}
