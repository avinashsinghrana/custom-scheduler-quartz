package com.common.scheduler.repository;

import com.common.scheduler.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface JobEntityRepository extends JpaRepository<JobEntity, Long> {
    
    Optional<JobEntity> findByJobName(String jobName);
    
    List<JobEntity> findByStatus(String status);
}
