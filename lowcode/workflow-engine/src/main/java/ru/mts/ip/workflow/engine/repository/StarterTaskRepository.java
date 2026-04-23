package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface StarterTaskRepository
    extends JpaRepository<StarterTaskEntity, UUID>, JpaSpecificationExecutor<StarterTaskEntity> {

  @Modifying
  @Query("DELETE FROM StarterTaskEntity t WHERE t.id IN "
         + "(SELECT st.id FROM StarterTaskEntity st WHERE "
         + "st.state = 'COMPLETE' AND st.createTime < :cutoffDate "
         + "ORDER BY st.createTime LIMIT :batchSize)")
  void deleteOldTasks(@Param("cutoffDate") OffsetDateTime cutoffDate,
      @Param("batchSize") int batchSize);
}
