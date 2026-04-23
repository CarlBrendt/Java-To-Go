package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import ru.mts.ip.workflow.engine.repository.OffsetBasedPageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
public class StarterSearching {

  private Integer limit = 100;
  private Integer offset = 0;
  private String name;
  private String type;
  private String tenantId;
  private List<Sorting> sorting;
  private List<UUID> workflowDefinitionToStartIds;
  private List<String> desiredStatuses;
  private List<String> actualStatuses;
  
  @Data
  public static class Sorting {
    private String name;
    private String direction;
  }
  
  public Pageable asPageableOrDefault(Sort defaultSort) {
    List<Order> orders = sorting == null ? List.of() :sorting.stream()
      .map(e -> new Order(toDirection(e.getDirection()), e.getName())).toList();
    Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
    return new OffsetBasedPageRequest(offset, limit, sort);
  }

  public List<Order> asOrders(Sort defaultSort) {
    List<Order> orders = sorting == null ? List.of() :sorting.stream()
        .map(e -> new Order(toDirection(e.getDirection()), e.getName())).toList();
    return orders;
  }
  
  private Direction toDirection(String text) {
    return Optional.ofNullable(text).map(Direction::fromString).orElse(Direction.DESC);
  }

  
}
