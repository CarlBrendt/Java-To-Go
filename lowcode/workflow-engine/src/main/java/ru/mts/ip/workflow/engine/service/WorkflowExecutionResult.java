package ru.mts.ip.workflow.engine.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowExecutionResult {

  private final CompletableFuture<Variables> resultFuture;
  
  @Getter
  private final String runId;
  @Getter
  private final String businessKey;
  
  @JsonIgnore
  public Variables getResult() throws InterruptedException, ExecutionException {
    return resultFuture.get();
  }

  public Variables getResult(long ttl, TimeUnit unit) throws TimeoutException, InterruptedException, ExecutionException {
    return resultFuture.get(ttl, unit);
  }

}