package com.common.scheduler.controller;

import com.common.scheduler.entity.JobEntity;
import com.common.scheduler.repository.JobEntityRepository;
import com.common.scheduler.service.DatabaseSchedulerSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/custom-scheduler")
@ConditionalOnProperty(name = "custom.scheduler.ui.enabled", havingValue = "true")
public class SchedulerUIController {

    private final JobEntityRepository jobEntityRepository;
    private final DatabaseSchedulerSyncService databaseSchedulerSyncService;

    public SchedulerUIController(JobEntityRepository jobEntityRepository, DatabaseSchedulerSyncService databaseSchedulerSyncService) {
        this.jobEntityRepository = jobEntityRepository;
        this.databaseSchedulerSyncService = databaseSchedulerSyncService;
    }

    /**
     * Serves the embedded HTML dashboard
     */
    @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
    public String getUi() {
        return SchedulerUIHtml.HTML;
    }

    /**
     * API to fetch all jobs
     */
    @GetMapping("/api/jobs")
    public List<JobEntity> getJobs() {
        return jobEntityRepository.findAll();
    }

    /**
     * API to toggle status
     */
    @PostMapping("/api/jobs/{jobId}/status")
    public void updateStatus(@PathVariable Long jobId, @RequestParam String status) {
        jobEntityRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status.toUpperCase());
            jobEntityRepository.save(job);
        });

        // Trigger synchronization so Quartz picks up the new status immediately
        databaseSchedulerSyncService.syncDatabaseJobsWithQuartz();
    }
}
