package org.apache.fineract.config.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportState;
import org.apache.fineract.config.properties.ImportProperties;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing import state.
 *
 * <p>Stores import state in Fineract as a custom data table to enable:
 *
 * <ul>
 *   <li>Checksum-based change detection (skip imports if config unchanged)
 *   <li>Managed resource tracking (delete resources not in config when in 'full' mode)
 *   <li>Import history and auditing
 * </ul>
 *
 * <p>State is stored per tenant and configuration source.
 */
@Slf4j
@Service
public class StateService {

  private static final String STATE_TABLE_NAME = "fineract_config_state";
  private static final String STATE_APP_TABLE = "m_office"; // Attach to office table (tenant-wide)

  private final FineractApiClient apiClient;
  private final ImportProperties importProperties;
  private final ObjectMapper objectMapper;

  public StateService(
      FineractApiClient apiClient, ImportProperties importProperties, ObjectMapper objectMapper) {
    this.apiClient = apiClient;
    this.importProperties = importProperties;
    this.objectMapper = objectMapper;
  }

  /**
   * Loads the previous import state.
   *
   * @return previous state, or empty state if none exists
   */
  public ImportState loadState() {
    log.debug("Loading import state from Fineract");

    try {
      // Check if state table exists
      if (!stateTableExists()) {
        log.info("State table does not exist, will create on first save");
        return new ImportState();
      }

      // Get state for head office (office ID 1)
      String stateJson = getStateJson();
      if (stateJson == null || stateJson.isEmpty()) {
        log.info("No previous import state found");
        return new ImportState();
      }

      ImportState state = objectMapper.readValue(stateJson, ImportState.class);
      log.info(
          "Loaded previous import state: checksum={}, lastImport={}",
          state.getChecksum(),
          state.getLastImportTime());
      return state;

    } catch (Exception ex) {
      log.warn("Failed to load import state, starting fresh: {}", ex.getMessage());
      return new ImportState();
    }
  }

  /**
   * Saves the import state.
   *
   * @param state state to save
   */
  public void saveState(ImportState state) {
    log.debug("Saving import state to Fineract");

    try {
      // Ensure state table exists
      if (!stateTableExists()) {
        createStateTable();
      }

      // Load current state for version check and concurrent import detection
      ImportState currentState = loadState();

      // Check for concurrent imports
      if (currentState.getImportInstanceId() != null
          && !currentState.getImportInstanceId().equals(state.getImportInstanceId())) {

        java.time.Duration timeSinceLastImport =
            java.time.Duration.between(currentState.getLastImportTime(), Instant.now());

        // If another import finished within last 5 minutes, log warning
        if (timeSinceLastImport.toMinutes() < 5) {
          log.warn(
              "Another import (ID: {}) completed {} seconds ago. "
                  + "Potential concurrent import detected.",
              currentState.getImportInstanceId(),
              timeSinceLastImport.getSeconds());
        }
      }

      // Increment version for optimistic locking
      Long currentVersion = currentState.getVersion() != null ? currentState.getVersion() : 0L;
      state.setVersion(currentVersion + 1);
      state.setLastImportTime(Instant.now());
      state.setStateUpdatedAt(Instant.now());

      // Set state created timestamp if not already set
      if (state.getStateCreatedAt() == null) {
        state.setStateCreatedAt(
            currentState.getStateCreatedAt() != null
                ? currentState.getStateCreatedAt()
                : Instant.now());
      }

      // Serialize state to JSON
      String stateJson = objectMapper.writeValueAsString(state);

      // Save or update state with version check
      saveStateJson(stateJson, currentVersion);

      log.info(
          "Import state saved: checksum={}, version={}, instanceId={}",
          state.getChecksum(),
          state.getVersion(),
          state.getImportInstanceId());

    } catch (org.apache.fineract.config.exception.ConcurrentImportException ex) {
      log.error("Concurrent import detected: {}", ex.getMessage());
      throw ex;
    } catch (Exception ex) {
      log.error("Failed to save import state: {}", ex.getMessage(), ex);
      // Non-critical error, continue
    }
  }

  /**
   * Checks if configuration has changed since last import.
   *
   * @param currentChecksum current configuration checksum
   * @param previousState previous import state
   * @return true if changed
   */
  public boolean hasChanged(String currentChecksum, ImportState previousState) {
    if (previousState == null || previousState.getChecksum() == null) {
      log.info("No previous checksum, treating as changed");
      return true;
    }

    boolean changed = !currentChecksum.equals(previousState.getChecksum());
    log.info(
        "Configuration change check: previous={}, current={}, changed={}",
        previousState.getChecksum(),
        currentChecksum,
        changed);

    return changed;
  }

  /**
   * Checks if state table exists.
   *
   * @return true if exists
   */
  private boolean stateTableExists() {
    try {
      List<Map<String, Object>> dataTables = apiClient.get("/api/v1/datatables", List.class);
      return dataTables.stream()
          .anyMatch(dt -> STATE_TABLE_NAME.equals(dt.get("registeredTableName")));
    } catch (Exception ex) {
      log.warn("Failed to check state table existence: {}", ex.getMessage());
      return false;
    }
  }

  /** Creates the state table. */
  private void createStateTable() {
    log.info("Creating state table: {}", STATE_TABLE_NAME);

    Map<String, Object> request = new HashMap<>();
    request.put("datatableName", STATE_TABLE_NAME);
    request.put("apptableName", STATE_APP_TABLE);
    request.put("multiRow", false);

    // Single column to store state as JSON
    Map<String, Object> column = new HashMap<>();
    column.put("name", "state_json");
    column.put("type", "Text");
    column.put("mandatory", true);
    request.put("columns", List.of(column));

    apiClient.post("/api/v1/datatables", request, Map.class);
    log.info("State table created successfully");
  }

  /**
   * Gets state JSON from Fineract.
   *
   * @return state JSON, or null if not exists
   */
  private String getStateJson() {
    try {
      // Get state for office ID 1 (head office)
      // Note: Fineract 1.x DataTable entry URL is /datatables/{table}/{entityId}
      Object response =
          apiClient.get("/api/v1/datatables/" + STATE_TABLE_NAME + "/1", Object.class);

      if (response instanceof List) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response;
        if (!rows.isEmpty()) {
          return (String) rows.get(0).get("state_json");
        }
      } else if (response instanceof Map) {
        Map<String, Object> responseMap = (Map<String, Object>) response;
        // Some Fineract versions return a wrapper with a 'data' array
        if (responseMap.containsKey("data")) {
          List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
          if (data != null && !data.isEmpty()) {
            return (String) data.get(0).get("state_json");
          }
        } else if (responseMap.containsKey("state_json")) {
          // Direct object
          return (String) responseMap.get("state_json");
        }
      }
    } catch (Exception ex) {
      log.debug("State not found or error retrieving: {}", ex.getMessage());
    }
    return null;
  }

  /**
   * Saves state JSON to Fineract with optimistic locking.
   *
   * @param stateJson state JSON to save
   * @param expectedVersion expected version number (for optimistic locking)
   * @throws JsonProcessingException if JSON processing fails
   * @throws org.apache.fineract.config.exception.ConcurrentImportException if version mismatch
   *     detected
   */
  private void saveStateJson(String stateJson, Long expectedVersion)
      throws JsonProcessingException {
    // Read current state to verify version hasn't changed
    String currentStateJson = getStateJson();
    if (currentStateJson != null && !currentStateJson.isEmpty()) {
      ImportState currentState = objectMapper.readValue(currentStateJson, ImportState.class);

      // Check for version mismatch (someone else updated the state)
      if (currentState.getVersion() != null && !currentState.getVersion().equals(expectedVersion)) {
        throw new org.apache.fineract.config.exception.ConcurrentImportException(
            String.format(
                "State version mismatch detected. Expected version %d but found %d. "
                    + "Another import may have run concurrently.",
                expectedVersion, currentState.getVersion()));
      }
    }

    Map<String, Object> request = new HashMap<>();
    request.put("state_json", stateJson);
    request.put("dateFormat", "yyyy-MM-dd");
    request.put("locale", "en");

    try {
      // Try to update existing state
      // Note: Fineract 1.x DataTable entry URL is /datatables/{table}/{entityId}
      apiClient.put("/api/v1/datatables/" + STATE_TABLE_NAME + "/1", request, Map.class);
      log.debug("State updated with version {}", expectedVersion + 1);
    } catch (Exception ex) {
      // If update fails, create new state entry
      log.debug("State update failed, creating new entry");
      // For single-row tables, we POST to the table resource with the entityId in the path or body
      // depending on version, but /datatables/{table}/{entityId} is common for creation if it
      // doesn't exist.
      apiClient.post("/api/v1/datatables/" + STATE_TABLE_NAME + "/1", request, Map.class);
      log.debug("State created");
    }
  }
}
