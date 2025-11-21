package org.apache.fineract.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Configuration properties for Fineract connection.
 *
 * <p>These properties configure the connection to Apache Fineract instance including URL, tenant,
 * authentication, and SSL settings.
 *
 * <p>Example configuration:
 *
 * <pre>
 * fineract:
 *   url: http://localhost:8080/fineract-provider
 *   tenant: default
 *   user: mifos
 *   password: password
 *   ssl-verify: true
 *   auth:
 *     type: basic
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "fineract")
public class FineractProperties {

  /** Fineract base URL (required). Example: http://localhost:8080/fineract-provider */
  @NotBlank(message = "Fineract URL is required")
  private String url;

  /** Tenant identifier (default: 'default') */
  @NotBlank(message = "Tenant identifier is required")
  private String tenant = "default";

  /** Username for basic authentication */
  private String user = "mifos";

  /** Password for basic authentication */
  private String password;

  /** Enable SSL certificate verification (default: true) */
  private boolean sslVerify = true;

  /** Connection timeout in seconds (default: 30) */
  private int connectionTimeout = 30;

  /** Read timeout in seconds (default: 60) */
  private int readTimeout = 60;

  /** Authentication configuration */
  private AuthProperties auth = new AuthProperties();

  /** Authentication properties */
  @Data
  public static class AuthProperties {
    /** Authentication type: 'basic' or 'oauth2' (default: 'basic') */
    private String type = "basic";

    /** OAuth2 configuration */
    private OAuth2Properties oauth2 = new OAuth2Properties();
  }

  /** OAuth2 authentication properties */
  @Data
  public static class OAuth2Properties {
    /** OAuth2 token URL */
    private String tokenUrl;

    /** OAuth2 client ID */
    private String clientId;

    /** OAuth2 client secret */
    private String clientSecret;

    /** OAuth2 grant type (default: 'password') */
    private String grantType = "password";

    /** OAuth2 scope (optional) */
    private String scope;
  }
}
