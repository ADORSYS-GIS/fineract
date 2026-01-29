package org.apache.fineract.config.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.Report;
import org.apache.fineract.config.model.systemconfig.ReportParameter;
import org.apache.fineract.config.provider.FineractApiClient;
import org.apache.fineract.config.util.InputValidator;
import org.apache.fineract.config.util.InputValidator.ValidationResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for Fineract Reports.
 *
 * <p>
 * Registers custom reports (Pentaho, Table, etc.) via the API.
 *
 * <p>
 * API Endpoints: - GET /api/v1/reports - POST /api/v1/reports - PUT
 * /api/v1/reports/{id}
 */
@Slf4j
@Component
public class ReportLoader {

    private final FineractApiClient apiClient;
    private final InputValidator inputValidator;

    public ReportLoader(FineractApiClient apiClient, InputValidator inputValidator) {
        this.apiClient = apiClient;
        this.inputValidator = inputValidator;
    }

    /**
     * Loads reports configuration.
     *
     * @param reports list of reports to load
     * @param context import context
     * @param result  import result
     */
    public void load(List<Report> reports, ImportContext context, ImportResult result) {
        log.debug("Loading {} reports", reports.size());

        for (Report report : reports) {
            try {
                loadSingleReport(report, context, result);
            } catch (Exception ex) {
                log.error("Failed to load report '{}': {}", report.getReportName(), ex.getMessage());
                result.recordEntity("report", ImportResult.EntityAction.FAILED);
            }
        }
    }

    private void loadSingleReport(Report report, ImportContext context, ImportResult result) {
        log.debug("Loading report: {}", report.getReportName());

        // Validate basic fields
        List<ValidationResult> validationResults = new ArrayList<>();
        validationResults.add(inputValidator.validateEntityIdentifier(report.getReportName(), "reportName"));

        ValidationResult validation = inputValidator.validateAll(validationResults);
        if (!validation.isValid()) {
            log.error("Validation failed for report '{}': {}", report.getReportName(), validation.getErrorMessage());
            throw new IllegalArgumentException("Invalid report data: " + validation.getErrorMessage());
        }

        // Check if report exists
        List<Map<String, Object>> existingReports = apiClient.get("/api/v1/reports", List.class);

        Map<String, Object> existingReport = existingReports.stream()
                .filter(r -> report.getReportName().equals(r.get("reportName")))
                .findFirst()
                .orElse(null);

        Long reportId;
        boolean updated = false;

        // Build request body
        Map<String, Object> request = new HashMap<>();
        request.put("reportName", report.getReportName());
        request.put("reportType", report.getReportType());
        request.put("reportSubType", report.getReportSubType());
        request.put("reportCategory", report.getReportCategory());
        request.put("description", report.getDescription());
        request.put("reportSql", report.getReportSql());
        request.put("useReport", report.getUseReport());

        if (report.getReportParameters() != null) {
            List<Map<String, Object>> params = new ArrayList<>();
            for (ReportParameter param : report.getReportParameters()) {
                Map<String, Object> paramMap = new HashMap<>();
                if (param.getId() != null)
                    paramMap.put("id", param.getId());
                if (param.getParameterId() != null)
                    paramMap.put("parameterId", param.getParameterId());
                if (param.getReportParameterName() != null)
                    paramMap.put("reportParameterName", param.getReportParameterName());
                if (param.getParameterName() != null)
                    paramMap.put("parameterName", param.getParameterName()); // API sometimes needs this
                params.add(paramMap);
            }
            request.put("reportParameters", params);
        }

        if (existingReport == null) {
            // Create New
            Map<String, Object> response = apiClient.post("/api/v1/reports", request, Map.class);
            reportId = ((Number) response.get("resourceId")).longValue();
            log.info("Report created: {} (ID: {})", report.getReportName(), reportId);
            result.recordEntity("report", ImportResult.EntityAction.CREATED);

        } else {
            // Update Existing
            reportId = ((Number) existingReport.get("id")).longValue();
            // API for update uses PUT /reports/{id}

            // NOTE: Fineract might complain if we try to update core reports, but usually
            // it allows SQL updates
            log.debug("Report exists: {} (ID: {}), updating...", report.getReportName(), reportId);

            apiClient.put("/api/v1/reports/" + reportId, request, Map.class);

            log.info("Report updated: {} (ID: {})", report.getReportName(), reportId);
            result.recordEntity("report", ImportResult.EntityAction.UPDATED);
        }

        context.registerEntity("report", report.getReportName(), reportId);
    }
}
