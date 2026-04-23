package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StarterWorkerRepository
    extends JpaRepository<WorkerEntity, UUID>, JpaSpecificationExecutor<WorkerEntity>{

  @Modifying
  @Query("update WorkerEntity w set w.lockedUntilTime = :dt where w.id in :ids and w.executorId = :executorId")
  void updateLockUntilTime(OffsetDateTime dt, List<UUID> ids, UUID executorId);

  @Modifying
  @Query("update WorkerEntity w set w.status = :status where w.starter.id = :id")
  void updateWorkerStatusForStarter(UUID id, String status);

  @Modifying
  @Query("update WorkerEntity w set w.status = :status where w.id = :id")
  void updateWorkerStatus(UUID id, String status);

  @Query("""
        select ww.id as id from WorkerEntity ww
          join ww.starter st
         where ww.status in (:statuses)
           and ww.retryCount > 0
           and (ww.overdueTime is null or ww.overdueTime < :odt)
           and (ww.lockedUntilTime is null or ww.lockedUntilTime < :odt)
           and st.type = :type
           order by ww.lockedUntilTime nulls first, ww.createTime
           limit 1
""")
  Optional<UUID> findFirstOrderByLockedTime(Set<String> statuses, String type, OffsetDateTime odt);

  List<WorkerEntity> findByStatusAndIdIn(String status, Collection<UUID> ids);
  
  List<WorkerEntity> findByStarterId(UUID starterId);

  Optional<WorkerEntity> findByIdAndExecutorId(UUID id, UUID executorId);
  
  boolean existsByIdAndExecutorId(UUID workerId, UUID executorId);
  
  @Modifying
  @Query("delete WorkerEntity w where w.starter.id = :id")
  void deleteWorkersForStarter(UUID id);

  @Query("select w.id from WorkerEntity w where w.executorId = :executorId")
  Set<UUID> findIdsByExecutorId(UUID executorId);

  @Override
  @EntityGraph(value = "Worker.withStarter", type = EntityGraph.EntityGraphType.LOAD)
  Page<WorkerEntity> findAll(Specification<WorkerEntity> spec, Pageable pageable);
}
