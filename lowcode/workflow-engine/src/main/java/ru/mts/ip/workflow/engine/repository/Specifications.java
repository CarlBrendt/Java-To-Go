package ru.mts.ip.workflow.engine.repository;

import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.DESCRIPTION;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.ID;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.NAME;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.OWNER_LOGIN;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.STATUS;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.TENANT_ID;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.VERSION;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.DELETED;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.LATEST;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.DefinitionSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;

public class Specifications {

  
  public static Specification<WorkflowDefinition> findById(UUID id){
    return (root, query, builder) -> builder.equal(root.get(ID), id);
  }

  public static Specification<WorkflowDefinition> findByRef(Ref ref){
    var id = ref.getId();
    if(id != null) {
      return findById(id);
    } else {
      var name = ref.getName();
      var tenantId = Optional.ofNullable(ref.getTenantId()).orElse(Const.DEFAULT_TENANT_ID);
      var version = Optional.ofNullable(ref.getVersion()).orElse(0);
      return (root, query, b) -> {
        Predicate eqName = b.equal(root.get(NAME), name);
        Predicate eqTenantId = b.equal(root.get(TENANT_ID), tenantId);
        Predicate eqVersion = b.equal(root.get(VERSION), version);
        return version > 0 ? b.and(eqName, eqTenantId, eqVersion) : b.and(eqName, eqTenantId);
      };
      
    }
  }

  public static Specification<WorkflowDefinition> searchDefinition(DefinitionSearching serching){
    
    Specification<WorkflowDefinition> res = (root, query, b) -> {
      List<Predicate> predicates = new ArrayList<>();
      
      predicates.add(b.equal(root.get(DELETED), false));
      
      List<String> products = serching.getProducts();
      if(products != null && !products.isEmpty()) {
        predicates.add(root.get(TENANT_ID).in(products));
      }
      
      String description = serching.getDescription();

      String version = serching.getVersion();
      if(version != null) {
        if("latest".equals(version)) {
          predicates.add(b.isTrue(root.get(LATEST)));
        } else {
          predicates.add(b.equal(root.get(VERSION), Integer.valueOf(version)));
        }
      }
      
      if(description != null && !description.isBlank()) {
        predicates.add(b.like(b.lower(root.get(DESCRIPTION)), "%" + description.toLowerCase() + "%"));
      }

      UUID id = serching.getId();
      if(id != null) {
        predicates.add(b.equal(root.get(ID), id));
      }
      
      String ownerLogin = serching.getOwnerLogin();
      if(ownerLogin != null && !ownerLogin.isBlank()) {
        predicates.add(b.equal(root.get(OWNER_LOGIN), ownerLogin));
      }
      
      List<String> statuses = serching.getStatuses();
      if(statuses != null && !statuses.isEmpty()) {
        predicates.add(root.get(STATUS).in(statuses));
      } else {
        predicates.add(root.get(STATUS).in(Const.DefinitionStatus.POSIBLE_VALUES));
      }
      
      var name = serching.getName();
      if(name != null && !name.isBlank()) {
        predicates.add(b.equal(root.get(NAME), name));
      }

      var tenantId = serching.getTenantId();
      if(tenantId != null && !tenantId.isBlank()) {
        predicates.add(b.equal(root.get(TENANT_ID), tenantId));
      }
      
      return b.and(predicates.toArray(new Predicate[] {}));
    };
    
    return res;
  }

  public static Specification<StarterEntity> searchStarter(StarterSearching serching){

    Specification<StarterEntity> res = (root, query, b) -> {
      List<Predicate> predicates = new ArrayList<>();

      // predicates.add(b.equal(root.get("enabled"), true));
      List<String> desiredStatuses = serching.getDesiredStatuses();
      if(desiredStatuses != null && !desiredStatuses.isEmpty()) {
        predicates.add(root.get("desiredStatus").in(desiredStatuses));
      }

      List<String> actualStatuses = serching.getActualStatuses();
      if(actualStatuses != null && !actualStatuses.isEmpty()) {
        predicates.add(root.get("actualStatus").in(actualStatuses));
      }

      List<UUID> definitionIds = serching.getWorkflowDefinitionToStartIds();
      if(definitionIds != null) {
        predicates.add(root.get("workflowDefinition").get("id").in(definitionIds));
      }

      var name = serching.getName();
      if(name != null && !name.isBlank()) {
        predicates.add(b.equal(root.get("name"), name));
      }

      var type = serching.getType();
      if(type != null && !type.isBlank()) {
        predicates.add(b.equal(root.get("type"), type));
      }

      var tenantId = serching.getTenantId();
      if(tenantId != null && !tenantId.isBlank()) {
        predicates.add(b.equal(root.get("tenantId"), tenantId));
      }

      return b.and(predicates.toArray(new Predicate[] {}));
    };

    return res;
  }

}
