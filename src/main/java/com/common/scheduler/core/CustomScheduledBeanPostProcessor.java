package com.common.scheduler.core;

import com.common.scheduler.annotation.CustomScheduled;
import com.common.scheduler.config.CustomSchedulerProperties;
import org.quartz.*;
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
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Scans beans for @CustomScheduled methods and registers them with Quartz Scheduler
 * supporting multi-tenant/multi-region job groups.
 */
public class CustomScheduledBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CustomScheduledBeanPostProcessor.class);

    private final ObjectProvider<Scheduler> schedulerProvider;
    private final CustomSchedulerProperties properties;
    private final List<ScheduledMethod> scheduledMethods = new ArrayList<>();

    public CustomScheduledBeanPostProcessor(ObjectProvider<Scheduler> schedulerProvider,
                                            CustomSchedulerProperties properties) {
        this.schedulerProvider = schedulerProvider;
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

        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.warn("No Quartz Scheduler bean found. @CustomScheduled methods will not be registered.");
            return;
        }

        for (ScheduledMethod sm : scheduledMethods) {
            try {
                scheduleForConfiguredGroups(scheduler, sm);
            } catch (SchedulerException e) {
                log.error("Failed to schedule method: " + sm.method.getName() + " on bean: " + sm.beanName, e);
            }
        }
    }

    private void scheduleForConfiguredGroups(Scheduler scheduler, ScheduledMethod sm) throws SchedulerException {
        Map<String, CustomSchedulerProperties.JobGroupConfig> groups = properties.getGroups();

        if (groups == null || groups.isEmpty()) {
            // Default behavior if no groups are configured
            log.info("No job groups configured. Using default group for {} on bean {}", sm.method.getName(), sm.beanName);
            String groupName = sm.annotation.jobGroup();
            String cron = sm.annotation.cron();
            scheduleJob(scheduler, sm, groupName, cron, TimeZone.getDefault());
        } else {
            // Register a job/trigger for each configured group
            for (Map.Entry<String, CustomSchedulerProperties.JobGroupConfig> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                CustomSchedulerProperties.JobGroupConfig config = entry.getValue();

                // Override cron if specified in properties, else fallback to annotation
                String cron = (config.getCron() != null && !config.getCron().isBlank()) ? config.getCron() : sm.annotation.cron();
                
                // Use the timezone from properties if available
                TimeZone timeZone = (config.getTimezone() != null && !config.getTimezone().isBlank()) 
                        ? TimeZone.getTimeZone(config.getTimezone()) 
                        : TimeZone.getDefault();

                scheduleJob(scheduler, sm, groupName, cron, timeZone);
            }
        }
    }

    private void scheduleJob(Scheduler scheduler, ScheduledMethod sm, String groupName, String cronExpression, TimeZone timeZone) throws SchedulerException {
        CustomScheduled annotation = sm.annotation;
        
        // Base name for the job/trigger. If not provided, fallback to beanName.methodName
        String baseJobName = annotation.jobName().isEmpty() ? sm.beanName + "." + sm.method.getName() : annotation.jobName();

        // Make identity unique per group
        JobKey jobKey = JobKey.jobKey(baseJobName, groupName);
        TriggerKey triggerKey = TriggerKey.triggerKey(baseJobName + "Trigger", groupName);

        if (scheduler.checkExists(jobKey)) {
            log.info("Job {} already exists in group {}. Skipping registration.", baseJobName, groupName);
            return;
        }

        JobDetail jobDetail = JobBuilder.newJob(MethodInvokingQuartzJob.class)
                .withIdentity(jobKey)
                .usingJobData(MethodInvokingQuartzJob.TARGET_BEAN_NAME, sm.beanName)
                .usingJobData(MethodInvokingQuartzJob.TARGET_METHOD_NAME, sm.method.getName())
                .storeDurably()
                .build();

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
                .inTimeZone(timeZone);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(scheduleBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job {} for group {} with cron {} and timezone {}", baseJobName, groupName, cronExpression, timeZone.getID());
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
