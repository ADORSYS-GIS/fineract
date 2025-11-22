package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.product.DelinquencyBucket;
import org.apache.fineract.config.model.product.DelinquencyRange;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for delinquency bucket configuration.
 *
 * <p>Creates delinquency ranges and buckets for loan portfolio management.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/delinquency/ranges - List all ranges
 *   <li>POST /api/v1/delinquency/ranges - Create range
 *   <li>GET /api/v1/delinquency/buckets - List all buckets
 *   <li>POST /api/v1/delinquency/buckets - Create bucket
 * </ul>
 */
@Slf4j
@Component
public class DelinquencyBucketLoader {

  private final FineractApiClient apiClient;

  public DelinquencyBucketLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads delinquency bucket configuration.
   *
   * @param buckets list of delinquency buckets
   * @param context import context
   * @param result import result
   */
  public void load(List<DelinquencyBucket> buckets, ImportContext context, ImportResult result) {
    log.info("Loading {} delinquency buckets", buckets.size());

    // Fetch existing ranges and buckets
    Map<String, Long> existingRanges = fetchExistingRanges();
    Map<String, Long> existingBuckets = fetchExistingBuckets();

    for (DelinquencyBucket bucket : buckets) {
      try {
        loadSingleBucket(bucket, existingRanges, existingBuckets, context, result);
      } catch (Exception ex) {
        log.error("Failed to load delinquency bucket '{}': {}", bucket.getName(), ex.getMessage());
        result.recordEntity("delinquencyBucket", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing delinquency ranges from Fineract.
   *
   * @return map of range classification to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingRanges() {
    Map<String, Long> ranges = new HashMap<>();
    try {
      List<Map<String, Object>> rangeList = apiClient.get("/api/v1/delinquency/ranges", List.class);
      for (Map<String, Object> range : rangeList) {
        String classification = (String) range.get("classification");
        Number id = (Number) range.get("id");
        if (classification != null && id != null) {
          ranges.put(classification, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing delinquency ranges: {}", ex.getMessage());
    }
    return ranges;
  }

  /**
   * Fetches existing delinquency buckets from Fineract.
   *
   * @return map of bucket name to ID
   */
  @SuppressWarnings("unchecked")
  private Map<String, Long> fetchExistingBuckets() {
    Map<String, Long> buckets = new HashMap<>();
    try {
      List<Map<String, Object>> bucketList =
          apiClient.get("/api/v1/delinquency/buckets", List.class);
      for (Map<String, Object> bucket : bucketList) {
        String name = (String) bucket.get("name");
        Number id = (Number) bucket.get("id");
        if (name != null && id != null) {
          buckets.put(name, id.longValue());
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing delinquency buckets: {}", ex.getMessage());
    }
    return buckets;
  }

  /**
   * Loads a single delinquency bucket with its ranges.
   *
   * @param bucket bucket configuration
   * @param existingRanges existing ranges map
   * @param existingBuckets existing buckets map
   * @param context import context
   * @param result import result
   */
  private void loadSingleBucket(
      DelinquencyBucket bucket,
      Map<String, Long> existingRanges,
      Map<String, Long> existingBuckets,
      ImportContext context,
      ImportResult result) {

    String name = bucket.getName();
    log.debug("Loading delinquency bucket: {}", name);

    // Check if bucket already exists
    if (existingBuckets.containsKey(name)) {
      Long bucketId = existingBuckets.get(name);
      log.info("Delinquency bucket already exists: {} (ID: {})", name, bucketId);
      result.recordEntity("delinquencyBucket", ImportResult.EntityAction.UNCHANGED);
      context.registerEntity("delinquencyBucket", name, bucketId);
      return;
    }

    // First, ensure all ranges exist
    List<Long> rangeIds = new ArrayList<>();
    List<DelinquencyRange> ranges = bucket.getRanges();

    // If no ranges defined, create a default range based on bucket name
    if (ranges == null || ranges.isEmpty()) {
      DelinquencyRange defaultRange = createDefaultRangeForBucket(name);
      if (defaultRange != null) {
        ranges = List.of(defaultRange);
      }
    }

    if (ranges != null) {
      for (DelinquencyRange range : ranges) {
        Long rangeId = ensureRangeExists(range, existingRanges, context, result);
        if (rangeId != null) {
          rangeIds.add(rangeId);
        }
      }
    }

    // Cannot create bucket without ranges
    if (rangeIds.isEmpty()) {
      log.warn("Cannot create delinquency bucket '{}': no ranges defined", name);
      result.recordEntity("delinquencyBucket", ImportResult.EntityAction.FAILED);
      return;
    }

    // Create bucket
    Map<String, Object> request = new HashMap<>();
    request.put("name", name);
    request.put("locale", "en");
    request.put("ranges", rangeIds);

    Map<String, Object> response =
        apiClient.post("/api/v1/delinquency/buckets", request, Map.class);
    Long bucketId = ((Number) response.get("resourceId")).longValue();

    log.info(
        "Delinquency bucket created: {} (ID: {}) with {} ranges", name, bucketId, rangeIds.size());
    result.recordEntity("delinquencyBucket", ImportResult.EntityAction.CREATED);
    context.registerEntity("delinquencyBucket", name, bucketId);
  }

  /**
   * Creates a default delinquency range based on bucket name.
   *
   * @param bucketName bucket name
   * @return default range or null if cannot infer
   */
  private DelinquencyRange createDefaultRangeForBucket(String bucketName) {
    DelinquencyRange range = new DelinquencyRange();

    // Infer classification and age range from bucket name
    String lowerName = bucketName.toLowerCase();
    if (lowerName.contains("early")) {
      range.setClassification("Early Stage");
      range.setMinAgeDays(1);
      range.setMaxAgeDays(30);
    } else if (lowerName.contains("moderate")) {
      range.setClassification("Moderate");
      range.setMinAgeDays(31);
      range.setMaxAgeDays(60);
    } else if (lowerName.contains("high risk") && !lowerName.contains("very")) {
      range.setClassification("High Risk");
      range.setMinAgeDays(61);
      range.setMaxAgeDays(90);
    } else if (lowerName.contains("very high")) {
      range.setClassification("Very High Risk");
      range.setMinAgeDays(91);
      range.setMaxAgeDays(180);
    } else if (lowerName.contains("default") || lowerName.contains("loss")) {
      range.setClassification("Default");
      range.setMinAgeDays(181);
      range.setMaxAgeDays(null); // Unbounded
    } else {
      // Cannot infer, return null
      log.warn("Cannot infer default range for bucket '{}'", bucketName);
      return null;
    }

    log.debug(
        "Created default range for bucket '{}': {} [{}-{} days]",
        bucketName,
        range.getClassification(),
        range.getMinAgeDays(),
        range.getMaxAgeDays());
    return range;
  }

  /**
   * Ensures a delinquency range exists, creating it if necessary.
   *
   * @param range range configuration
   * @param existingRanges existing ranges map
   * @param context import context
   * @param result import result
   * @return range ID
   */
  private Long ensureRangeExists(
      DelinquencyRange range,
      Map<String, Long> existingRanges,
      ImportContext context,
      ImportResult result) {

    String classification = range.getClassification();

    // Check if already exists
    if (existingRanges.containsKey(classification)) {
      Long rangeId = existingRanges.get(classification);
      log.debug("Delinquency range already exists: {} (ID: {})", classification, rangeId);
      return rangeId;
    }

    // Create range
    Map<String, Object> request = new HashMap<>();
    request.put("classification", classification);
    request.put("locale", "en");

    if (range.getMinAgeDays() != null) {
      request.put("minimumAgeDays", range.getMinAgeDays());
    } else {
      request.put("minimumAgeDays", 0);
    }

    if (range.getMaxAgeDays() != null) {
      request.put("maximumAgeDays", range.getMaxAgeDays());
    }

    try {
      Map<String, Object> response =
          apiClient.post("/api/v1/delinquency/ranges", request, Map.class);
      Long rangeId = ((Number) response.get("resourceId")).longValue();

      log.info(
          "Delinquency range created: {} (ID: {}) [{}-{} days]",
          classification,
          rangeId,
          range.getMinAgeDays(),
          range.getMaxAgeDays());
      result.recordEntity("delinquencyRange", ImportResult.EntityAction.CREATED);
      context.registerEntity("delinquencyRange", classification, rangeId);
      existingRanges.put(classification, rangeId);

      return rangeId;
    } catch (Exception ex) {
      log.error("Failed to create delinquency range '{}': {}", classification, ex.getMessage());
      result.recordEntity("delinquencyRange", ImportResult.EntityAction.FAILED);
      return null;
    }
  }
}
