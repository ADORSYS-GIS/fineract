package org.apache.fineract.config.util;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom Jackson deserializer for converting date arrays [year, month, day] to LocalDate.
 *
 * <p>This deserializer handles YAML arrays in the format [2024, 1, 15] and converts them to
 * LocalDate objects. It provides graceful error handling with informative messages.
 *
 * <p>Example YAML:
 *
 * <pre>
 * activationDate: [2024, 1, 15]
 * submittedOnDate: [2024, 1, 10]
 * </pre>
 */
public class DateArrayDeserializer extends JsonDeserializer<LocalDate> {

  /**
   * Deserializes a date array [year, month, day] to LocalDate.
   *
   * @param jsonParser the JSON parser
   * @param deserializationContext the deserialization context
   * @return parsed LocalDate object
   * @throws IOException if deserialization fails
   */
  @Override
  public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    // Handle null values
    if (jsonParser.getCurrentToken() == null) {
      return null;
    }

    try {
      // Parse as list of integers
      @SuppressWarnings("unchecked")
      List<Integer> dateArray = jsonParser.readValueAs(List.class);

      // Validate array length
      if (dateArray == null || dateArray.isEmpty()) {
        return null;
      }

      if (dateArray.size() < 3) {
        throw new IllegalArgumentException(
            "Date array must have at least 3 elements [year, month, day], got: " + dateArray);
      }

      // Extract year, month, day
      Integer year = dateArray.get(0);
      Integer month = dateArray.get(1);
      Integer day = dateArray.get(2);

      // Validate values
      if (year == null || month == null || day == null) {
        throw new IllegalArgumentException(
            "Date array contains null values: [" + year + ", " + month + ", " + day + "]");
      }

      // Create and return LocalDate
      return LocalDate.of(year, month, day);

    } catch (IllegalArgumentException e) {
      throw new IOException("Failed to parse date array: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new IOException(
          "Invalid date format. Expected array [year, month, day], got: " + jsonParser.getText(),
          e);
    }
  }
}
