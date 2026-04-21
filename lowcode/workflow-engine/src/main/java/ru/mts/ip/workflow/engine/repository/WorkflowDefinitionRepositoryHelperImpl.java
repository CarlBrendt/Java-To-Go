package ru.mts.ip.workflow.engine.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkflowDefinitionRepositoryHelperImpl implements WorkflowDefinitionRepositoryHelper{

  private final WorkflowDefinitionRepository repo;
  
  @Override
  @Transactional
  public Optional<WorkflowDefinition> findDeployedDefinition(Ref workflowRef) {
    Optional<WorkflowDefinition> res = Optional.empty();
    var id = workflowRef.getId();
    if(id != null) {
      res = repo.findByIdAndStatusAndDeleted(id, Const.DefinitionStatus.DEPLOYED, false);
    } else {
      var name = workflowRef.getName();
      if(name != null) {
        var tenantId = Optional.ofNullable(workflowRef.getTenantId()).orElse("default");
        var version = workflowRef.getVersion();
        if(version != null) {
          res = repo.findFirstByNameAndTenantIdAndVersionAndStatusOrderByVersionDesc(name, tenantId, version, Const.DefinitionStatus.DEPLOYED);
        } else {
          res = repo.findFirstByNameAndTenantIdAndStatusAndAvailabilityStatusOrderByVersionDesc(name, tenantId, Const.DefinitionStatus.DEPLOYED, Const.DefinitionAvailabilityStatus.ACTIVE);
        }
      }
    }
    return res;
  }
  
}
