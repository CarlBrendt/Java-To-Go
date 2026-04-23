package ru.mts.ip.workflow.engine.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.utility.DateHelper;

import java.util.Set;
import java.util.UUID;

public class StarterWorkerSpecifications {
  
  public static Specification<WorkerEntity> findById(UUID id){
    return (root, query, builder) -> builder.equal(root.get("id"), id);
  }

  public static Specification<WorkerEntity> findExecutable(String type, Set<String> statuses){
    return (root, query, b) -> {
      var now = DateHelper.now();
      var starter = root.get("starter");
      var stateIn = root.get("status").in(statuses);
      var typeEq = b.equal(root.get("starter").get("type") , type);
      var overdue = b.lessThan(root.get("overdueTime"), now);
      var retryable = b.greaterThan(root.get("retryCount"), 0);
      var afterStart = b.or(starter.get("startDateTime").isNull(),b.lessThanOrEqualTo(starter.get("startDateTime"),now));
      var beforeEnd = b.or(starter.get("endDateTime").isNull(), b.greaterThanOrEqualTo(starter.get("endDateTime"), now));
      var workTime = b.and(afterStart, beforeEnd);
      var notLocked = b.or(b.isNull(root.get("lockedUntilTime")), b.lessThan(root.get("lockedUntilTime"), now));
      return b.and(stateIn, retryable, overdue, notLocked, typeEq, workTime);
    };
  }
  
}
