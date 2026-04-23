package ru.mts.workflowmail.share.validation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.service.ErrorCompiler;
import ru.mts.workflowmail.service.ScriptExecutorService;
import ru.mts.workflowmail.service.StarterScriptValidationService;
import ru.mts.workflowmail.service.dto.MailConsumerForInternal;
import ru.mts.workflowmail.share.script.ScriptExecutionContext;

import java.util.List;

@RequiredArgsConstructor
public class Context {

  private final StarterScriptValidationService scriptValidationService;
  private final ErrorCompiler compiler;
  private final ScriptExecutorService scriptExecutorService;

  @Getter
  private ScriptExecutionContext mailConsumerScriptContext;

  boolean isExecutable(String text) {
    return scriptExecutorService.isExecutable(text);
  }

  boolean isSecretRef(String text) {
    return text != null && text.startsWith("secret{");
  }

  public List<ErrorDescription> validateOutputTemplateValueReplacement(JsonNode value) {
    return scriptValidationService.validateOutputTemplateValueReplacement(value, mailConsumerScriptContext)
        .map(compiler::toErrorDescription).orElse(List.of());
  }

  public void initScriptExecutionContext(MailConsumerForInternal consumer) {
    mailConsumerScriptContext = scriptValidationService.compileScriptContext(consumer);
  }
}
