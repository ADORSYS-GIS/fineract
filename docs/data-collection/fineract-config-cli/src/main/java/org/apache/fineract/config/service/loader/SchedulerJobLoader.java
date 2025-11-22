package org.apache.fineract.config.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.config.model.ImportContext;
import org.apache.fineract.config.model.ImportResult;
import org.apache.fineract.config.model.systemconfig.SchedulerJob;
import org.apache.fineract.config.provider.FineractApiClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Loader for scheduler job configuration.
 *
 * <p>Configures Fineract scheduler jobs for background tasks.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/jobs - List all jobs
 *   <li>PUT /api/v1/jobs/{id} - Update job configuration
 *   <li>POST /api/v1/jobs/{id}?command=executeJob - Run job immediately
 * </ul>
 */
@Slf4j
@Component
public class SchedulerJobLoader {

  private final FineractApiClient apiClient;

  public SchedulerJobLoader(FineractApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Loads scheduler job configuration.
   *
   * @param jobs list of scheduler jobs
   * @param context import context
   * @param result import result
   */
  public void load(List<SchedulerJob> jobs, ImportContext context, ImportResult result) {
    log.info("Configuring {} scheduler jobs", jobs.size());

    // Fetch existing jobs from Fineract
    Map<String, Map<String, Object>> existingJobs = fetchExistingJobs();
    log.debug("Found {} existing scheduler jobs in Fineract", existingJobs.size());

    for (SchedulerJob job : jobs) {
      try {
        loadSingleJob(job, existingJobs, context, result);
      } catch (Exception ex) {
        log.error("Failed to configure scheduler job '{}': {}", job.getName(), ex.getMessage());
        result.recordEntity("schedulerJob", ImportResult.EntityAction.FAILED);
      }
    }
  }

  /**
   * Fetches existing scheduler jobs from Fineract.
   *
   * @return map of job name to job details
   */
  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> fetchExistingJobs() {
    Map<String, Map<String, Object>> jobs = new HashMap<>();

    try {
      List<Map<String, Object>> jobList = apiClient.get("/api/v1/jobs", List.class);
      for (Map<String, Object> job : jobList) {
        String displayName = (String) job.get("displayName");
        String name = (String) job.get("name");

        // Index by both displayName and name for flexible matching
        if (displayName != null) {
          jobs.put(displayName.toLowerCase(), job);
        }
        if (name != null) {
          jobs.put(name.toLowerCase(), job);
        }
      }
    } catch (Exception ex) {
      log.warn("Could not fetch existing scheduler jobs: {}", ex.getMessage());
    }

    return jobs;
  }

  /**
   * Loads a single scheduler job configuration.
   *
   * @param job job configuration
   * @param existingJobs map of existing jobs
   * @param context import context
   * @param result import result
   */
  private void loadSingleJob(
      SchedulerJob job,
      Map<String, Map<String, Object>> existingJobs,
      ImportContext context,
      ImportResult result) {

    String jobName = job.getName() != null ? job.getName() : job.getDisplayName();
    log.debug("Configuring scheduler job: {}", jobName);

    // Find the job in Fineract (match by name or displayName)
    Map<String, Object> existingJob = findJob(job, existingJobs);

    if (existingJob == null) {
      log.warn(
          "Scheduler job '{}' not found in Fineract. Jobs cannot be created, only configured.",
          jobName);
      result.recordEntity("schedulerJob", ImportResult.EntityAction.FAILED);
      return;
    }

    Long jobId = ((Number) existingJob.get("jobId")).longValue();

    // Check if job needs update
    boolean needsUpdate = false;
    Map<String, Object> updateRequest = new HashMap<>();

    // Check cron expression
    if (job.getCronExpression() != null) {
      String existingCron = (String) existingJob.get("cronExpression");
      if (!job.getCronExpression().equals(existingCron)) {
        updateRequest.put("cronExpression", job.getCronExpression());
        needsUpdate = true;
      }
    }

    // Check active status
    if (job.getActive() != null) {
      Boolean existingActive = (Boolean) existingJob.get("active");
      if (!job.getActive().equals(existingActive)) {
        updateRequest.put("active", job.getActive());
        needsUpdate = true;
      }
    }

    if (needsUpdate) {
      // Update the job
      apiClient.put("/api/v1/jobs/" + jobId, updateRequest, Map.class);
      log.info(
          "Scheduler job updated: {} (ID: {}) - cron: {}, active: {}",
          jobName,
          jobId,
          job.getCronExpression(),
          job.getActive());
      result.recordEntity("schedulerJob", ImportResult.EntityAction.UPDATED);
    } else {
      log.debug("Scheduler job unchanged: {}", jobName);
      result.recordEntity("schedulerJob", ImportResult.EntityAction.UNCHANGED);
    }

    // Register job for reference
    context.registerEntity("schedulerJob", jobName, jobId);

    // Run job immediately if requested
    if (Boolean.TRUE.equals(job.getRunOnImport())) {
      runJobNow(jobId, jobName);
    }
  }

  /**
   * Finds a job in existing jobs by name or displayName.
   *
   * @param job job configuration
   * @param existingJobs map of existing jobs
   * @return existing job or null if not found
   */
  private Map<String, Object> findJob(
      SchedulerJob job, Map<String, Map<String, Object>> existingJobs) {

    // Try by name first
    if (job.getName() != null) {
      Map<String, Object> found = existingJobs.get(job.getName().toLowerCase());
      if (found != null) {
        return found;
      }
    }

    // Try by displayName
    if (job.getDisplayName() != null) {
      Map<String, Object> found = existingJobs.get(job.getDisplayName().toLowerCase());
      if (found != null) {
        return found;
      }
    }

    // Try partial match
    String searchName = (job.getName() != null ? job.getName() : job.getDisplayName());
    if (searchName != null) {
      String searchLower = searchName.toLowerCase();
      for (Map.Entry<String, Map<String, Object>> entry : existingJobs.entrySet()) {
        if (entry.getKey().contains(searchLower) || searchLower.contains(entry.getKey())) {
          return entry.getValue();
        }
      }
    }

    return null;
  }

  /**
   * Runs a scheduler job immediately.
   *
   * @param jobId job ID
   * @param jobName job name (for logging)
   */
  private void runJobNow(Long jobId, String jobName) {
    try {
      apiClient.post("/api/v1/jobs/" + jobId + "?command=executeJob", Map.of(), Map.class);
      log.info("Scheduler job triggered: {} (ID: {})", jobName, jobId);
    } catch (Exception ex) {
      log.warn("Could not trigger scheduler job '{}': {}", jobName, ex.getMessage());
    }
  }
}
