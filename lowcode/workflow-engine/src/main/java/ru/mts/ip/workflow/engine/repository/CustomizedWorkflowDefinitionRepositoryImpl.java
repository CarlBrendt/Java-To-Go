package ru.mts.ip.workflow.engine.repository;

import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.CHANGE_TIME;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.CREATE_TIME;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.DESCRIPTION;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.ID;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.NAME;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.OWNER_LOGIN;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.STATUS;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.TENANT_ID;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.TYPE;
import static ru.mts.ip.workflow.engine.entity.WorkflowDefinition_.VERSION;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.service.DefinitionListValue;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.DefinitionSearching;

@Repository
@RequiredArgsConstructor
public class CustomizedWorkflowDefinitionRepositoryImpl implements CustomizedWorkflowDefinitionRepository{

  
  private final EntityManager em;

  @Override
  public List<DefinitionListValue> search(DefinitionSearching searchConfig) {
    List<DefinitionListValue> res = new ArrayList<>();
    var b = em.getCriteriaBuilder();
    var query = b.createTupleQuery();
    var root = query.from(WorkflowDefinition.class);
    
    query.multiselect(
      root.get(ID).alias(ID),
      root.get(TYPE).alias(TYPE),
      root.get(CHANGE_TIME).alias(CHANGE_TIME),
      root.get(CREATE_TIME).alias(CREATE_TIME),
      root.get(NAME).alias(NAME),
      root.get(TENANT_ID).alias(TENANT_ID),
      root.get(DESCRIPTION).alias(DESCRIPTION),
      root.get(VERSION).alias(VERSION),
      root.get(STATUS).alias(STATUS),
      root.get(OWNER_LOGIN).alias(OWNER_LOGIN)
     );
    
    query.where(Specifications.searchDefinition(searchConfig).toPredicate(root, query, b));
    query.orderBy( b.desc(root.get(CHANGE_TIME)));
    
    List<Tuple> gg = em.createQuery(query).setFirstResult(searchConfig.getOffset()).setMaxResults(searchConfig.getLimit()).getResultList();

    gg.stream().forEach(t -> {
      res.add(new DefinitionListValue()
        .setId(t.get(ID, UUID.class))
        .setType(t.get(TYPE, String.class))
        .setName(t.get(NAME, String.class))
        .setDescription(t.get(DESCRIPTION, String.class))
        .setTenantId(t.get(TENANT_ID, String.class))
        .setCreateTime(DateHelper.asTextISO(t.get(CREATE_TIME, OffsetDateTime.class), null))
        .setChangeTime(DateHelper.asTextISO(t.get(CHANGE_TIME, OffsetDateTime.class), null))
        .setVersion(t.get(VERSION, Integer.class))
        .setStatus(t.get(STATUS, String.class))
        .setOwnerLogin(t.get(OWNER_LOGIN, String.class))
      );
    });
    
    return res;
  }

  @Override
  public Long searchCount(DefinitionSearching searchConfig) {
    var b = em.getCriteriaBuilder();
    var query = b.createQuery(Long.class);
    var root = query.from(WorkflowDefinition.class);
    query.select(b.count(root));
    query.where(Specifications.searchDefinition(searchConfig).toPredicate(root, query, b));
    return em.createQuery(query).getSingleResult();
  }
  
  
}
