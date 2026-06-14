package com.common.scheduler.config;

import com.common.scheduler.core.CustomScheduledBeanPostProcessor;
import com.common.scheduler.repository.JobEntityRepository;
import com.common.scheduler.repository.TriggerEntityRepository;
import com.common.scheduler.service.DatabaseSchedulerSyncService;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Scheduler.class)
@PropertySource("classpath:custom-scheduler-default.properties")
@EnableConfigurationProperties(CustomSchedulerProperties.class)
@EntityScan(basePackages = "com.common.scheduler.entity")
@EnableJpaRepositories(basePackages = "com.common.scheduler.repository")
public class CustomSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CustomScheduledBeanPostProcessor customScheduledBeanPostProcessor(
            ObjectProvider<JobEntityRepository> jobRepoProvider,
            ObjectProvider<TriggerEntityRepository> triggerRepoProvider,
            CustomSchedulerProperties properties) {
        return new CustomScheduledBeanPostProcessor(jobRepoProvider, triggerRepoProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseSchedulerSyncService databaseSchedulerSyncService(JobEntityRepository jobEntityRepository, Scheduler scheduler) {
        return new DatabaseSchedulerSyncService(jobEntityRepository, scheduler);
    }

    @Bean
    public com.common.scheduler.controller.SchedulerUIController schedulerUIController(
            JobEntityRepository jobEntityRepository, 
            DatabaseSchedulerSyncService databaseSchedulerSyncService) {
        return new com.common.scheduler.controller.SchedulerUIController(jobEntityRepository, databaseSchedulerSyncService);
    }
}
