package org.apache.fineract.config.model.systemconfig;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/** Custom data table. */
@Data
public class DataTable {
  private String name;

  @JsonAlias({"appTableName", "apptableName"})
  private String apptableName; // Entity type (m_client, m_loan, etc.)

  private Boolean multiRow;
  private List<DataTableColumn> columns = new ArrayList<>();
}
