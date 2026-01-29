package org.apache.fineract.config.model.systemconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportParameter {

    private Long id;
    private Long parameterId;
    private Long reportId;
    private String parameterName;
    private String reportParameterName;
}
