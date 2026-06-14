package com.common.scheduler.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_job_registry")
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String beanName;

    @Column(nullable = false)
    private String methodName;

    @Column(nullable = false)
    private String status; // e.g., ACTIVE, INACTIVE

    @Column(nullable = false)
    private Boolean parallelism = false;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TriggerEntity> triggers = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getParallelism() {
        return parallelism;
    }

    public void setParallelism(Boolean parallelism) {
        this.parallelism = parallelism;
    }

    public List<TriggerEntity> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<TriggerEntity> triggers) {
        this.triggers = triggers;
    }
    
    public void addTrigger(TriggerEntity trigger) {
        triggers.add(trigger);
        trigger.setJob(this);
    }
}
