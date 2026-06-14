# Scheduler Common Jar

A common Spring Boot starter library designed to easily provide multi-tenant and multi-region Quartz scheduling capabilities to any Spring Boot application. 

This library abstracts away complex Quartz scheduling logic, allowing you to seamlessly register method-level jobs and schedule them across multiple timezones and regions concurrently.

## Key Features

1. **`@EnableCustomScheduler` Annotation**: Add this to your Spring Boot main application class. It automatically initializes the Quartz components, forces it to safely persist schedules using your default `DataSource`, and prevents duplicate schedule creation across cluster nodes.
2. **`@CustomScheduled` Annotation**: A method-level annotation used to flag any bean method as a scheduled task.
3. **Multi-Region / Multi-Tenant Scaling**: Seamlessly bind single methods to trigger concurrently across multiple country timezones.
4. **Zero-Configuration Defaults**: If no custom regions are defined, it safely falls back to standard single-trigger scheduling.
5. **Database Agnostic**: Inherits `spring.quartz.job-store-type=jdbc` out of the box, utilizing your host Spring Boot application's `DataSource` to automatically construct necessary standard Quartz tables (`QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, etc.).

---

## 🚀 Complete Workflow & Integration Guide

### 1. Build and Install the Library
First, build the library from the source code and install it in your local `.m2` maven repository.
```bash
mvn clean install
```

### 2. Include the Dependency
In the target Spring Boot Application where you wish to run scheduled tasks, include the dependency in your `pom.xml`:
```xml
<dependency>
    <groupId>com.common</groupId>
    <artifactId>scheduler-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3. Enable the Scheduler
Open your main Application class and add the `@EnableCustomScheduler` annotation. This tells the library to intercept beans, prepare the database, and inject the core Engine.

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

### 4. Provide Database Configuration
Quartz needs a database to store its triggers. Because this library inherits your Spring application's data source, simply provide standard database details in your `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```
*Note: Because our library automatically sets `spring.quartz.jdbc.initialize-schema=always`, Quartz will automatically generate the required database tables upon the first application startup.*

### 5. Schedule a Target Method
Use the `@CustomScheduled` annotation on any method within a standard Spring Bean (e.g. `@Service`, `@Component`).

The `cron` supplied here acts as the **global default**.

```java
import org.springframework.stereotype.Service;
import com.common.scheduler.annotation.CustomScheduled;

@Service
public class BillingService {

    // By default, this will run at 12:00 PM (Noon) every day.
    @CustomScheduled(cron = "0 0 12 * * ?", jobName = "dailyBilling")
    public void executeDailyBilling() {
        System.out.println("Executing daily billing cycle for region...");
    }
}
```

### 6. Configure Timezones and Multi-Region Groups (Optional)
If your SaaS operates across different countries, you can automatically schedule the single `executeDailyBilling()` method to run relative to the timezone of each country. 

Add the `custom.scheduler.groups` properties to your host application's `application.properties`:

```properties
# ---------------------------------------------------------
# Tenant 1: India 
# (Fires at 12:00 PM Indian Standard Time)
# ---------------------------------------------------------
custom.scheduler.groups.IN.timezone=Asia/Kolkata

# ---------------------------------------------------------
# Tenant 2: USA - New York 
# (Fires at 12:00 PM Eastern Standard Time)
# ---------------------------------------------------------
custom.scheduler.groups.US.timezone=America/New_York

# ---------------------------------------------------------
# Tenant 3: Europe - Berlin
# (We OVERRIDE the cron to fire at 2:00 PM Berlin Time instead)
# ---------------------------------------------------------
custom.scheduler.groups.EU.timezone=Europe/Berlin
custom.scheduler.groups.EU.cron=0 0 14 * * ?
```

**How It Works:**
When the Spring Application starts up, the engine observes 3 Job Groups defined in your properties. It will dynamically wrap your `BillingService.executeDailyBilling()` method inside 3 separate and persistent Quartz Jobs. Each Trigger will strictly observe its configured TimeZone independently.

*If no `custom.scheduler.groups` are defined, the library defaults to creating exactly 1 trigger utilizing the system's default TimeZone.*

---

## Technical Details 
- **Underlying Scheduler:** [Quartz Scheduler](http://www.quartz-scheduler.org/) via `spring-boot-starter-quartz`.
- **Reflection Access:** The library utilizes a secure custom `QuartzJobBean` implementation (`MethodInvokingQuartzJob.java`) which securely obtains your target Spring Bean by its Bean Name directly from the `ApplicationContext` and triggers it dynamically. This avoids direct serialization limits with Quartz.
