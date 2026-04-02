package org.apache.fineract.config.service.loader;

import java.util.List;
import java.util.Map;

import org.apache.fineract.config.domain.BusinessDate;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BusinessDateLoader {

  private final FineractApiClient fineractApiClient;

  @Autowired
  public BusinessDateLoader(FineractApiClient fineractApiClient) {
    this.fineractApiClient = fineractApiClient;
  }

  public void load(BusinessDate businessDate) {
    if (businessDate != null) {
      List<Map<String, Object>> existingBusinessDates =
          fineractApiClient.get("/api/v1/businessdate", List.class);
      if (existingBusinessDates != null && !existingBusinessDates.isEmpty()) {
        Map<String, Object> existingBusinessDate = existingBusinessDates.get(0);
        if (!isEqual(businessDate, existingBusinessDate)) {
          fineractApiClient.createBusinessDate(businessDate);
        }
      } else {
        fineractApiClient.createBusinessDate(businessDate);
      }
    }
  }

  private boolean isEqual(BusinessDate businessDate, Map<String, Object> existingBusinessDate) {
    return businessDate.getType().equals(existingBusinessDate.get("type"))
        && businessDate.getDate().equals(existingBusinessDate.get("date"))
        && businessDate.getDateFormat().equals(existingBusinessDate.get("dateFormat"))
        && businessDate.getLocale().equals(existingBusinessDate.get("locale"));
  }
}
