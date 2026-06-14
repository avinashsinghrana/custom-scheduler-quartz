# Scheduler Common Jar

A common Spring Boot starter library designed to provide multi-tenant and multi-region Quartz scheduling capabilities powered by a Database Registry Workflow.

This library allows you to securely manage Jobs and Triggers entirely via your database, and schedule the same task across multiple timezones or crons flawlessly!

## Key Features
1. **Database Registry Workflow**: Jobs are inserted into a custom JPA registry (`custom_job_registry`) and wait in an `INACTIVE` state. Once marked `ACTIVE`, they are dynamically pushed to the Quartz Engine!
2. **Selective Job Grouping**: You can map a specific number of region/tenant groups to specific scheduled methods dynamically!
3. **Parallelism Control**: Enforce strict one-at-a-time execution or allow concurrent overlapping executions seamlessly.
4. **Zero-Configuration Defaults**: Inherits your Spring Boot `DataSource` directly and auto-creates all necessary Quartz tables (`spring.quartz.jdbc.initialize-schema=always`).

---

## 🚀 Complete Integration Guide

### 1. Include the Dependency
After building the project with `mvn clean install`, add this to your target Spring Boot application:
```xml
<dependency>
    <groupId>com.common</groupId>
    <artifactId>scheduler-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable the Scheduler
Add `@EnableCustomScheduler` to your main class:
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.common.scheduler.annotation.EnableCustomScheduler;

@SpringBootApplication
@EnableCustomScheduler
public class MySaaSApplication {
    public static void main(String[] args) {
        SpringApplication.run(MySaaSApplication.class, args);
    }
}
```

### 3. Database Configuration
Provide standard database details in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 4. Advanced Group Targeting (3 Groups vs 6 Groups)
You can define as many groups as you want globally. You can change their timezones or override their crons!

In your `application.properties`:
```properties
# Group 1-3 (e.g. Asia/Europe)
custom.scheduler.groups.G1.timezone=Asia/Kolkata
custom.scheduler.groups.G2.timezone=Europe/Berlin
custom.scheduler.groups.G3.timezone=Asia/Tokyo

# Group 4-6 (e.g. Americas)
custom.scheduler.groups.G4.timezone=America/New_York
custom.scheduler.groups.G5.timezone=America/Los_Angeles
custom.scheduler.groups.G6.timezone=America/Sao_Paulo
```

Then, use the `allowedGroups` parameter to map **3 groups to Scheduler 1** and **6 groups to Scheduler 2**:

```java
import org.springframework.stereotype.Service;
import com.common.scheduler.annotation.CustomScheduled;

@Service
public class TaskService {

    // --- SCHEDULER 1 ---
    // This will generate exactly 3 triggers (for G1, G2, G3)
    @CustomScheduled(
        jobName = "scheduler1",
        cron = "0 0 12 * * ?", // Runs at 12 PM
        parallelism = false,   // Strictly runs one-at-a-time across cluster
        allowedGroups = {"G1", "G2", "G3"}
    )
    public void scheduler1() {
        System.out.println("Scheduler 1 executing...");
    }

    // --- SCHEDULER 2 ---
    // This will generate exactly 6 triggers (for G1 through G6)
    @CustomScheduled(
        jobName = "scheduler2",
        cron = "0 0 9 * * ?", // Runs at 9 AM
        parallelism = true,   // Can run concurrently
        allowedGroups = {"G1", "G2", "G3", "G4", "G5", "G6"}
    )
    public void scheduler2() {
        System.out.println("Scheduler 2 executing...");
    }
}
```

### 5. Activating the Jobs
When your application starts, you'll see your `custom_job_registry` and `custom_trigger_registry` tables automatically populated with these jobs.

Because they are inserted as `INACTIVE`, they won't run yet. Activate them via SQL or your backend panel:
```sql
UPDATE custom_job_registry SET status = 'ACTIVE' WHERE job_name IN ('scheduler1', 'scheduler2');
```

Then, trigger the sync process (by restarting your app, or by manually calling `databaseSchedulerSyncService.syncDatabaseJobsWithQuartz()`). The Quartz engine will take over and automatically execute your tasks exactly across the 9 defined timezones!

### 6. Configurable Embedded UI Dashboard
This library ships with a zero-dependency, modern HTML dashboard. It allows you to monitor all active jobs, view execution metrics (like last execution time and status), and toggle jobs directly without restarting your app!

To enable it, simply add this property to your `application.properties`:
```properties
custom.scheduler.ui.enabled=true
```

Once enabled, visit `http://localhost:8080/custom-scheduler/ui` to see the dashboard.

The dashboard uses a REST API (`/custom-scheduler/api/jobs`) that returns a comprehensive JSON structure of your execution history. Here is an example of what it returns:

```json
[
    {
        "id": 3,
        "jobName": "scheduler3",
        "beanName": "sampleJobService",
        "methodName": "scheduler3",
        "status": "ACTIVE",
        "parallelism": false,
        "triggers": [
            {
                "id": 13,
                "jobGroup": "G1",
                "cronExpression": "0 * * * * ?",
                "timezone": "Asia/Kolkata",
                "lastExecutionDate": "2026-06-14T20:24:00.021",
                "nextExecutionDate": "2026-06-14T20:25:00",
                "lastExecutionStatus": "SUCCESS"
            },
            {
                "id": 18,
                "jobGroup": "UK_SHIFT",
                "cronExpression": "0 * * * * ?",
                "timezone": "Europe/London",
                "lastExecutionDate": "2026-06-14T20:24:00.097",
                "nextExecutionDate": "2026-06-14T20:25:00",
                "lastExecutionStatus": "SUCCESS"
            }
        ]
    }
]
```
*(Response truncated for brevity, but includes all jobs and their respective groups/timezones).*

### 7. Sample Project Reference
A complete, runnable sample Spring Boot project using this common jar is available.
To link or clone it, add the following remote:
```bash
git remote add origin https://github.com/avinashsinghrana/sample-project-for-custom-scheduler.git
```
