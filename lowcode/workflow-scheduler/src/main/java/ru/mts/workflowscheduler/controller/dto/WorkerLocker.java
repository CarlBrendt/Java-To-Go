package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.service.Const;

import java.util.Set;
import java.util.UUID;



@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WorkerLocker {
  @NotNull
  private UUID executorId;
  @NotNull
  @NotEmpty
  @Setter
  private String type;
  @Setter
  private Set<String> statuses;

  public static WorkerLocker defaultInstance() {
    return new WorkerLocker(Const.getApplicationInstanceId(), Const.StarterType.SCHEDULER, null);
  }

  private WorkerLocker(UUID executorId, String type, Set<String> statuses) {
    this.executorId = executorId;
    this.type = type;
    this.statuses = statuses;
  }

}
