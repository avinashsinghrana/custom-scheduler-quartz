package com.common.scheduler.service;

import com.common.scheduler.core.ConcurrentMethodInvokingQuartzJob;
import com.common.scheduler.core.NonConcurrentMethodInvokingQuartzJob;
import com.common.scheduler.entity.JobEntity;
import com.common.scheduler.entity.TriggerEntity;
import com.common.scheduler.repository.JobEntityRepository;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.TimeZone;

@Service
public class DatabaseSchedulerSyncService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchedulerSyncService.class);

    private final JobEntityRepository jobEntityRepository;
    private final Scheduler scheduler;

    public DatabaseSchedulerSyncService(JobEntityRepository jobEntityRepository, Scheduler scheduler) {
        this.jobEntityRepository = jobEntityRepository;
        this.scheduler = scheduler;
    }

    /**
     * Runs automatically when the Spring application is fully started.
     * It can also be called manually to force a sync if database statuses are updated.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncDatabaseJobsWithQuartz() {
        log.info("Starting synchronization between Database Registry and Quartz Scheduler...");

        List<JobEntity> allJobs = jobEntityRepository.findAll();

        for (JobEntity job : allJobs) {
            try {
                if ("ACTIVE".equalsIgnoreCase(job.getStatus())) {
                    scheduleActiveJob(job);
                } else {
                    unscheduleInactiveJob(job);
                }
            } catch (SchedulerException e) {
                log.error("Failed to sync job: " + job.getJobName(), e);
            }
        }

        log.info("Synchronization complete.");
    }

    private void scheduleActiveJob(JobEntity job) throws SchedulerException {
        for (TriggerEntity triggerEntity : job.getTriggers()) {
            JobKey jobKey = JobKey.jobKey(job.getJobName(), triggerEntity.getJobGroup());
            TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName() + "Trigger", triggerEntity.getJobGroup());

            Class<? extends Job> jobClass = Boolean.TRUE.equals(job.getParallelism()) 
                    ? ConcurrentMethodInvokingQuartzJob.class 
                    : NonConcurrentMethodInvokingQuartzJob.class;

            // Build Job Detail
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .usingJobData(ConcurrentMethodInvokingQuartzJob.TARGET_BEAN_NAME, job.getBeanName())
                    .usingJobData(ConcurrentMethodInvokingQuartzJob.TARGET_METHOD_NAME, job.getMethodName())
                    .storeDurably()
                    .build();

            // Build Trigger
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(triggerEntity.getCronExpression())
                    .inTimeZone(TimeZone.getTimeZone(triggerEntity.getTimezone()));

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .withSchedule(scheduleBuilder)
                    .build();

            if (scheduler.checkExists(jobKey)) {
                // If it exists, we might need to update the trigger if cron changed
                Trigger existingTrigger = scheduler.getTrigger(triggerKey);
                if (existingTrigger instanceof CronTrigger) {
                    CronTrigger existingCron = (CronTrigger) existingTrigger;
                    if (!existingCron.getCronExpression().equals(triggerEntity.getCronExpression()) ||
                        !existingCron.getTimeZone().getID().equals(triggerEntity.getTimezone())) {
                        
                        log.info("Updating existing Quartz trigger for job {} group {}", job.getJobName(), triggerEntity.getJobGroup());
                        scheduler.rescheduleJob(triggerKey, trigger);
                    }
                }
            } else {
                // Doesn't exist, schedule it
                log.info("Scheduling ACTIVE job {} for group {}", job.getJobName(), triggerEntity.getJobGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            }
        }
    }

    private void unscheduleInactiveJob(JobEntity job) throws SchedulerException {
        for (TriggerEntity triggerEntity : job.getTriggers()) {
            JobKey jobKey = JobKey.jobKey(job.getJobName(), triggerEntity.getJobGroup());
            if (scheduler.checkExists(jobKey)) {
                log.info("Unscheduling INACTIVE job {} for group {}", job.getJobName(), triggerEntity.getJobGroup());
                scheduler.deleteJob(jobKey);
            }
        }
    }
}
