package ru.mts.ip.workflow.engine.lang.plant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

@Data
public class Pouch {
  private Map<String, Activity> states = new HashMap<>();
  private Set<String> repeats = new HashSet<>();
  private boolean repeatCration;
  private final IdGen idGen = new IdGen();

  public Activity get(String id) {
    return states.get(id);
  }

  public void repeat(String id) {
    repeats.add(id);
  }

  public boolean exists(String id) {
    return repeats.contains(id);
  }

  public String nextId(String prefix, String exists) {
    return idGen.nextId(prefix, exists);
  }
}
