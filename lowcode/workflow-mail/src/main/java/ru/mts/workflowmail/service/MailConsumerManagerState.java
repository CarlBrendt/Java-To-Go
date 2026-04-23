package ru.mts.workflowmail.service;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowmail.service.dto.WorkerState;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Accessors(chain = true)
public class MailConsumerManagerState {
  private Collection<WorkerState> workers = new ArrayList<>();
}
