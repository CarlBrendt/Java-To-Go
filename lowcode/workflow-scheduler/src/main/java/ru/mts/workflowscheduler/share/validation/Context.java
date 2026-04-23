package ru.mts.workflowscheduler.share.validation;

import lombok.RequiredArgsConstructor;
import ru.mts.workflowscheduler.service.ErrorCompiler;
import ru.mts.workflowscheduler.service.ScriptExecutorService;

@RequiredArgsConstructor
public class Context {

  private final ErrorCompiler compiler;
  private final ScriptExecutorService scriptExecutorService;

  boolean isExecutable(String text) {
    return scriptExecutorService.isExecutable(text);
  }

  boolean isSecretRef(String text) {
    return text != null && text.startsWith("secret{");
  }

}
