package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mts.ip.workflow.engine.entity.StarterEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StarterRepository extends JpaRepository<StarterEntity, UUID>, CustomizedStarterRepository{

  Optional<StarterEntity> findByTypeAndNameAndTenantId(String type, String name, String tenantId);
  List<StarterEntity> findByWorkflowDefinitionId(UUID workflowDefinitionId);

  Optional<StarterEntity> findByNameAndTypeAndWorkflowDefinitionId(String name, String type, UUID definitionId);

  @Query("""
    select s
    from StarterEntity s
    where s.endDateTime is not null and  s.endDateTime < CURRENT_TIMESTAMP
    and s.actualStatus in (:statuses)
""")
  List<StarterEntity> getExpiredStarters(List<String> statuses);
}
