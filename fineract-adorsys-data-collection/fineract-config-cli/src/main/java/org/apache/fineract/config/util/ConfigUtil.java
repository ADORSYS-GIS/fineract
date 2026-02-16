package org.apache.fineract.config.util;

/** Utility methods for configuration loading. */
public class ConfigUtil {

  /**
   * Normalizes a string for comparison by removing non-alphanumeric characters and converting to
   * lowercase.
   *
   * @param value value to normalize
   * @return normalized value
   */
  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
  }
}
