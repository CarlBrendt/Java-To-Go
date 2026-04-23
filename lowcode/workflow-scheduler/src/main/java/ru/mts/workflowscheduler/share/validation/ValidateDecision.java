package ru.mts.workflowscheduler.share.validation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.exception.ErrorMessageArgs;
import ru.mts.workflowscheduler.share.validation.schema.ConstraintViolation;

import java.util.List;

@Data
@Accessors(chain = true)
public class ValidateDecision {

  private Errors2 error;
  private ErrorMessageArgs args;
  private List<ErrorDescription> errorDescriptions;
  private ValidateDecision cause;


  public boolean isError() {
    return error != null || errorDescriptions != null;
  }

  public ValidateDecision(Errors2 error){
    this.error = error;
  }

  public ValidateDecision(Errors2 error, ValidateDecision cause){
    this.cause = cause;
    this.error = error;
  }

  public ValidateDecision(){

  }

  public ValidateDecision(List<ErrorDescription> descriptions){
    errorDescriptions = descriptions;
  }

  public ConstraintViolation asConstraintViolation(JsonNode rejectedvalue) {
    return new ConstraintViolation().setError(error).setMessageArgs(args).setRejectedValue(rejectedvalue);
  }

}
