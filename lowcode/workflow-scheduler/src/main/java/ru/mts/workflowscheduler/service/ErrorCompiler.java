package ru.mts.workflowscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mts.workflowscheduler.exception.ErrorContext;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.exception.ErrorLocation;
import ru.mts.workflowscheduler.exception.ErrorMessageArgs;
import ru.mts.workflowscheduler.share.script.ClientError;
import ru.mts.workflowscheduler.share.script.ClientErrorDescription;
import ru.mts.workflowscheduler.share.validation.Errors2;
import ru.mts.workflowscheduler.share.validation.schema.ConstraintViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ErrorCompiler {

  public final Internationalizer internationalizer;
  public final WikiErrorPageProvider wikiErrorPageProvider;

  public ErrorLocation location(String fieldPath) {
    return new ErrorLocation().setFieldPath(fieldPath);
  }

  public ErrorContext ctx(Object rejectedValie) {
    return new ErrorContext(rejectedValie);
  }

  public ErrorLocation location() {
    return new ErrorLocation();
  }

  public ErrorMessageArgs args(Object[] messageArgs, Object[] adviceMessageArgs) {
    return new ErrorMessageArgs().setMessageArgs(messageArgs).setAdviceMessageArgs(adviceMessageArgs);
  }

  public ErrorMessageArgs args() {
    return new ErrorMessageArgs();
  }

  public ErrorMessageArgs args(Object ...args) {
    return new ErrorMessageArgs().and(args);
  }

  public ErrorMessageArgs msgArgs(Object ...messageArgs) {
    return new ErrorMessageArgs().setMessageArgs(messageArgs);
  }

  public ErrorMessageArgs adviceArgs(Object ...adviceMessageArgs) {
    return new ErrorMessageArgs().setAdviceMessageArgs(adviceMessageArgs);
  }

  public ErrorDescription error(Errors2 error, ErrorLocation location, ErrorContext context) {
    return error(error, location, context, null, null);
  }

  public ErrorDescription error(Errors2 error, ErrorLocation location) {
    return error(error, location, null, null, null);
  }

  public ErrorDescription error(Errors2 error, ErrorMessageArgs messageArgs) {
    return error(error, null, null, messageArgs, null);
  }

  public ErrorDescription error(Errors2 error, ErrorContext ctx) {
    return error(error, null, ctx, null, null);
  }

  public ErrorDescription error(Errors2 error, ErrorContext ctx, ErrorMessageArgs args) {
    return error(error, null, ctx, args, null);
  }

  public ErrorDescription errorWithMessageArgs(Errors2 error, ErrorLocation location, ErrorMessageArgs args) {
    return error(error, location, null, args, null);
  }

  public ErrorDescription error(Errors2 error, Exception ex) {
    return error(error, null, null, null, ex);
  }

  public ErrorDescription error(Errors2 error) {
    return error(error, null, null, null, null);
  }

  public ErrorDescription error(Errors2 error, ErrorLocation location, ErrorContext context, ErrorMessageArgs messageArgs) {
    return error(error, location, context, messageArgs, null);
  }

  public ErrorDescription error(Errors2 error, ErrorLocation location, ErrorContext context, ErrorMessageArgs messageArgs, Exception exception) {
    Object[] errorMessageArgs = Optional.ofNullable(messageArgs).map(ErrorMessageArgs::getMessageArgs).orElse(null);
    Object[] adviceMessageArgs = Optional.ofNullable(messageArgs).map(ErrorMessageArgs::getAdviceMessageArgs).orElse(null);

   var res = new ErrorDescription()
        .setCode(error.getCode())
        .setLevel(error.getLevel())
        .setLocation(location)
        .setMessage(internationalizer.resolveMessage(error.getErrorMessageAlias(), errorMessageArgs))
        .setSolvingAdviceMessage(internationalizer.resolveMessage(error.getSolvingMessageAlias(), adviceMessageArgs))
        .setSolvingAdviceUrl(wikiErrorPageProvider.findPageUrl(error.getCode()))
        .setContext(context);

   Optional.ofNullable(exception).ifPresent(ex -> res.setSystemMessage(ex.getMessage()));
   return res;

  }

  public List<ErrorDescription> toErrorDescription(ConstraintViolation violation) {
    var completeErrors = violation.getCompleteErrors();
    if(completeErrors != null && !completeErrors.isEmpty()) {
      return completeErrors;
    } else {
      var cause = violation.getCause();
      Errors2 err = violation.getError();
      ErrorMessageArgs messageArgs = violation.getMessageArgs();

      ErrorDescription desc = messageArgs != null ? error(err, messageArgs) : error(err);

      Optional.ofNullable(violation.getPath()).ifPresent(path -> {
        desc.setLocation(location(path));
      });
      Optional.ofNullable(violation.getRejectedValue()).ifPresent(val -> {
        desc.setContext(ctx(val));
      });
      if(cause != null) {
        desc.setCause(toErrorDescription(cause));
      }
      return List.of(desc);
    }
  }

  public List<ErrorDescription> toErrorDescription(ClientError clientError) {
    var compiled = clientError.getCompiledErrors();
    if(compiled != null && !compiled.isEmpty()) {
      return compiled;
    }
    List<ErrorDescription> res = new ArrayList<>();
    List<ClientErrorDescription> errors = clientError.getErrors();
    if(errors != null) {
      res.addAll(toErrorDescription(errors));
    }
    return res;
  }

  public List<ErrorDescription> toErrorDescription(List<ClientErrorDescription> errors) {
    List<ErrorDescription> res = new ArrayList<>();
    if(errors != null) {
      errors.forEach(ce -> {
        Errors2 code = ce.getError();
        Object[] messageArgs = ce.getMessageArgs();
        Object[] adviceMessageArgs = ce.getAdviceMessageArgs();
        ErrorDescription desc = error(code, msgArgs(messageArgs).setAdviceMessageArgs(adviceMessageArgs));
        desc.setLocation(ce.getLocation());
        desc.setContext(ce.getContext());
        desc.setInputValidationContext(ce.getInputValidationContext());
        desc.setScriptContext(ce.getScriptContext());
        desc.setSystemMessage(ce.getSystemMessage());
        res.add(desc);
      });
    }
    return res;
  }

}
