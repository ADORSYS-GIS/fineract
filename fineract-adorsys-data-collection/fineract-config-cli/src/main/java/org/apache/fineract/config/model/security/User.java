package org.apache.fineract.config.model.security;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * User account configuration.
 *
 * <p>Defines a system user with authentication credentials, role assignments, and office/staff
 * linkages.
 *
 * <p>API Endpoint: POST /api/v1/users
 */
@Slf4j
@Data
public class User {
  /** Unique username (immutable after creation). */
  @JsonProperty("username")
  private String username;

  /** User's first name. */
  @JsonProperty("firstname")
  private String firstname;

  /** User's last name. */
  @JsonProperty("lastname")
  private String lastname;

  /** User's email address. */
  @JsonProperty("email")
  private String email;

  /**
   * User's password (will be hashed by Fineract).
   *
   * <p>Best practice: Use environment variables or secret files, not hardcoded values.
   *
   * <p>Example: ${env:USER_PASSWORD} or ${file:/run/secrets/user_password}
   */
  @JsonProperty("password")
  private String password;

  /**
   * Password confirmation (must match password).
   *
   * <p>Required for user creation but not for updates.
   */
  @JsonProperty("repeatPassword")
  private String repeatPassword;

  /** Whether password should never expire. Defaults to false. */
  @JsonProperty("passwordNeverExpires")
  private Boolean passwordNeverExpires;

  /** Reference to office by name (resolved to officeId at runtime). */
  @JsonProperty("officeName")
  private String officeName;

  /** List of role names assigned to this user (resolved to role IDs at runtime). */
  @JsonProperty("roles")
  private List<String> roles = new ArrayList<>();

  /** Whether this is a self-service user (for client portal access). Defaults to false. */
  @JsonProperty("isSelfServiceUser")
  private Boolean isSelfServiceUser;

  /**
   * Reference to staff member by name (optional, for users who are also staff).
   *
   * <p>Links this user account to a staff record. Typically used for loan officers and branch
   * managers.
   *
   * <p>Example: "Paul Mbida" (resolved to staff ID at runtime)
   */
  @JsonProperty("staffName")
  private String staffName;

  /**
   * Whether to send password to user's email after creation. Defaults to false.
   *
   * <p>Security note: Not recommended for production use.
   */
  @JsonProperty("sendPasswordToEmail")
  private Boolean sendPasswordToEmail;

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
