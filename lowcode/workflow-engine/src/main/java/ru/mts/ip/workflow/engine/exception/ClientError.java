package ru.mts.ip.workflow.engine.exception;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.dto.ScriptErrorContext;
import ru.mts.ip.workflow.engine.service.WorkflowIstanceIdentity;

@SuppressWarnings("serial")
public class ClientError extends RuntimeException{

  @Getter
  private HttpStatus status;
  @Getter
  private final List<ClientErrorDescription> errors;

  @Getter @Setter
  private List<ErrorDescription> compiledErrors;

  public ClientError(HttpStatus status, Errors2 error, ErrorMessagePouch messages) {
    this(List.of(new ClientErrorDescription().setError(error).setMessageAargs(messages.getMessageAargs())
        .setSystemMessage(messages.getSystemMessage()).setAdviceMessageArgs(messages.getAdviceMessageArgs())));
    
    this.status = status;
  }

  public ClientError(Errors2 error, ErrorMessagePouch messages) {
    this(HttpStatus.BAD_REQUEST, error, messages);
  }

  public ClientError() {
    this.errors = null;
    this.status = HttpStatus.BAD_REQUEST;
    this.compiledErrors = List.of();
  }

  public ClientError(@NonNull List<ClientErrorDescription> errors) {
    this(errors, null);
    this.status = HttpStatus.BAD_REQUEST;
    this.compiledErrors = List.of();
  }
  

  public ClientError(@NonNull ClientErrorDescription error) {
    this(List.of(error));
  }

  public ClientError(@NonNull List<ClientErrorDescription> errors, Throwable th) {
    super(th);
    this.errors = errors;
    this.status = HttpStatus.BAD_REQUEST;
  }

  public void setLocation(ErrorLocation newLocation) {
    if(errors != null) {
      errors.forEach(e -> {
        e.setParentLocation(newLocation);
      });
    }
    
    if(compiledErrors != null) {
      compiledErrors.forEach(e -> {
        e.setParentLocation(newLocation);
      });
    }
  }
  
  public static ClientError instanceNotFound(WorkflowIstanceIdentity identity) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKFLOW_INSTANCE_IS_NOT_FOUND, new ErrorMessagePouch());
  }

  public static ClientError syncRunTimeout() {
    return new ClientError(HttpStatus.REQUEST_TIMEOUT, Errors2.SYNC_EXECUTION_TIMEOUT, new ErrorMessagePouch());
  }
  
  public ClientError(String message) {
    super(message);
    this.errors = null;
    this.status = HttpStatus.BAD_REQUEST;
  }
  
  private static String findSystemMessage(ErrorDescription desc) {
    String res = null;
    if(desc != null) {
      String sysMessage = desc.getSystemMessage();
      if(sysMessage == null) {
        ScriptErrorContext scriptContext = desc.getScriptContext();
        if(scriptContext != null) {
          res = scriptContext.getSystemMessage();
        }
      }
    }
    return res;
  }
  
  public static ClientError compiled(List<ErrorDescription> errors) {
    List<String> systemMessages = 
        Optional.ofNullable(errors).orElse(List.of())
        .stream()
        .map(ClientError::findSystemMessage)
        .toList();
    ClientError result = new ClientError("Script execution errors: [%s]".formatted(String.join(", ", systemMessages)));
    result.compiledErrors = errors == null ? List.of() : errors;
    return result;
  }
  
}
