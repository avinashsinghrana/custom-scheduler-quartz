package com.common.scheduler.core;

import org.quartz.DisallowConcurrentExecution;
import org.springframework.context.ApplicationContext;

/**
 * Quartz Job that enforces strict single-execution locking across the cluster.
 */
@DisallowConcurrentExecution
public class NonConcurrentMethodInvokingQuartzJob extends ConcurrentMethodInvokingQuartzJob {

    public NonConcurrentMethodInvokingQuartzJob(ApplicationContext applicationContext) {
        super(applicationContext);
    }
}
