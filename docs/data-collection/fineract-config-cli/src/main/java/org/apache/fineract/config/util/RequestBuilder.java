package org.apache.fineract.config.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for building consistent Fineract API request payloads.
 *
 * <p>This class standardizes request construction across all loaders, ensuring consistent handling
 * of common parameters like locale and dateFormat.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>Automatic locale injection for requests with numeric/date parameters
 *   <li>Fluent builder API for readability
 *   <li>Centralized default values
 *   <li>Type-safe parameter handling
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>
 * Map&lt;String, Object&gt; request = RequestBuilder.create()
 *     .withLocale()
 *     .put("name", "My Product")
 *     .put("interestRate", 5.0)
 *     .build();
 * </pre>
 */
public class RequestBuilder {

  /** Default locale used for Fineract API requests. */
  public static final String DEFAULT_LOCALE = "en";

  /** Default date format used for Fineract API requests. */
  public static final String DEFAULT_DATE_FORMAT = "dd MMMM yyyy";

  private final Map<String, Object> request;
  private boolean includeLocale = false;
  private boolean includeDateFormat = false;

  private RequestBuilder() {
    this.request = new HashMap<>();
  }

  /**
   * Creates a new request builder.
   *
   * @return new RequestBuilder instance
   */
  public static RequestBuilder create() {
    return new RequestBuilder();
  }

  /**
   * Adds the default locale parameter to the request.
   *
   * <p>Required by Fineract API for requests with numeric parameters (BigDecimal, Integer) or date
   * parameters.
   *
   * @return this builder for chaining
   */
  public RequestBuilder withLocale() {
    this.includeLocale = true;
    return this;
  }

  /**
   * Adds a custom locale parameter to the request.
   *
   * @param locale the locale string (e.g., "en", "fr", "es")
   * @return this builder for chaining
   */
  public RequestBuilder withLocale(String locale) {
    this.request.put("locale", locale);
    this.includeLocale = false; // Custom locale provided, don't add default
    return this;
  }

  /**
   * Adds the default date format parameter to the request.
   *
   * <p>Required by Fineract API for requests with date parameters.
   *
   * @return this builder for chaining
   */
  public RequestBuilder withDateFormat() {
    this.includeDateFormat = true;
    return this;
  }

  /**
   * Adds a custom date format parameter to the request.
   *
   * @param dateFormat the date format string (e.g., "dd MMMM yyyy", "yyyy-MM-dd")
   * @return this builder for chaining
   */
  public RequestBuilder withDateFormat(String dateFormat) {
    this.request.put("dateFormat", dateFormat);
    this.includeDateFormat = false; // Custom format provided, don't add default
    return this;
  }

  /**
   * Adds locale and date format parameters to the request.
   *
   * <p>Convenience method for requests that require both locale and dateFormat.
   *
   * @return this builder for chaining
   */
  public RequestBuilder withLocaleDateFormat() {
    this.includeLocale = true;
    this.includeDateFormat = true;
    return this;
  }

  /**
   * Adds a parameter to the request.
   *
   * <p>Null values are added to the map as-is. Use {@link #putIfNotNull} to skip null values.
   *
   * @param key parameter name
   * @param value parameter value (can be null)
   * @return this builder for chaining
   */
  public RequestBuilder put(String key, Object value) {
    this.request.put(key, value);
    return this;
  }

  /**
   * Adds a parameter to the request only if the value is not null.
   *
   * <p>This is useful for optional parameters that should be omitted from the request when null.
   *
   * @param key parameter name
   * @param value parameter value (skipped if null)
   * @return this builder for chaining
   */
  public RequestBuilder putIfNotNull(String key, Object value) {
    if (value != null) {
      this.request.put(key, value);
    }
    return this;
  }

  /**
   * Adds a parameter to the request only if the value is not null or empty.
   *
   * <p>For String values, checks both null and empty. For other types, only checks null.
   *
   * @param key parameter name
   * @param value parameter value (skipped if null or empty string)
   * @return this builder for chaining
   */
  public RequestBuilder putIfNotEmpty(String key, Object value) {
    if (value == null) {
      return this;
    }

    if (value instanceof String) {
      String strValue = (String) value;
      if (!strValue.isBlank()) {
        this.request.put(key, value);
      }
    } else {
      this.request.put(key, value);
    }

    return this;
  }

  /**
   * Adds multiple parameters from a Map to the request.
   *
   * <p>This is useful for bulk parameter addition or when combining with other builders.
   *
   * @param parameters map of parameters to add
   * @return this builder for chaining
   */
  public RequestBuilder putAll(Map<String, Object> parameters) {
    this.request.putAll(parameters);
    return this;
  }

  /**
   * Conditionally adds a parameter based on a boolean flag.
   *
   * @param condition if true, the parameter is added
   * @param key parameter name
   * @param value parameter value
   * @return this builder for chaining
   */
  public RequestBuilder putIf(boolean condition, String key, Object value) {
    if (condition) {
      this.request.put(key, value);
    }
    return this;
  }

  /**
   * Builds the final request map with all parameters.
   *
   * <p>Automatically adds locale and/or dateFormat if they were requested via {@link
   * #withLocale()}, {@link #withDateFormat()}, or {@link #withLocaleDateFormat()}.
   *
   * @return the completed request map
   */
  public Map<String, Object> build() {
    if (includeLocale) {
      request.putIfAbsent("locale", DEFAULT_LOCALE);
    }

    if (includeDateFormat) {
      request.putIfAbsent("dateFormat", DEFAULT_DATE_FORMAT);
    }

    return request;
  }

  /**
   * Gets the current size of the request (number of parameters).
   *
   * @return number of parameters in the request
   */
  public int size() {
    return request.size();
  }

  /**
   * Checks if the request contains a specific parameter.
   *
   * @param key parameter name to check
   * @return true if the parameter exists
   */
  public boolean containsKey(String key) {
    return request.containsKey(key);
  }

  /**
   * Removes a parameter from the request.
   *
   * @param key parameter name to remove
   * @return this builder for chaining
   */
  public RequestBuilder remove(String key) {
    request.remove(key);
    return this;
  }

  /**
   * Clears all parameters from the request.
   *
   * @return this builder for chaining
   */
  public RequestBuilder clear() {
    request.clear();
    return this;
  }

  // ===========================================================================================
  // HELPER METHODS FOR COMMON REQUEST PATTERNS
  // ===========================================================================================

  /**
   * Creates a request builder for product creation/update requests.
   *
   * <p>Automatically includes locale as it's required for all product API requests with numeric
   * fields.
   *
   * @return new RequestBuilder with locale enabled
   */
  public static RequestBuilder forProduct() {
    return create().withLocale();
  }

  /**
   * Creates a request builder for account creation/update requests.
   *
   * <p>Automatically includes locale and dateFormat as they're commonly needed for account
   * requests.
   *
   * @return new RequestBuilder with locale and dateFormat enabled
   */
  public static RequestBuilder forAccount() {
    return create().withLocaleDateFormat();
  }

  /**
   * Creates a request builder for transaction requests.
   *
   * <p>Automatically includes locale and dateFormat as they're required for transaction API
   * requests.
   *
   * @return new RequestBuilder with locale and dateFormat enabled
   */
  public static RequestBuilder forTransaction() {
    return create().withLocaleDateFormat();
  }

  /**
   * Creates a request builder for configuration requests.
   *
   * <p>Uses standard request builder without automatic locale (config endpoints vary in
   * requirements).
   *
   * @return new RequestBuilder
   */
  public static RequestBuilder forConfig() {
    return create();
  }
}
