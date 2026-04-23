package ru.mts.workflowscheduler.service;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.service.dto.WorkerState;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Accessors(chain = true)
public class SchedulerConsumerManagerState {
  private Collection<WorkerState> workers = new ArrayList<>();
}
