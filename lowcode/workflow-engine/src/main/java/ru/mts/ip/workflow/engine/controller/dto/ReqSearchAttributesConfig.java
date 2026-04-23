package ru.mts.ip.workflow.engine.controller.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import lombok.Data;
import lombok.Getter;

@Data
public class ReqSearchAttributesConfig {
  
  private String description;
  private String creatorLogin;
  private String name;
  
  private Long limit = 100L;
  private Long offset = 0L;
  private ReqSorting sorting;
  
  @Getter
  public static class ReqSorting{
    private Map<String, Direction> deirections = new HashMap<>();
    public ReqSorting(String sorting) {
      if(sorting != null) {
        String[] parts = sorting.split(",");
        for(String part: parts) {
          String[] subParts = part.split("-");
          String name = subParts.length > 0 ? subParts[0] : null;
          String direction = subParts.length > 1 ? subParts[1] : null;
          if(name != null) {
            deirections.put(name, toDirection(direction));
          }
        }
      }
    }

    private Direction toDirection(String text) {
      return Optional.ofNullable(text)
          .map(Direction::fromString)
          .orElse(Direction.DESC);
    }
    
  }
  
}
