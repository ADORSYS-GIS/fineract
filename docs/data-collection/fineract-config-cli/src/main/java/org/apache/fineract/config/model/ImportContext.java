package org.apache.fineract.config.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

/**
 * Context for import execution.
 *
 * <p>Maintains state during import process including entity ID mappings for dependency resolution
 * and managed resource tracking.
 *
 * <p>Cache limits are enforced to prevent unbounded memory growth:
 *
 * <ul>
 *   <li>Entity ID cache: 10,000 entries per entity type
 *   <li>Total cache entries: 100,000 across all types
 *   <li>Custom data: 1,000 entries
 * </ul>
 */
@Data
public class ImportContext {

  /** Maximum entries per entity type cache */
  private static final int MAX_ENTRIES_PER_TYPE = 10_000;

  /** Maximum total cache entries across all types */
  private static final int MAX_TOTAL_ENTRIES = 100_000;

  /** Maximum custom data entries */
  private static final int MAX_CUSTOM_DATA_ENTRIES = 1_000;

  /** Entity ID cache for dependency resolution. Map: entityType -> (identifier -> id) */
  private Map<String, Map<String, Long>> entityIdCache = new ConcurrentHashMap<>();

  /** Managed resources created in this import. Map: entityType -> Set of IDs */
  private Map<String, java.util.Set<String>> managedResources = new ConcurrentHashMap<>();

  /** Custom data storage for cross-loader communication */
  private Map<String, Object> customData = new ConcurrentHashMap<>();

  /** Import configuration */
  private FineractConfig config;

  /** Dry-run mode flag */
  private boolean dryRun = false;

  /** Cache statistics */
  private CacheStatistics cacheStats = new CacheStatistics();

  /**
   * Registers an entity ID for dependency resolution.
   *
   * <p>Enforces cache limits to prevent unbounded memory growth.
   *
   * @param entityType entity type (e.g., "office", "glAccount")
   * @param identifier entity identifier (name, code, externalId)
   * @param id Fineract entity ID
   * @throws IllegalStateException if cache limits exceeded
   */
  public void registerEntity(String entityType, String identifier, Long id) {
    // Check total cache size limit
    int totalEntries = entityIdCache.values().stream().mapToInt(Map::size).sum();
    if (totalEntries >= MAX_TOTAL_ENTRIES) {
      throw new IllegalStateException(
          String.format(
              "Entity cache limit exceeded: %d entries (max: %d). "
                  + "Consider splitting configuration into smaller imports.",
              totalEntries, MAX_TOTAL_ENTRIES));
    }

    // Get or create type cache
    Map<String, Long> typeCache =
        entityIdCache.computeIfAbsent(entityType, k -> new ConcurrentHashMap<>());

    // Check per-type cache size limit
    if (typeCache.size() >= MAX_ENTRIES_PER_TYPE) {
      throw new IllegalStateException(
          String.format(
              "Entity cache limit exceeded for type '%s': %d entries (max: %d). "
                  + "Consider splitting configuration into smaller imports.",
              entityType, typeCache.size(), MAX_ENTRIES_PER_TYPE));
    }

    typeCache.put(identifier, id);
    cacheStats.recordCachePut(entityType);
  }

  /**
   * Gets entity ID by identifier.
   *
   * @param entityType entity type
   * @param identifier entity identifier
   * @return entity ID, or null if not found
   */
  public Long getEntityId(String entityType, String identifier) {
    Map<String, Long> typeCache = entityIdCache.get(entityType);
    Long result = typeCache != null ? typeCache.get(identifier) : null;

    // Record cache hit/miss
    if (result != null) {
      cacheStats.recordCacheHit(entityType);
    } else {
      cacheStats.recordCacheMiss(entityType);
    }

    return result;
  }

  /**
   * Adds a managed resource.
   *
   * @param entityType entity type
   * @param entityId entity ID (as string)
   */
  public void addManagedResource(String entityType, String entityId) {
    managedResources.computeIfAbsent(entityType, k -> ConcurrentHashMap.newKeySet()).add(entityId);
  }

  /**
   * Checks if a resource is managed.
   *
   * @param entityType entity type
   * @param entityId entity ID (as string)
   * @return true if managed, false otherwise
   */
  public boolean isManagedResource(String entityType, String entityId) {
    java.util.Set<String> typeResources = managedResources.get(entityType);
    return typeResources != null && typeResources.contains(entityId);
  }

  /**
   * Gets all managed resources for an entity type.
   *
   * @param entityType entity type
   * @return set of entity IDs
   */
  public java.util.Set<String> getManagedResources(String entityType) {
    return managedResources.getOrDefault(entityType, java.util.Collections.emptySet());
  }

  /**
   * Caches an entity lookup result.
   *
   * @param entityType entity type
   * @param identifier identifier
   * @param id entity ID
   */
  public void cacheEntity(String entityType, String identifier, Long id) {
    registerEntity(entityType, identifier, id);
  }

  /**
   * Resolves an entity ID by identifier (alias for getEntityId).
   *
   * @param entityType entity type
   * @param identifier entity identifier
   * @return entity ID, or null if not found
   */
  public Long resolveEntityId(String entityType, String identifier) {
    return getEntityId(entityType, identifier);
  }

  /**
   * Gets all managed resources (for state persistence).
   *
   * @return map of entity type to set of resource IDs
   */
  public Map<String, java.util.Set<String>> getManagedResources() {
    return managedResources;
  }

  /**
   * Gets custom data for cross-loader communication.
   *
   * @return custom data map
   */
  public Map<String, Object> getCustomData() {
    return customData;
  }

  /**
   * Stores custom data with size limit enforcement.
   *
   * @param key data key
   * @param value data value
   * @throws IllegalStateException if custom data limit exceeded
   */
  public void putCustomData(String key, Object value) {
    if (customData.size() >= MAX_CUSTOM_DATA_ENTRIES && !customData.containsKey(key)) {
      throw new IllegalStateException(
          String.format(
              "Custom data limit exceeded: %d entries (max: %d)",
              customData.size(), MAX_CUSTOM_DATA_ENTRIES));
    }
    customData.put(key, value);
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics
   */
  public CacheStatistics getCacheStatistics() {
    return cacheStats;
  }

  /** Clears all caches (useful for testing). */
  public void clearCaches() {
    entityIdCache.clear();
    managedResources.clear();
    customData.clear();
    cacheStats.reset();
  }

  /**
   * Gets current cache size statistics.
   *
   * @return map of cache metrics
   */
  public Map<String, Integer> getCacheSizeMetrics() {
    Map<String, Integer> metrics = new LinkedHashMap<>();

    int totalEntries = 0;
    for (Map.Entry<String, Map<String, Long>> entry : entityIdCache.entrySet()) {
      int size = entry.getValue().size();
      metrics.put("entityCache." + entry.getKey(), size);
      totalEntries += size;
    }

    metrics.put("entityCache.total", totalEntries);
    metrics.put(
        "managedResources.total",
        managedResources.values().stream().mapToInt(java.util.Set::size).sum());
    metrics.put("customData.total", customData.size());

    return metrics;
  }

  /**
   * Cache statistics tracking.
   *
   * <p>Tracks cache hits, misses, and puts for performance monitoring.
   */
  public static class CacheStatistics {
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> misses = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> puts = new ConcurrentHashMap<>();

    public void recordCacheHit(String entityType) {
      hits.computeIfAbsent(entityType, k -> new AtomicInteger()).incrementAndGet();
    }

    public void recordCacheMiss(String entityType) {
      misses.computeIfAbsent(entityType, k -> new AtomicInteger()).incrementAndGet();
    }

    public void recordCachePut(String entityType) {
      puts.computeIfAbsent(entityType, k -> new AtomicInteger()).incrementAndGet();
    }

    public int getHits(String entityType) {
      AtomicInteger counter = hits.get(entityType);
      return counter != null ? counter.get() : 0;
    }

    public int getMisses(String entityType) {
      AtomicInteger counter = misses.get(entityType);
      return counter != null ? counter.get() : 0;
    }

    public int getPuts(String entityType) {
      AtomicInteger counter = puts.get(entityType);
      return counter != null ? counter.get() : 0;
    }

    public int getTotalHits() {
      return hits.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getTotalMisses() {
      return misses.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getTotalPuts() {
      return puts.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public double getHitRate() {
      int totalHits = getTotalHits();
      int totalRequests = totalHits + getTotalMisses();
      return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    public Map<String, Map<String, Integer>> getDetailedStats() {
      Map<String, Map<String, Integer>> stats = new LinkedHashMap<>();

      for (String entityType : puts.keySet()) {
        Map<String, Integer> typeStats = new LinkedHashMap<>();
        typeStats.put("hits", getHits(entityType));
        typeStats.put("misses", getMisses(entityType));
        typeStats.put("puts", getPuts(entityType));

        int typeHits = getHits(entityType);
        int typeRequests = typeHits + getMisses(entityType);
        double hitRate = typeRequests > 0 ? (double) typeHits / typeRequests : 0.0;
        typeStats.put("hitRatePercent", (int) (hitRate * 100));

        stats.put(entityType, typeStats);
      }

      return stats;
    }

    public void reset() {
      hits.clear();
      misses.clear();
      puts.clear();
    }
  }
}
