package ru.mts.ip.workflow.engine.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching.Sorting;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomizedStarterRepositoryImpl implements CustomizedStarterRepository {

  
  private final EntityManager em;

  @Override
  public List<StarterShortListValue> search(StarterSearching searchConfig) {
    var b = em.getCriteriaBuilder();
    var query = b.createTupleQuery();
    var root = query.from(StarterEntity.class);
    var wfDefinition = root.join("workflowDefinition");
    
    query.multiselect(
      root.get("id").alias("id"),
      root.get("type").alias("type"),
      root.get("name").alias("name"),
      root.get("tenantId").alias("tenantId"),
      root.get("description").alias("description"),
      root.get("desiredStatus").alias("desiredStatus"),
      root.get("actualStatus").alias("actualStatus"),
      root.get("createTime").alias("createTime"),
      root.get("startDateTime").alias("startDateTime"),
      root.get("endDateTime").alias("endDateTime"),
      wfDefinition.get("id").alias("workflowDefinitionToStartId")
    );
    
    query.where(Specifications.searchStarter(searchConfig).toPredicate(root, query, b));
    
    var sorting = searchConfig.getSorting();
    if(sorting != null && !sorting.isEmpty()) {
      query.orderBy(toOrders(sorting, b, root));
    } else {
      query.orderBy(b.desc(root.get("createTime")));
    }
    
    
    List<Tuple> gg = em.createQuery(query).setFirstResult(searchConfig.getOffset()).setMaxResults(searchConfig.getLimit()).getResultList();
    return gg.stream().map(this::toValue).toList();
  }
  
  private StarterShortListValue toValue(Tuple t) {
    return new StarterShortListValue()
        .setId(t.get("id", UUID.class))
        .setType(t.get("type", String.class))
        .setName(t.get("name", String.class))
        .setTenantId(t.get("tenantId", String.class))
        .setDescription(t.get("description", String.class))
        .setDesiredStatus(t.get("desiredStatus", String.class))
        .setActualStatus(t.get("actualStatus", String.class))
        .setCreateTime(t.get("createTime", OffsetDateTime.class))
        .setStartDateTime(t.get("startDateTime", OffsetDateTime.class))
        .setEndDateTime(t.get("endDateTime", OffsetDateTime.class))
        .setWorkflowDefinitionToStartId(t.get("workflowDefinitionToStartId", UUID.class));
  }
  
  private List<Order> toOrders(List<Sorting> sortings, CriteriaBuilder b, Root<StarterEntity> root) {
    return sortings.stream().map(sortEntry -> toOrders(sortEntry, b, root)).toList();
  }
  
  private Order toOrders(Sorting sorting, CriteriaBuilder b, Root<StarterEntity> root) {
    @NonNull var name = sorting.getName();
    @NonNull var direction = sorting.getDirection();
    switch (direction) {
      case Const.SortingDirection.ASC:
        return b.asc(root.get(name));
      case Const.SortingDirection.DESC:
        return b.desc(root.get(name));
      default :
        throw new IllegalArgumentException();
    }
  }
  
  @Override
  public Long searchCount(StarterSearching searchConfig) {
    var b = em.getCriteriaBuilder();
    var query = b.createQuery(Long.class);
    var root = query.from(StarterEntity.class);
    query.select(b.count(root));
    query.where(Specifications.searchStarter(searchConfig).toPredicate(root, query, b));
    return em.createQuery(query).getSingleResult();
  }
  
}
