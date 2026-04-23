package ru.mts.ip.workflow.engine.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mts.ip.workflow.engine.entity.WorkflowAccessWhiteListId;
import ru.mts.ip.workflow.engine.entity.WorkflowAccessWhitelist;

public interface WorkflowAccessWhiteListRepository extends JpaRepository<WorkflowAccessWhitelist, WorkflowAccessWhiteListId>{
  
  boolean existsByOauth2ClientIdAndWorkflowDefinitionId(String oauth2ClientId, UUID workflowDefinitionId);
  
  @Modifying
  @Query("delete from WorkflowAccessWhitelist l where l.workflowDefinitionId in(:ids)")
  void clearWorkflowAccessList(@Param("ids") Set<UUID> ids);

  List<WorkflowAccessWhitelist> getByWorkflowDefinitionId(UUID workflowDefinitionId);
  
}
