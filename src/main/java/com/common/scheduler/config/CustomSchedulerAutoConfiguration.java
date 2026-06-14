package com.common.scheduler.config;

import com.common.scheduler.core.CustomScheduledBeanPostProcessor;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Scheduler.class)
@PropertySource("classpath:custom-scheduler-default.properties")
@EnableConfigurationProperties(CustomSchedulerProperties.class)
public class CustomSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CustomScheduledBeanPostProcessor customScheduledBeanPostProcessor(
            ObjectProvider<Scheduler> schedulerProvider,
            CustomSchedulerProperties properties) {
        return new CustomScheduledBeanPostProcessor(schedulerProvider, properties);
    }
}
