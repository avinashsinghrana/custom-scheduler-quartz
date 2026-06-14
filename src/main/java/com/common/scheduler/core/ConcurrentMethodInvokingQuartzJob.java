package com.common.scheduler.core;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Generic Quartz Job that invokes a specific method on a target Spring Bean.
 * Allows concurrent executions.
 */
public class ConcurrentMethodInvokingQuartzJob extends QuartzJobBean {

    public static final String TARGET_BEAN_NAME = "targetBeanName";
    public static final String TARGET_METHOD_NAME = "targetMethodName";

    private final ApplicationContext applicationContext;

    public ConcurrentMethodInvokingQuartzJob(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String beanName = jobDataMap.getString(TARGET_BEAN_NAME);
        String methodName = jobDataMap.getString(TARGET_METHOD_NAME);

        String status = "SUCCESS";
        JobExecutionException executionException = null;

        try {
            Object bean = applicationContext.getBean(beanName);
            Method method = ReflectionUtils.findMethod(bean.getClass(), methodName);
            
            if (method != null) {
                ReflectionUtils.makeAccessible(method);
                ReflectionUtils.invokeMethod(method, bean);
            } else {
                throw new JobExecutionException("Method " + methodName + " not found on bean " + beanName);
            }
        } catch (Exception e) {
            status = "FAILED";
            executionException = new JobExecutionException("Failed to invoke scheduled method", e);
            throw executionException;
        } finally {
            updateExecutionMetrics(context, status);
        }
    }

    private void updateExecutionMetrics(JobExecutionContext context, String status) {
        try {
            String jobName = context.getJobDetail().getKey().getName();
            String jobGroup = context.getTrigger().getKey().getGroup();

            com.common.scheduler.repository.JobEntityRepository jobRepo = applicationContext.getBean(com.common.scheduler.repository.JobEntityRepository.class);
            com.common.scheduler.repository.TriggerEntityRepository triggerRepo = applicationContext.getBean(com.common.scheduler.repository.TriggerEntityRepository.class);

            jobRepo.findByJobName(jobName).ifPresent(job -> {
                triggerRepo.findByJobAndJobGroup(job, jobGroup).ifPresent(trigger -> {
                    if (context.getFireTime() != null) {
                        trigger.setLastExecutionDate(context.getFireTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    }
                    if (context.getNextFireTime() != null) {
                        trigger.setNextExecutionDate(context.getNextFireTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    }
                    trigger.setLastExecutionStatus(status);
                    triggerRepo.save(trigger);
                });
            });
        } catch (Exception e) {
            // Log silently, we don't want metric update failures to crash the job itself
            e.printStackTrace();
        }
    }
}
