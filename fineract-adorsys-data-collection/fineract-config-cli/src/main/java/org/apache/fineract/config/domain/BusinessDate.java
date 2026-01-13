package org.apache.fineract.config.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BusinessDate {
  private final String type;
  private final String date;
  private final String dateFormat;
  private final String locale;

  @JsonCreator
  public BusinessDate(
      @JsonProperty("type") String type,
      @JsonProperty("date") String date,
      @JsonProperty("dateFormat") String dateFormat,
      @JsonProperty("locale") String locale) {
    this.type = type;
    this.date = date;
    this.dateFormat = dateFormat;
    this.locale = locale;
  }

  public String getType() {
    return type;
  }

  public String getDate() {
    return date;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public String getLocale() {
    return locale;
  }
}
