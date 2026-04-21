package ru.mts.ip.workflow.engine.lang.plant;

import java.util.HashSet;
import java.util.Set;
import ru.mts.ip.workflow.engine.exception.DuplicateIdException;

public class IdGen {

  private int counter;
  private final Set<String> released = new HashSet<>();

  public String nextId(String prefix, String exists) {
    if (exists == null) {
      return String.format("%s_%s", prefix, counter++);
    } else {
      if (released.contains(exists)) {
        throw new DuplicateIdException("duplicate id[%s]".formatted(exists));
      } else {
        released.add(exists);
        return exists;
      }
    }
  }
}
