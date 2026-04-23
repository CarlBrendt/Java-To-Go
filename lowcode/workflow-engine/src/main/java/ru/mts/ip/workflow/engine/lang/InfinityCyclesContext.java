package ru.mts.ip.workflow.engine.lang;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class InfinityCyclesContext {
  private List<List<String>> infinityCycles;
}
