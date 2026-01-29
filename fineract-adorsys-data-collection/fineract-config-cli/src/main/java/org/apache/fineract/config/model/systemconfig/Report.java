package org.apache.fineract.config.model.systemconfig;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Report {

    private Long id;
    private String reportName;
    private String reportType;
    private String reportSubType;
    private String reportCategory;
    private String description;
    private String reportSql;
    private Boolean coreReport;
    private Boolean useReport;

    private List<ReportParameter> reportParameters = new ArrayList<>();
}
