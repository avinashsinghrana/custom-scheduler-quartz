package com.common.scheduler.annotation;

import com.common.scheduler.config.CustomSchedulerAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation to enable the custom common Quartz scheduler in a Spring Boot application.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CustomSchedulerAutoConfiguration.class)
public @interface EnableCustomScheduler {
}
