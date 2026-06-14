package com.common.scheduler.core;

import com.common.scheduler.annotation.CustomScheduled;
import com.common.scheduler.config.CustomSchedulerProperties;
import com.common.scheduler.entity.JobEntity;
import com.common.scheduler.entity.TriggerEntity;
import com.common.scheduler.repository.JobEntityRepository;
import com.common.scheduler.repository.TriggerEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Scans beans for @CustomScheduled methods and registers them in the database registry.
 */
public class CustomScheduledBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CustomScheduledBeanPostProcessor.class);

    private final ObjectProvider<JobEntityRepository> jobRepoProvider;
    private final ObjectProvider<TriggerEntityRepository> triggerRepoProvider;
    private final CustomSchedulerProperties properties;
    private final List<ScheduledMethod> scheduledMethods = new ArrayList<>();

    public CustomScheduledBeanPostProcessor(
            ObjectProvider<JobEntityRepository> jobRepoProvider,
            ObjectProvider<TriggerEntityRepository> triggerRepoProvider,
            CustomSchedulerProperties properties) {
        this.jobRepoProvider = jobRepoProvider;
        this.triggerRepoProvider = triggerRepoProvider;
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Map<Method, CustomScheduled> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<CustomScheduled>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, CustomScheduled.class));

        if (!annotatedMethods.isEmpty()) {
            annotatedMethods.forEach((method, customScheduled) -> {
                scheduledMethods.add(new ScheduledMethod(beanName, method, customScheduled));
                log.debug("Found @CustomScheduled method: {} on bean: {}", method.getName(), beanName);
            });
        }
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (scheduledMethods.isEmpty()) {
            return;
        }

        JobEntityRepository jobRepo = jobRepoProvider.getIfAvailable();
        TriggerEntityRepository triggerRepo = triggerRepoProvider.getIfAvailable();

        if (jobRepo == null || triggerRepo == null) {
            log.warn("JPA Repositories not found. @CustomScheduled methods will not be registered to DB.");
            return;
        }

        for (ScheduledMethod sm : scheduledMethods) {
            registerToDatabase(jobRepo, triggerRepo, sm);
        }
    }

    private void registerToDatabase(JobEntityRepository jobRepo, TriggerEntityRepository triggerRepo, ScheduledMethod sm) {
        CustomScheduled annotation = sm.annotation;
        String baseJobName = annotation.jobName().isEmpty() ? sm.beanName + "." + sm.method.getName() : annotation.jobName();

        // 1. Ensure JobEntity exists
        JobEntity jobEntity = jobRepo.findByJobName(baseJobName).orElseGet(() -> {
            JobEntity newJob = new JobEntity();
            newJob.setJobName(baseJobName);
            newJob.setBeanName(sm.beanName);
            newJob.setMethodName(sm.method.getName());
            newJob.setStatus("INACTIVE"); // Default status as requested
            newJob.setParallelism(annotation.parallelism());
            log.info("Registering new JobEntity: {} with INACTIVE status and parallelism: {}", baseJobName, annotation.parallelism());
            return jobRepo.save(newJob);
        });

        // 2. Register Triggers for configured groups
        Map<String, CustomSchedulerProperties.JobGroupConfig> groups = properties.getGroups();
        if (groups == null || groups.isEmpty()) {
            String groupName = annotation.jobGroup();
            String cron = annotation.cron();
            saveOrUpdateTrigger(triggerRepo, jobEntity, groupName, cron, TimeZone.getDefault().getID());
        } else {
            List<String> allowedGroupsList = Arrays.asList(annotation.allowedGroups());
            
            for (Map.Entry<String, CustomSchedulerProperties.JobGroupConfig> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                
                // If allowedGroups is defined on the method, skip groups not in the list
                if (!allowedGroupsList.isEmpty() && !allowedGroupsList.contains(groupName)) {
                    continue;
                }

                CustomSchedulerProperties.JobGroupConfig config = entry.getValue();

                String cron = (config.getCron() != null && !config.getCron().isBlank()) ? config.getCron() : annotation.cron();
                String timezoneId = (config.getTimezone() != null && !config.getTimezone().isBlank()) 
                        ? config.getTimezone() 
                        : TimeZone.getDefault().getID();

                saveOrUpdateTrigger(triggerRepo, jobEntity, groupName, cron, timezoneId);
            }
        }
    }

    private void saveOrUpdateTrigger(TriggerEntityRepository triggerRepo, JobEntity job, String groupName, String cron, String timezone) {
        Optional<TriggerEntity> existing = triggerRepo.findByJobAndJobGroup(job, groupName);
        if (existing.isEmpty()) {
            TriggerEntity trigger = new TriggerEntity();
            trigger.setJob(job);
            trigger.setJobGroup(groupName);
            trigger.setCronExpression(cron);
            trigger.setTimezone(timezone);
            triggerRepo.save(trigger);
            log.info("Registered new Trigger for Job: {} in Group: {}", job.getJobName(), groupName);
        } else {
            // Update existing trigger if properties changed
            TriggerEntity trigger = existing.get();
            boolean changed = false;
            if (!trigger.getCronExpression().equals(cron)) {
                trigger.setCronExpression(cron);
                changed = true;
            }
            if (!trigger.getTimezone().equals(timezone)) {
                trigger.setTimezone(timezone);
                changed = true;
            }
            if (changed) {
                triggerRepo.save(trigger);
                log.info("Updated Trigger for Job: {} in Group: {}", job.getJobName(), groupName);
            }
        }
    }

    private static class ScheduledMethod {
        final String beanName;
        final Method method;
        final CustomScheduled annotation;

        ScheduledMethod(String beanName, Method method, CustomScheduled annotation) {
            this.beanName = beanName;
            this.method = method;
            this.annotation = annotation;
        }
    }
}
