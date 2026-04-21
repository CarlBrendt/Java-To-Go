package ru.mts.ip.workflow.engine.executor;

import java.util.Set;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResolveExternalPropertiesConfig {
  private Set<String> toResolve;
}
