package com.common.scheduler.annotation;

import java.lang.annotation.*;

/**
 * Annotation to schedule a method execution using Quartz.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomScheduled {

    /**
     * A Quartz cron expression to define the schedule.
     */
    String cron();

    /**
     * Optional name of the job. If empty, a default name will be generated.
     */
    String jobName() default "";

    /**
     * Optional group of the job.
     */
    String jobGroup() default "DEFAULT_GROUP";
}
