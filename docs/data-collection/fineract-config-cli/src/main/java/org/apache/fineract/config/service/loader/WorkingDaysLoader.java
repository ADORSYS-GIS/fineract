package org.apache.fineract.config.service.loader;

import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.WorkingDaysConfig;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.apache.fineract.config.util.RequestBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for working days configuration.
 *
 * <p>Configures the working days calendar for the Fineract tenant.
 *
 * <p>API Endpoint: PUT /api/v1/workingdays
 */
@Slf4j
@Component
public class WorkingDaysLoader {

  private final FineractApiClient apiClient;

  public WorkingDaysLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads working days configuration.
   *
   * @param workingDaysConfig working days configuration
   * @param context import context
   * @param result import result
   */
  public void load(
      WorkingDaysConfig workingDaysConfig, ImportContext context, ImportResult result) {
    log.debug("Loading working days configuration");

    try {
      // Convert repaymentRescheduleType from string to integer ID using centralized mapper
      Integer repaymentRescheduleTypeId =
          FineractEnumMapper.mapRepaymentRescheduleType(
              workingDaysConfig.getRepaymentRescheduleType());

      // Build request using RequestBuilder utility
      Map<String, Object> request =
          RequestBuilder.forConfig()
              .withLocale() // Required for numeric parameters
              .put("recurrence", workingDaysConfig.getRecurrence())
              .put("repaymentRescheduleType", repaymentRescheduleTypeId)
              .put(
                  "extendTermForDailyRepayments",
                  workingDaysConfig.isExtendTermForDailyRepayments())
              .build();

      // Update working days configuration (system singleton - always an update)
      Map<String, Object> response = apiClient.put("/api/v1/workingdays", request, Map.class);

      log.info(
          "Working days configured: {} (rescheduling: {} -> ID: {})",
          workingDaysConfig.getRecurrence(),
          workingDaysConfig.getRepaymentRescheduleType(),
          repaymentRescheduleTypeId);

      result.recordEntity("workingDays", ImportResult.EntityAction.UPDATED);

    } catch (Exception ex) {
      log.error("Failed to configure working days: {}", ex.getMessage(), ex);
      result.recordEntity("workingDays", ImportResult.EntityAction.FAILED);
      throw ex;
    }
  }
}
