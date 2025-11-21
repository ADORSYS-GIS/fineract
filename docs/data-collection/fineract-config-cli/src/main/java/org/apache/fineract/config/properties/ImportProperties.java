package org.apache.fineract.config.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Configuration properties for import behavior.
 *
 * <p>These properties control how the configuration import process behaves including file
 * locations, validation, state management, and managed resource modes.
 *
 * <p>Example configuration:
 *
 * <pre>
 * import:
 *   files:
 *     locations: /config/*.yml
 *   validate: true
 *   parallel: false
 *   remote-state:
 *     enabled: true
 *   managed:
 *     office: full
 *     client: no-delete
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "import")
public class ImportProperties {

  /** File configuration */
  private FilesProperties files = new FilesProperties();

  /** Enable validation before import (default: true) */
  private boolean validate = true;

  /** Force import even if checksum unchanged (default: false) */
  private boolean force = false;

  /** Dry run mode - preview changes without applying (default: false) */
  private boolean dryRun = false;

  /** Enable parallel processing for independent entities (default: false) */
  private boolean parallel = false;

  /** Remote state management configuration */
  private RemoteStateProperties remoteState = new RemoteStateProperties();

  /** Variable substitution configuration */
  private VarSubstitutionProperties varSubstitution = new VarSubstitutionProperties();

  /**
   * Managed resource modes per entity type.
   *
   * <p>Values: 'full' (delete unmanaged resources) or 'no-delete' (preserve unmanaged resources)
   *
   * <p>Example:
   *
   * <pre>
   * managed:
   *   office: full
   *   staff: full
   *   client: no-delete
   *   loanProduct: full
   * </pre>
   */
  private Map<String, ManagedResourceMode> managed = new HashMap<>();

  /** Parallel processing configuration */
  private ParallelProperties parallelConfig = new ParallelProperties();

  /** File location properties */
  @Data
  public static class FilesProperties {
    /**
     * Path(s) to configuration files. Supports glob patterns.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>Single file: /config/demo.yml
     *   <li>Multiple files: /config/system.yml,/config/products.yml
     *   <li>Glob pattern: /config/*.yml
     * </ul>
     */
    @NotBlank(message = "Import file location is required")
    private String locations = "/config/*.yml";
  }

  /** Remote state management properties */
  @Data
  public static class RemoteStateProperties {
    /** Enable remote state tracking in Fineract database (default: true) */
    private boolean enabled = true;

    /** Encryption key for state storage (optional) */
    private String encryptionKey;

    /**
     * Checksum behavior when mismatch detected: 'fail' (abort) or 'continue' (re-import) Default:
     * continue
     */
    private String checksumBehavior = "continue";
  }

  /** Variable substitution properties */
  @Data
  public static class VarSubstitutionProperties {
    /** Enable variable substitution (default: true) */
    private boolean enabled = true;

    /** Variable prefix (default: ${) */
    private String prefix = "${";

    /** Variable suffix (default: }) */
    private String suffix = "}";
  }

  /** Parallel processing properties */
  @Data
  public static class ParallelProperties {
    /** Thread pool size for parallel imports (default: 4) */
    private int threadPoolSize = 4;

    /** Maximum wait time in seconds (default: 300) */
    private int maxWaitSeconds = 300;
  }

  /** Managed resource mode enum */
  public enum ManagedResourceMode {
    /** Full management - create, update, delete unmanaged resources */
    FULL("full"),

    /** No-delete mode - create, update, but preserve unmanaged resources */
    NO_DELETE("no-delete");

    private final String value;

    ManagedResourceMode(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static ManagedResourceMode fromValue(String value) {
      for (ManagedResourceMode mode : values()) {
        if (mode.value.equalsIgnoreCase(value)) {
          return mode;
        }
      }
      throw new IllegalArgumentException("Invalid managed resource mode: " + value);
    }
  }
}
