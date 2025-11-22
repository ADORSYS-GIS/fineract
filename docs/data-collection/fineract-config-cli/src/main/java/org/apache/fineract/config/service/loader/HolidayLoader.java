package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.Holiday;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.FineractEnumMapper;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for holidays configuration.
 *
 * <p>Creates holidays for offices with repayment rescheduling rules.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>POST /api/v1/holidays - Create holiday
 *   <li>POST /api/v1/holidays/{id}?command=activate - Activate holiday
 * </ul>
 */
@Slf4j
@Component
public class HolidayLoader {

  private final FineractApiClient apiClient;

  public HolidayLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads holiday configuration.
   *
   * @param holidays list of holidays
   * @param context import context
   * @param result import result
   */
  public void load(List<Holiday> holidays, ImportContext context, ImportResult result) {
    log.info("Loading {} holidays", holidays.size());

    // Fetch existing holidays for idempotency check
    List<Map<String, Object>> existingHolidays = fetchExistingHolidays();
    Map<String, Long> holidayNameToId = buildHolidayNameMap(existingHolidays);

    for (Holiday holiday : holidays) {
      try {
        loadSingleHoliday(holiday, holidayNameToId, context, result);
      } catch (Exception ex) {
        log.error("Failed to load holiday '{}': {}", holiday.getName(), ex.getMessage());
        result.recordEntity("holiday", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing holidays from Fineract.
   *
   * @return list of existing holidays
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> fetchExistingHolidays() {
    try {
      // Need officeId parameter to see holidays
      return apiClient.get("/api/v1/holidays?officeId=1&status=all", List.class);
    } catch (Exception ex) {
      log.warn("Could not fetch existing holidays: {}", ex.getMessage());
      return List.of();
    }
  }

  /**
   * Builds a map of holiday names to IDs.
   *
   * @param holidays list of holidays
   * @return map of name -> ID
   */
  private Map<String, Long> buildHolidayNameMap(List<Map<String, Object>> holidays) {
    Map<String, Long> map = new HashMap<>();
    for (Map<String, Object> holiday : holidays) {
      String name = (String) holiday.get("name");
      Number id = (Number) holiday.get("id");
      if (name != null && id != null) {
        map.put(name, id.longValue());
      }
    }
    return map;
  }

  /**
   * Loads a single holiday.
   *
   * @param holiday holiday to load
   * @param existingHolidays map of existing holiday names to IDs
   * @param context import context
   * @param result import result
   */
  private void loadSingleHoliday(
      Holiday holiday,
      Map<String, Long> existingHolidays,
      ImportContext context,
      ImportResult result) {

    String name = holiday.getName();
    log.debug("Loading holiday: {}", name);

    // Check if holiday already exists
    if (existingHolidays.containsKey(name)) {
      Long holidayId = existingHolidays.get(name);
      log.info("Holiday already exists: {} (ID: {})", name, holidayId);
      result.recordEntity("holiday", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("holiday", name, holidayId);
      return;
    }

    // Build request
    Map<String, Object> request = buildCreateRequest(holiday, context);

    // Create holiday
    Map<String, Object> response = apiClient.post("/api/v1/holidays", request, Map.class);
    Long holidayId = ((Number) response.get("resourceId")).longValue();

    log.info("Holiday created: {} (ID: {})", name, holidayId);
    result.recordEntity("holiday", ImportResult.EntityAction.CREATED);
    context.registerEntity("holiday", name, holidayId);

    // Activate holiday (holidays are created in pending state)
    try {
      activateHoliday(holidayId, name);
    } catch (Exception ex) {
      log.warn("Could not activate holiday '{}': {}", name, ex.getMessage());
    }
  }

  /**
   * Builds the API request for creating a holiday.
   *
   * @param holiday holiday configuration
   * @param context import context
   * @return request map
   */
  private Map<String, Object> buildCreateRequest(Holiday holiday, ImportContext context) {
    Map<String, Object> request = new HashMap<>();

    request.put("name", holiday.getName());
    request.put("locale", "en");
    request.put("dateFormat", "yyyy-MM-dd");

    if (holiday.getDescription() != null) {
      request.put("description", holiday.getDescription());
    }

    // Handle dates (support both string and array format)
    request.put("fromDate", formatDate(holiday.getFromDate()));
    request.put(
        "toDate",
        formatDate(holiday.getToDate() != null ? holiday.getToDate() : holiday.getFromDate()));

    // Map rescheduling rule to integer ID
    Integer reschedulingType =
        FineractEnumMapper.mapHolidayReschedulingRule(holiday.getRepaymentSchedulingRule());
    request.put("reschedulingType", reschedulingType);

    // If rescheduling to specific date, provide the date
    if (reschedulingType == 2 && holiday.getRepaymentsRescheduledTo() != null) {
      request.put("repaymentsRescheduledTo", formatDate(holiday.getRepaymentsRescheduledTo()));
    }

    // Resolve office IDs from office names
    List<Long> officeIds = resolveOfficeIds(holiday.getOfficeNames(), context);
    if (officeIds.isEmpty()) {
      // Default to head office (ID 1) if no offices specified
      officeIds = List.of(1L);
    }
    // API requires array of objects with officeId field
    List<Map<String, Object>> offices = new ArrayList<>();
    for (Long officeId : officeIds) {
      Map<String, Object> office = new HashMap<>();
      office.put("officeId", officeId);
      offices.add(office);
    }
    request.put("offices", offices);

    return request;
  }

  /**
   * Formats a date for the Fineract API.
   *
   * <p>Handles both string format ("2024-01-01") and array format [2024, 1, 1].
   *
   * @param date date object (String or List)
   * @return formatted date string
   */
  @SuppressWarnings("unchecked")
  private String formatDate(Object date) {
    if (date == null) {
      return null;
    }

    if (date instanceof String) {
      return (String) date;
    }

    if (date instanceof List) {
      List<Integer> parts = (List<Integer>) date;
      if (parts.size() >= 3) {
        return String.format("%04d-%02d-%02d", parts.get(0), parts.get(1), parts.get(2));
      }
    }

    return date.toString();
  }

  /**
   * Resolves office names to office IDs.
   *
   * @param officeNames list of office names
   * @param context import context with cached office IDs
   * @return list of office IDs
   */
  private List<Long> resolveOfficeIds(List<String> officeNames, ImportContext context) {
    if (officeNames == null || officeNames.isEmpty()) {
      return List.of();
    }

    List<Long> officeIds = new ArrayList<>();
    for (String officeName : officeNames) {
      Long officeId = context.getEntityId("office", officeName);
      if (officeId != null) {
        officeIds.add(officeId);
      } else {
        // Try to find office by name via API
        Long resolvedId = resolveOfficeIdByName(officeName);
        if (resolvedId != null) {
          officeIds.add(resolvedId);
          context.registerEntity("office", officeName, resolvedId);
        } else {
          log.warn("Could not resolve office '{}' for holiday", officeName);
        }
      }
    }

    return officeIds;
  }

  /**
   * Resolves an office ID by name via API lookup.
   *
   * @param officeName office name
   * @return office ID or null if not found
   */
  @SuppressWarnings("unchecked")
  private Long resolveOfficeIdByName(String officeName) {
    try {
      List<Map<String, Object>> offices = apiClient.get("/api/v1/offices", List.class);
      for (Map<String, Object> office : offices) {
        if (officeName.equals(office.get("name"))) {
          return ((Number) office.get("id")).longValue();
        }
      }
    } catch (Exception ex) {
      log.debug("Could not look up office '{}': {}", officeName, ex.getMessage());
    }
    return null;
  }

  /**
   * Activates a holiday (holidays are created in pending state).
   *
   * @param holidayId holiday ID
   * @param name holiday name (for logging)
   */
  private void activateHoliday(Long holidayId, String name) {
    try {
      apiClient.post("/api/v1/holidays/" + holidayId + "?command=activate", Map.of(), Map.class);
      log.debug("Holiday activated: {} (ID: {})", name, holidayId);
    } catch (Exception ex) {
      log.warn("Could not activate holiday '{}' (ID: {}): {}", name, holidayId, ex.getMessage());
    }
  }
}
