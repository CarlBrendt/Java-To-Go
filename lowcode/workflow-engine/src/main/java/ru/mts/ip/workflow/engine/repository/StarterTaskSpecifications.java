package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public class StarterTaskSpecifications {
  
  public static Specification<StarterTaskEntity> findById(UUID id){
    return (root, query, builder) -> builder.equal(root.get("id"), id);
  }

  public static Specification<StarterTaskEntity> findExecutable(Set<StarterTaskEntity.Type> types, Set<StarterTaskEntity.State> states){
    return (root, query, b) -> {
      var now = OffsetDateTime.now();
      var typeIn = root.get("type").in(types);
      var stateIn = root.get("state").in(states);
      var notOld = b.greaterThan(root.get("createTime"), now.minusDays(7));
      var overdued = b.lessThan(root.get("overdueTime"), now);
      var retriable = b.greaterThan(root.get("retryCount"), 0);
      var notLocked = b.or(b.isNull(root.get("lockedUntilTime")), b.lessThan(root.get("lockedUntilTime"), now));
      return b.and(typeIn, stateIn, notOld, retriable, overdued, notLocked);
    };
  }
  
}
