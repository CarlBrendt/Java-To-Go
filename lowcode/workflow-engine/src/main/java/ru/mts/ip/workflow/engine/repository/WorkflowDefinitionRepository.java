package ru.mts.ip.workflow.engine.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.WorkflowVersion;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID>, JpaSpecificationExecutor<WorkflowDefinition>, CustomizedWorkflowDefinitionRepository {

  Optional<WorkflowDefinition> findFirstByNameOrderByVersionDesc(String name);

  Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndStatusOrderByVersionDesc(String name, String tenantId, String status);

  boolean existsByNameAndTenantIdAndStatusAndDeleted(String name, String tenantId, String status, boolean deleted);

  Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndVersionAndStatusOrderByVersionDesc(String name, String tenantId, Integer version, String status);

  Optional<WorkflowDefinition> findByIdAndStatusAndDeleted(UUID id, String status, boolean deleted);

  @Modifying
  @Query("update WorkflowDefinition d set d.deleted = true where d.name = :name and d.tenantId = :tenantId and d.status = :status")
  void markRemoved(String name, String tenantId, String status);

  @Modifying
  @Query("update WorkflowDefinition d set d.deleted = true where d.name = :name and d.tenantId = :tenantId and d.status = :status and d.version = :version")
  void markRemoved(String name, String tenantId, String status, Integer version);

  @Modifying
  @Query("update WorkflowDefinition d set d.latest = false where d.id = :id")
  void markNotLatest(UUID id);

  @Modifying
  @Query("update WorkflowDefinition d set d.latest = false where d.name = :name and d.tenantId = :tenantId and d.status = :status and d.latest = true")
  void markAllNotLatest(String name, String tenantId, String status);

  Optional<WorkflowVersion> findFirstVersionByNameAndTenantIdAndStatusOrderByVersionDesc(String name, String tenantId, String status);

  Optional<WorkflowDefinition> findFirstByNameAndTenantIdAndStatusAndAvailabilityStatusOrderByVersionDesc(String name, String tenantId, String status,String availabilityStatus);
}
