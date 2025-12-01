package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.DataTable;
import org.apache.fineract.config.model.systemconfig.DataTableColumn;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for data tables (custom fields).
 *
 * <p>Creates custom data tables with columns for extending entity data.
 *
 * <p>API Endpoints: - POST /api/v1/datatables - PUT /api/v1/datatables/{tableName}
 */
@Slf4j
@Component
public class DataTableLoader {

  private final FineractApiClient apiClient;

  public DataTableLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads data tables.
   *
   * @param dataTables list of data tables
   * @param context import context
   * @param result import result
   */
  public void load(List<DataTable> dataTables, ImportContext context, ImportResult result) {
    log.debug("Loading {} data tables", dataTables.size());

    for (DataTable dataTable : dataTables) {
      try {
        loadSingleDataTable(dataTable, context, result);
      } catch (Exception ex) {
        log.error("Failed to load data table '{}': {}", dataTable.getName(), ex.getMessage());
        result.recordEntity("dataTable", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Loads a single data table.
   *
   * @param dataTable data table to load
   * @param context import context
   * @param result import result
   */
  private void loadSingleDataTable(
      DataTable dataTable, ImportContext context, ImportResult result) {
    log.debug("Loading data table: {}", dataTable.getName());

    // Get existing data tables
    List<Map<String, Object>> existingTables = apiClient.get("/api/v1/datatables", List.class);

    // Find table by name
    Map<String, Object> existingTable =
        existingTables.stream()
            .filter(t -> dataTable.getName().equals(t.get("registeredTableName")))
            .findFirst()
            .orElse(null);

    if (existingTable == null) {
      // Create new data table
      Map<String, Object> request = buildCreateRequest(dataTable);
      Map<String, Object> response = apiClient.post("/api/v1/datatables", request, Map.class);

      log.info(
          "Data table created: {} (entity: {}, {} columns)",
          dataTable.getName(),
          dataTable.getApptableName(),
          dataTable.getColumns().size());
      result.recordEntity("dataTable", ImportResult.EntityAction.CREATED);
    } else {
      log.debug("Data table already exists: {}", dataTable.getName());
      // Note: Data table updates are complex and may require dropping/recreating
      // For now, we just log that it exists
      result.recordEntity("dataTable", ImportResult.EntityAction.UNCHANGED);
    }
  }

  /**
   * Builds API request for creating a data table.
   *
   * @param dataTable data table
   * @return request map
   */
  private Map<String, Object> buildCreateRequest(DataTable dataTable) {
    Map<String, Object> request = new HashMap<>();

    request.put("datatableName", dataTable.getName());
    request.put(
        "apptableName", dataTable.getApptableName()); // Entity type (m_client, m_loan, etc.)

    if (dataTable.getMultiRow() != null) {
      request.put("multiRow", dataTable.getMultiRow());
    }

    // entitySubType is required for m_client tables (PERSON or ENTITY)
    if (dataTable.getEntitySubType() != null) {
      request.put("entitySubType", dataTable.getEntitySubType());
    } else if ("m_client".equals(dataTable.getApptableName())) {
      // Default to PERSON for client tables if not specified
      request.put("entitySubType", "PERSON");
    }

    // Build columns
    List<Map<String, Object>> columns = new ArrayList<>();
    for (DataTableColumn column : dataTable.getColumns()) {
      Map<String, Object> columnMap = new HashMap<>();
      columnMap.put("name", column.getName());
      columnMap.put("type", column.getType()); // String, Number, Decimal, Date, Dropdown, etc.

      if (column.getLength() != null) {
        columnMap.put("length", column.getLength());
      }

      if (column.getMandatory() != null) {
        columnMap.put("mandatory", column.getMandatory());
      }

      if (column.getCode() != null) {
        // For dropdown columns, reference a code
        columnMap.put("code", column.getCode());
      }

      columns.add(columnMap);
    }

    request.put("columns", columns);

    return request;
  }
}
