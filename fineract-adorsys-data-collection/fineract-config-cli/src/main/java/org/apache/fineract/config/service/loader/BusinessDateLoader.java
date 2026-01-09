package org.apache.fineract.config.service.loader;

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
      fineractApiClient.createBusinessDate(businessDate);
    }
  }
}
