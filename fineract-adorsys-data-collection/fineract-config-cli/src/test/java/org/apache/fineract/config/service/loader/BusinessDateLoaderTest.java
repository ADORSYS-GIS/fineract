package org.apache.fineract.config.service.loader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.domain.BusinessDate;
import org.apache.fineract.config.provider.FineractApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusinessDateLoaderTest {

  private FineractApiClient fineractApiClient;
  private BusinessDateLoader businessDateLoader;

  @BeforeEach
  void setUp() {
    fineractApiClient = mock(FineractApiClient.class);
    businessDateLoader = new BusinessDateLoader(fineractApiClient);
  }

  @Test
  void shouldCreateBusinessDateWhenNoneExists() {
    BusinessDate businessDate =
        new BusinessDate("BUSINESS_DATE", "12 January 2026", "dd MMMM yyyy", "en");
    when(fineractApiClient.get(eq("/api/v1/businessdate"), eq(List.class)))
        .thenReturn(Collections.emptyList());

    businessDateLoader.load(businessDate);

    verify(fineractApiClient).createBusinessDate(businessDate);
  }

  @Test
  void shouldUpdateBusinessDateWhenExistsAndIsDifferent() {
    BusinessDate businessDate =
        new BusinessDate("BUSINESS_DATE", "12 January 2026", "dd MMMM yyyy", "en");
    Map<String, Object> existingBusinessDate = new HashMap<>();
    existingBusinessDate.put("type", "BUSINESS_DATE");
    existingBusinessDate.put("date", "09 January 2026");
    existingBusinessDate.put("dateFormat", "dd MMMM yyyy");
    existingBusinessDate.put("locale", "en");

    when(fineractApiClient.get(eq("/api/v1/businessdate"), eq(List.class)))
        .thenReturn(Collections.singletonList(existingBusinessDate));

    businessDateLoader.load(businessDate);

    verify(fineractApiClient).createBusinessDate(businessDate);
  }

  @Test
  void shouldNotUpdateBusinessDateWhenExistsAndIsSame() {
    BusinessDate businessDate =
        new BusinessDate("BUSINESS_DATE", "12 January 2026", "dd MMMM yyyy", "en");
    Map<String, Object> existingBusinessDate = new HashMap<>();
    existingBusinessDate.put("type", "BUSINESS_DATE");
    existingBusinessDate.put("date", "12 January 2026");
    existingBusinessDate.put("dateFormat", "dd MMMM yyyy");
    existingBusinessDate.put("locale", "en");

    when(fineractApiClient.get(eq("/api/v1/businessdate"), eq(List.class)))
        .thenReturn(Collections.singletonList(existingBusinessDate));

    businessDateLoader.load(businessDate);

    verify(fineractApiClient, never()).createBusinessDate(any(BusinessDate.class));
  }

  @Test
  void shouldDoNothingWhenBusinessDateIsNull() {
    businessDateLoader.load(null);

    verify(fineractApiClient, never()).createBusinessDate(any(BusinessDate.class));
  }
}
