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
            throw new JobExecutionException("Failed to invoke scheduled method", e);
        }
    }
}
