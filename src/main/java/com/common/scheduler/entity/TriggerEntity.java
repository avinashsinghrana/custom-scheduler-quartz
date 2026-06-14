package com.common.scheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_trigger_registry", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"job_id", "jobGroup"})
})
public class TriggerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnore
    private JobEntity job;

    @Column(nullable = false)
    private String jobGroup;

    @Column(nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "last_execution_date")
    private LocalDateTime lastExecutionDate;

    @Column(name = "next_execution_date")
    private LocalDateTime nextExecutionDate;

    @Column(name = "last_execution_status")
    private String lastExecutionStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobEntity getJob() {
        return job;
    }

    public void setJob(JobEntity job) {
        this.job = job;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalDateTime getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    public LocalDateTime getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(LocalDateTime nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public String getLastExecutionStatus() {
        return lastExecutionStatus;
    }

    public void setLastExecutionStatus(String lastExecutionStatus) {
        this.lastExecutionStatus = lastExecutionStatus;
    }
}
