package com.common.scheduler.repository;

import com.common.scheduler.entity.JobEntity;
import com.common.scheduler.entity.TriggerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TriggerEntityRepository extends JpaRepository<TriggerEntity, Long> {

    Optional<TriggerEntity> findByJobAndJobGroup(JobEntity job, String jobGroup);
}
