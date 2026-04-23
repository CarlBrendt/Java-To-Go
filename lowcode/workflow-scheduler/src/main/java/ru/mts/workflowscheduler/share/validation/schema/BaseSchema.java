package ru.mts.workflowscheduler.share.validation.schema;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.service.Const;
import ru.mts.workflowscheduler.share.validation.Constraint;
import ru.mts.workflowscheduler.share.validation.Context;
import ru.mts.workflowscheduler.share.validation.Errors2;
import ru.mts.workflowscheduler.share.validation.ValidateDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseSchema {

  protected List<ConstraintViolation> violations = new ArrayList<>();

  @Getter
  private String path;

  public BaseSchema(List<Constraint> constraints) {
    this.constraints.addAll(constraints);
  }

  public BaseSchema() {
    this(List.of());
  }

  protected void addConstraint(Constraint constraint) {
    constraints.add(constraint);
  }

  public BaseSchema copy() {
    return new BaseSchema(constraints);
  }

  protected Map<String, BaseSchema> getNested(){
    return Map.of();
  }

  protected void enrichContext(Context ctx, JsonNode value){

  }

  protected List<Constraint> copyConstraints(){
    return new ArrayList<>(constraints);
  }

  protected final List<Constraint> constraints = new ArrayList<>();

  public void initPath(String val) {
    path = val == null || val.isBlank() ? "$" : val;
    getNested().forEach((k,v) -> v.initPath("%s.%s".formatted(path, k)));
  }

  public boolean containErrors() {
    return !violations.stream().filter(ConstraintViolation::containsCriticalErrors).toList().isEmpty();
  }

  public void validate(Context ctx, JsonNode json) {
    enrichContext(ctx, json);
    ValidateDecision des = new ValidateDecision();
    for(Constraint c : constraints) {
      des = c.getValidation().validate(ctx, json);
      Errors2 error = des.getError();
      List<ErrorDescription> completeErrors = des.getErrorDescriptions();

      if(error != null) {
        violations.add(new ConstraintViolation().setError(des.getError()).setPath(path).setRejectedValue(json).setMessageArgs(des.getArgs()));
        break;
      } else if (completeErrors != null && !completeErrors.isEmpty()) {
        completeErrors.stream().forEach(ed -> ed.setRootFieldPath(path));
        violations.add(new ConstraintViolation().setCompleteErrors(completeErrors));
        if(!completeErrors.stream().filter(err -> Const.ErrorLevel.CRITICAL.equals(err.getLevel())).toList().isEmpty()) {
          break;
        }
      }
    }
  }

  public List<ConstraintViolation> getViolations(){
    List<ConstraintViolation> res = new ArrayList<>(violations);
    getNested().values().forEach(f -> res.addAll(f.getViolations()));
    return res;
  }

}
